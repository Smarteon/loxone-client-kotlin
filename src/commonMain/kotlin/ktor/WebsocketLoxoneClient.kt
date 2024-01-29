package cz.smarteon.loxone.ktor

import co.touchlab.stately.concurrency.AtomicReference
import cz.smarteon.loxone.Codec
import cz.smarteon.loxone.Codec.loxJson
import cz.smarteon.loxone.Command
import cz.smarteon.loxone.LoxoneClient
import cz.smarteon.loxone.LoxoneEndpoint
import cz.smarteon.loxone.LoxoneResponse
import cz.smarteon.loxone.LoxoneTokenAuthenticator
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import io.ktor.websocket.CloseReason.Codes.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

class WebsocketLoxoneClient(
    private val endpoint: LoxoneEndpoint,
    private val authenticator: LoxoneTokenAuthenticator? = null
) : LoxoneClient {

    private val client = HttpClient {
        install(WebSockets)
    }

    private val webSocketSession = AtomicReference<ClientWebSocketSession?>(null)

    private val scope = CoroutineScope(Dispatchers.Default) // TODO think more about correct dispacthers

    private val textMessages = Channel<String>(capacity = 10)

    override suspend fun <RESPONSE : LoxoneResponse> call(command: Command<RESPONSE>): RESPONSE {
        val session = ensureSession()
        if (command.authenticated) {
            authenticator?.ensureAuthenticated(this)
        }

        session.send(command.pathSegments.joinToString(separator = "/")) // TODO is url encoding of segments needed here?

        @Suppress("UNCHECKED_CAST")
        return loxJson.decodeFromString(command.responseType.deserializer, textMessages.receive()) as RESPONSE
    }

    override suspend fun callRaw(command: String): String {
        val session = ensureSession()
        authenticator?.ensureAuthenticated(this)
        session.send(command)
        return textMessages.receive()
    }

    override suspend fun close() {
        scope.cancel("Closing the connection")
        webSocketSession.get()?.close(CloseReason(NORMAL, "LoxoneKotlin finished"))
    }

    private suspend fun ensureSession(): ClientWebSocketSession {
        webSocketSession.compareAndSet(null, client.webSocketSession (
            host = endpoint.host,
            port = endpoint.port,
            path = endpoint.path + WS_PATH,
            block = {
                url.protocol = if (endpoint.useSsl) URLProtocol.WSS else URLProtocol.WS
            }
        ))
        return checkNotNull(webSocketSession.get()) { "WebSocketSession should not be null right after init" }
            .also { session ->
                session.incoming.receiveAsFlow().onEach(::processFrame).launchIn(scope)
            }
    }

    private suspend fun processFrame(frame: Frame) {
        when(frame.frameType) {
            FrameType.BINARY -> println(Codec.readHeader(frame.data))
            FrameType.TEXT -> textMessages.send(frame.data.decodeToString())
            else -> error("Unexpected frame of type ${frame.frameType}")
        }
    }

    companion object {
        private const val WS_PATH = "/ws/rfc6455"
    }
}
