package cz.smarteon.loxkt.ktor

import cz.smarteon.loxkt.Codec
import cz.smarteon.loxkt.Codec.loxJson
import cz.smarteon.loxkt.Command
import cz.smarteon.loxkt.CommandEncryption
import cz.smarteon.loxkt.LoxoneCommands
import cz.smarteon.loxkt.LoxoneCrypto
import cz.smarteon.loxkt.LoxoneEndpoint
import cz.smarteon.loxkt.LoxoneException
import cz.smarteon.loxkt.LoxoneResponse
import cz.smarteon.loxkt.LoxoneTokenAuthenticator
import cz.smarteon.loxkt.WebsocketLoxoneClient
import cz.smarteon.loxkt.event.LoxoneEvent
import cz.smarteon.loxkt.message.LoxoneMsg
import cz.smarteon.loxkt.message.MessageHeader
import cz.smarteon.loxkt.message.MessageKind
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
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
import kotlin.concurrent.Volatile
import kotlin.jvm.JvmOverloads
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Suppress("TooManyFunctions")
class KtorWebsocketLoxoneClient internal constructor(
    private val client: HttpClient,
    private val endpoint: LoxoneEndpoint? = null,
    private val authenticator: LoxoneTokenAuthenticator? = null,
    private val commandEncryption: CommandEncryption = CommandEncryption.NONE,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    eventBufferSize: Int = DEFAULT_EVENT_BUFFER_SIZE
) : WebsocketLoxoneClient {

    @JvmOverloads constructor(
        endpoint: LoxoneEndpoint,
        authenticator: LoxoneTokenAuthenticator? = null,
        commandEncryption: CommandEncryption = CommandEncryption.NONE,
        eventBufferSize: Int = DEFAULT_EVENT_BUFFER_SIZE
    ) : this(
        HttpClient { install(WebSockets) },
        endpoint,
        authenticator,
        commandEncryption,
        Dispatchers.Default,
        eventBufferSize
    )

    private val logger = KotlinLogging.logger {}

    private val sessionMutex = Mutex()
    private var session: ClientWebSocketSession? = null

    /** Serializes the send + receive (+ salt rotation) of a single command over the socket. */
    private val callMutex = Mutex()

    /** Guards the one-time command-encryption key exchange per session. */
    private val keyExchangeMutex = Mutex()
    // @Volatile so the fast-path (unsynchronized) reads in the double-checked key exchange and the
    // reads under callMutex safely observe the values published under keyExchangeMutex.
    @Volatile private var sessionKey: SessionKey? = null
    @Volatile private var publicKey: String? = null

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
        ensureSession()
        // Key exchange must precede authentication: the token commands are encrypted and the
        // Miniserver rejects a key exchange started mid-authentication.
        val willAuthenticate = command.authenticated && authenticator != null
        if (commandEncryption != CommandEncryption.NONE || command.encrypted || willAuthenticate) {
            ensureKeyExchange()
        }
        if (command.authenticated) {
            authenticator?.ensureAuthenticated(this)
        }

        val response = exchange(command.pathSegments.joinToString(separator = "/"), command.encrypted)

        @Suppress("UNCHECKED_CAST")
        return loxJson.decodeFromString(command.responseType.deserializer, response) as RESPONSE
    }

    override suspend fun callRaw(command: String): String {
        ensureSession()
        // callRaw always authenticates (when an authenticator is present), so key exchange may be needed.
        if (commandEncryption != CommandEncryption.NONE || authenticator != null) {
            ensureKeyExchange()
        }
        authenticator?.ensureAuthenticated(this)
        return exchange(command, forceEncrypt = false)
    }

    /**
     * Wraps (encrypting if required), sends [plain] and receives the response, decrypting it when the
     * command was sent over the `fenc` channel. Serialized via [callMutex] because salt rotation and
     * the single-slot response channels require one in-flight command at a time.
     */
    private suspend fun exchange(plain: String, forceEncrypt: Boolean): String = callMutex.withLock {
        val ws = checkNotNull(session) { "WebSocketSession should not be null" }
        val wrapped = wrap(plain, forceEncrypt)
        logger.trace { "Sending command: ${wrapped.wire}" }
        ws.send(wrapped.wire)
        val raw = receiveTextMessage()
        // Advance the salt only after a successful round-trip, so a failed send/receive doesn't
        // leave the client's salt ahead of what the Miniserver accepted.
        wrapped.saltToCommit?.let { sessionKey?.commitSalt(it) }
        if (wrapped.responseEncrypted) {
            val sk = checkNotNull(sessionKey) { "Session key missing for encrypted response" }
            LoxoneCrypto.aesDecrypt(raw, sk.aesKey, sk.iv)
        } else {
            raw
        }
    }

    /**
     * Builds the wire form of [plain]: either the plain command, or the AES-encrypted
     * `jdev/sys/enc/` (response plain) / `jdev/sys/fenc/` (response encrypted) form. Bootstrap
     * the keyexchange command is never encrypted.
     */
    private fun wrap(plain: String, forceEncrypt: Boolean): WrappedCommand {
        val mustEncrypt = forceEncrypt || commandEncryption != CommandEncryption.NONE
        val neverEncrypt = plain.startsWith(KEY_EXCHANGE_PREFIX)
        if (!mustEncrypt || neverEncrypt) return WrappedCommand(plain, responseEncrypted = false)

        val sk = checkNotNull(sessionKey) { "Key exchange must complete before encrypting commands" }
        val (saltPart, nextSalt) = sk.nextSaltPart()
        val cipher = LoxoneCrypto.aesEncrypt("$saltPart/$plain", sk.aesKey, sk.iv)
        // Commands that are mandatorily encrypted (e.g. token acquisition) use the enc channel; only
        // general commands honour REQUEST_RESPONSE to also get an encrypted response (fenc).
        val useFenc = commandEncryption == CommandEncryption.REQUEST_RESPONSE && !forceEncrypt
        val prefix = if (useFenc) FENC_PREFIX else ENC_PREFIX
        return WrappedCommand(
            wire = prefix + cipher.encodeURLParameter(),
            responseEncrypted = useFenc,
            saltToCommit = nextSalt
        )
    }

    /**
     * Performs the one-time command-encryption key exchange for the current session: fetches the
     * Miniserver public key (over HTTP - it is not available on the websocket channel), generates the
     * AES key/iv, and RSA-exchanges them over the websocket.
     */
    private suspend fun ensureKeyExchange() {
        if (sessionKey != null) return
        keyExchangeMutex.withLock {
            if (sessionKey != null) return
            val pem = publicKey ?: fetchPublicKey().also { publicKey = it }
            val key = LoxoneCrypto.generateAesKey()
            val iv = LoxoneCrypto.generateAesIv()
            // The session key is passed as raw Base64 - unlike enc/fenc ciphers, the keyexchange
            // command is not URI-component-encoded (matches the protocol docs and loxone-java).
            val encryptedSessionKey = LoxoneCrypto.createSessionKey(key, iv, pem)
            val response = exchange("$KEY_EXCHANGE_PREFIX$encryptedSessionKey", forceEncrypt = false)
            val msg = loxJson.decodeFromString<LoxoneMsg>(response)
            if (msg.code != LoxoneMsg.CODE_OK) throw LoxoneException("Key exchange failed with code ${msg.code}")
            logger.debug { "Command encryption key exchange completed" }
            sessionKey = SessionKey(key, iv)
        }
    }

    /**
     * Fetches the Miniserver public key over HTTP (`jdev/sys/getPublicKey` is rejected on the
     * websocket channel) and normalizes the certificate-wrapped value to a usable PUBLIC KEY PEM.
     */
    private suspend fun fetchPublicKey(): String {
        val keyResponse = client.get {
            url {
                endpoint?.let {
                    protocol = if (it.useSsl) URLProtocol.HTTPS else URLProtocol.HTTP
                    host = it.host
                    port = it.port
                    encodedPath = it.path.trimEnd('/') + GET_PUBLIC_KEY_PATH
                } ?: run { encodedPath = GET_PUBLIC_KEY_PATH }
            }
        }.bodyAsText()
        val keyMsg = loxJson.decodeFromString<LoxoneMsg>(keyResponse)
        if (keyMsg.code != LoxoneMsg.CODE_OK) {
            throw LoxoneException("Failed to retrieve public key, code ${keyMsg.code}")
        }
        // value is a plain (certificate-wrapped) string, not a {"publicKey":...} object
        return LoxoneCrypto.normalizePublicKeyPem(loxJson.decodeFromString<String>(keyMsg.value))
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
        sessionKey = null
        publicKey = null
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

    // Used solely for the keepalive command, which is never encrypted.
    private suspend fun ClientWebSocketSession.send(command: Command<*>) {
        val joinedCmd = command.pathSegments.joinToString(separator = "/")
        logger.trace { "Sending command: $joinedCmd" }
        send(joinedCmd)
    }

    /** AES session key/iv negotiated during key exchange, with per-command rotating salt. */
    private class SessionKey(val aesKey: ByteArray, val iv: ByteArray) {
        private var prevSalt: String? = null

        /**
         * Computes the salt prefix for the next command without committing the rotation: the first
         * command uses `salt/{salt}`, subsequent ones `nextSalt/{prevSalt}/{nextSalt}` (anti-replay).
         * Returns the prefix paired with the new salt; pass that salt to [commitSalt] only after the
         * command round-trips successfully, so a failed send/receive doesn't desync the salt sequence.
         */
        fun nextSaltPart(): Pair<String, String> {
            val next = LoxoneCrypto.generateSalt()
            val part = prevSalt?.let { "nextSalt/$it/$next" } ?: "salt/$next"
            return part to next
        }

        fun commitSalt(salt: String) {
            prevSalt = salt
        }
    }

    private class WrappedCommand(
        val wire: String,
        val responseEncrypted: Boolean,
        val saltToCommit: String? = null
    )

    companion object {
        private const val WS_PATH = "/ws/rfc6455"
        private const val DEFAULT_EVENT_BUFFER_SIZE = 100
        private const val GET_PUBLIC_KEY_PATH = "/jdev/sys/getPublicKey"
        private const val KEY_EXCHANGE_PREFIX = "jdev/sys/keyexchange/"
        private const val ENC_PREFIX = "jdev/sys/enc/"
        private const val FENC_PREFIX = "jdev/sys/fenc/"
        private val RCV_TXT_MSG_TIMEOUT = 10.seconds
        private val KEEP_ALIVE_INTERVAL = 4.minutes
        private val KEEP_ALIVE_RESPONSE_TIMEOUT = 30.seconds
    }
}
