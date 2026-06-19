package cz.smarteon.loxkt.ktor

import cz.smarteon.loxkt.Codec.writeHeader
import cz.smarteon.loxkt.message.MessageHeader
import cz.smarteon.loxkt.message.MessageKind.TEXT
import io.ktor.client.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import io.ktor.client.plugins.websocket.WebSockets as ClientWebsockets
import io.ktor.server.websocket.WebSockets as ServerWebsockets

data class WebSocketTestContext(val testedClient: HttpClient, val received: Channel<String>)

/**
 * Starts a Ktor test WebSocket server on /ws/rfc6455 with built-in keepalive handling.
 * Each handler receives the session as receiver and the matched command string.
 */
suspend fun startTestWebSocketServer(
    vararg handlers: Pair<String, suspend WebSocketServerSession.(String) -> Unit>
): WebSocketTestContext {
    val handlerMap = handlers.toMap()
    lateinit var ctx: WebSocketTestContext

    ApplicationTestBuilder().apply {
        application { install(ServerWebsockets) }
        val received = Channel<String>(Channel.BUFFERED)

        routing {
            webSocketRaw(path = "/ws/rfc6455") {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val payload = frame.readText()
                        received.send(payload)
                        when (payload) {
                            "keepalive" -> send(Frame.Binary(true, writeHeader(MessageHeader.KEEP_ALIVE)))
                            else -> handlerMap[payload]?.invoke(this, payload)
                        }
                    }
                }
            }
        }

        ctx = WebSocketTestContext(createClient { install(ClientWebsockets) }, received)
    }

    return ctx
}

/** Sends a two-frame Loxone response: binary message header followed by the JSON text payload. */
suspend fun WebSocketServerSession.sendLoxoneResponse(json: String) {
    val bytes = json.encodeToByteArray()
    send(Frame.Binary(true, writeHeader(MessageHeader(TEXT, false, bytes.size.toLong()))))
    send(Frame.Text(true, bytes))
}
