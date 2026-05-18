package cz.smarteon.loxkt.ktor

import cz.smarteon.loxkt.Codec
import cz.smarteon.loxkt.Codec.loxJson
import cz.smarteon.loxkt.Command
import cz.smarteon.loxkt.LoxoneCommands
import cz.smarteon.loxkt.LoxoneEndpoint
import cz.smarteon.loxkt.LoxoneResponse
import cz.smarteon.loxkt.LoxoneTokenAuthenticator
import cz.smarteon.loxkt.WebsocketLoxoneClient
import cz.smarteon.loxkt.event.LoxoneEvent
import cz.smarteon.loxkt.message.MessageHeader
import cz.smarteon.loxkt.message.MessageKind
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import io.ktor.websocket.CloseReason.Codes.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.jvm.JvmOverloads
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class KtorWebsocketLoxoneClient internal constructor(
    private val client: HttpClient,
    private val endpoint: LoxoneEndpoint? = null,
    private val authenticator: LoxoneTokenAuthenticator? = null,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    eventBufferSize: Int = DEFAULT_EVENT_BUFFER_SIZE
) : WebsocketLoxoneClient {

    @JvmOverloads constructor(
        endpoint: LoxoneEndpoint,
        authenticator: LoxoneTokenAuthenticator? = null,
        eventBufferSize: Int = DEFAULT_EVENT_BUFFER_SIZE
    ) : this(HttpClient { install(WebSockets) }, endpoint, authenticator, Dispatchers.Default, eventBufferSize)

    private val logger = KotlinLogging.logger {}

    private val sessionMutex = Mutex()
    private var session: ClientWebSocketSession? = null

    /**
     * Scope used for all background tasks in the client. Should not be used for executing commands (call* functions).
     */
    private val scope = CoroutineScope(dispatcher)

    private val keepAliveHeader = Channel<MessageHeader>(capacity = 1)
    private val textMsgHeader = Channel<MessageHeader>(capacity = 1)
    private val binaryMsgHeader = Channel<MessageHeader>(capacity = 1)
    private val textMessages = Channel<String>(capacity = 10)

    private val _events = MutableSharedFlow<LoxoneEvent>(extraBufferCapacity = eventBufferSize)
    override val events: SharedFlow<LoxoneEvent> = _events.asSharedFlow()

    override suspend fun <RESPONSE : LoxoneResponse> call(command: Command<RESPONSE>): RESPONSE {
        val session = ensureSession()
        if (command.authenticated) {
            authenticator?.ensureAuthenticated(this)
        }

        session.send(command)

        @Suppress("UNCHECKED_CAST")
        return loxJson.decodeFromString(command.responseType.deserializer, receiveTextMessage()) as RESPONSE
    }

    override suspend fun callRaw(command: String): String {
        val session = ensureSession()
        authenticator?.ensureAuthenticated(this)
        logger.trace { "Sending command: $command" }
        session.send(command)
        return receiveTextMessage()
    }

    override suspend fun callRawForData(command: String): ByteArray {
        TODO("Not yet implemented")
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun close() {
        try {
            logger.debug { "Closing authenticator" }
            authenticator?.close(this@KtorWebsocketLoxoneClient)
        } catch (e: Exception) {
            logger.error(e) { "Error closing authenticator" }
        }
        logger.debug { "Closing session" }
        session?.close(CloseReason(NORMAL, "LoxoneKotlin finished"))
        scope.cancel("Closing the connection")
    }

    private suspend fun ensureSession(): ClientWebSocketSession {
        sessionMutex.withLock {
            if (session == null) {
                client.webSocketSession(
                    host = endpoint?.host,
                    port = endpoint?.port,
                    path = if (endpoint != null) endpoint.path + WS_PATH else WS_PATH,
                    block = {
                        url.protocol = if (endpoint?.useSsl == true) URLProtocol.WSS else URLProtocol.WS
                    }
                ).let { newSession ->
                    logger.debug { "WebSocketSession session created" }
                    session = newSession
                    newSession.incoming.receiveAsFlow().onEach(::processFrame).launchIn(scope)
                    startKeepAlive(newSession)
                }
            }
        }
        return checkNotNull(session) { "WebSocketSession should not be null after init" }
    }

    private suspend fun processFrame(frame: Frame) {
        when (frame.frameType) {
            FrameType.BINARY -> {
                val incomingBinaryMsgHeader = binaryMsgHeader.tryReceive()
                if (incomingBinaryMsgHeader.isSuccess) {
                    val header = incomingBinaryMsgHeader.getOrThrow()
                    logger.trace { "Processing binary message of kind: ${header.kind}" }
                    processBinaryMessage(header, frame.data)
                } else {
                    val header = Codec.readHeader(frame.data)
                    logger.trace { "Incoming message header: $header" }
                    when (header.kind) {
                        MessageKind.KEEP_ALIVE -> keepAliveHeader.send(header)
                        MessageKind.TEXT -> textMsgHeader.send(header)
                        else -> binaryMsgHeader.send(header)
                    }
                }
            }
            FrameType.TEXT -> {
                val textData = frame.data.decodeToString()
                logger.trace { "Incoming message: $textData" }
                textMessages.send(textData)
            }
            else -> error("Unexpected frame of type ${frame.frameType}")
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun processBinaryMessage(header: MessageHeader, data: ByteArray) {
        try {
            when (header.kind) {
                MessageKind.EVENT_VALUE -> processAndEmitEvents("value", data, Codec::readValueEvents)
                MessageKind.EVENT_TEXT -> processAndEmitEvents("text", data, Codec::readTextEvents)
                MessageKind.EVENT_DAYTIMER -> processAndEmitEvents("daytimer", data, Codec::readDaytimerEvents)
                MessageKind.EVENT_WEATHER -> processAndEmitEvents("weather", data, Codec::readWeatherEvents)
                MessageKind.FILE -> {
                    logger.debug { "Received binary file of ${data.size} bytes (binary file handling not implemented)" }
                }
                MessageKind.OUT_OF_SERVICE -> {
                    logger.warn { "Received out-of-service indicator, Miniserver may be updating" }
                }
                else -> {
                    logger.warn { "Unexpected binary message kind: ${header.kind}" }
                }
            }
        } catch (e: IndexOutOfBoundsException) {
            logger.error(e) { "Buffer positioning error while parsing binary message of kind ${header.kind}" }
        } catch (e: IllegalArgumentException) {
            logger.error(e) { "Invalid argument while parsing binary message of kind ${header.kind}" }
        }
    }

    private fun <T : LoxoneEvent> processAndEmitEvents(
        eventType: String,
        data: ByteArray,
        parser: (ByteArray) -> List<T>
    ) {
        val events = parser(data)
        logger.debug { "Received ${events.size} $eventType events" }
        events.forEach { emitEvent(it) }
    }
    

    private fun emitEvent(event: LoxoneEvent) {
        val success = _events.tryEmit(event)
        if (!success) {
            logger.warn { "Event buffer full, dropping event ${event.uuid}" }
        }
    }

    private suspend fun receiveTextMessage() = withTimeout(RCV_TXT_MSG_TIMEOUT) {
        textMsgHeader.receive()
        textMessages.receive()
    }

    private suspend fun startKeepAlive(session: ClientWebSocketSession) = scope.launch {
        while (true) {
            delay(KEEP_ALIVE_INTERVAL)
            session.send(LoxoneCommands.KEEP_ALIVE)
            val keepAliveResponse = withTimeoutOrNull(KEEP_ALIVE_RESPONSE_TIMEOUT) {
                keepAliveHeader.receive()
            }
            if (keepAliveResponse == null) {
                logger.info { "Keepalive response not received within timeout, closing connection" }
                close()
            }
        }
    }

    private suspend fun ClientWebSocketSession.send(command: Command<*>) {
        // TODO is url encoding of segments needed here?
        val joinedCmd = command.pathSegments.joinToString(separator = "/")
        logger.trace { "Sending command: $joinedCmd" }
        send(joinedCmd)
    }

    companion object {
        private const val WS_PATH = "/ws/rfc6455"
        private const val DEFAULT_EVENT_BUFFER_SIZE = 100
        private val RCV_TXT_MSG_TIMEOUT = 10.seconds
        private val KEEP_ALIVE_INTERVAL = 4.minutes
        private val KEEP_ALIVE_RESPONSE_TIMEOUT = 30.seconds
    }
}
