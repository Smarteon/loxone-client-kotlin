package cz.smarteon.loxone.ktor

import cz.smarteon.loxone.Codec
import cz.smarteon.loxone.Codec.loxJson
import cz.smarteon.loxone.Command
import cz.smarteon.loxone.LoxoneClient
import cz.smarteon.loxone.LoxoneCommands
import cz.smarteon.loxone.LoxoneEndpoint
import cz.smarteon.loxone.LoxoneResponse
import cz.smarteon.loxone.LoxoneTokenAuthenticator
import cz.smarteon.loxone.message.MessageHeader
import cz.smarteon.loxone.message.MessageKind
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import io.ktor.websocket.CloseReason.Codes.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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

class WebsocketLoxoneClient internal constructor(
    private val client: HttpClient,
    private val endpoint: LoxoneEndpoint? = null,
    private val authenticator: LoxoneTokenAuthenticator? = null
) : LoxoneClient {

    @JvmOverloads constructor(
        endpoint: LoxoneEndpoint,
        authenticator: LoxoneTokenAuthenticator? = null
    ) : this(HttpClient { install(WebSockets) }, endpoint, authenticator)

    private val logger = KotlinLogging.logger {}

    private val sessionMutex = Mutex()
    private var session: ClientWebSocketSession? = null

    /**
     * Scope used for all background tasks in the client. Should not be used for executing commands (call* functions).
     */
    private val scope = CoroutineScope(Dispatchers.Default) // TODO think more about correct dispacthers

    private val keepAliveHeader = Channel<MessageHeader>(capacity = 1)
    private val textMsgHeader = Channel<MessageHeader>(capacity = 1)
    private val binaryMsgHeader = Channel<MessageHeader>(capacity = 1)
    private val textMessages = Channel<String>(capacity = 10)

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

    override suspend fun close() {
        scope.cancel("Closing the connection")
        session?.close(CloseReason(NORMAL, "LoxoneKotlin finished"))
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
                    logger.trace { "Incoming binary message" }
                    // TODO process binary message
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

    private suspend fun receiveTextMessage() = withTimeout(RCV_TXT_MSG_TIMEOUT) {
        textMsgHeader.receive()
        textMessages.receive()
    }

    private suspend fun startKeepAlive(session: ClientWebSocketSession) = scope.launch {
        while (true) {
            session.send(LoxoneCommands.KEEP_ALIVE)
            val keepAliveResponse = withTimeoutOrNull(KEEP_ALIVE_RESPONSE_TIMEOUT) {
                keepAliveHeader.receive()
            }
            if (keepAliveResponse == null) {
                logger.info { "Keepalive response not received within timeout, closing connection" }
                close()
            }
            delay(KEEP_ALIVE_INTERVAL)
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
        private val RCV_TXT_MSG_TIMEOUT = 10.seconds
        private val KEEP_ALIVE_INTERVAL = 4.minutes
        private val KEEP_ALIVE_RESPONSE_TIMEOUT = 30.seconds
    }
}
