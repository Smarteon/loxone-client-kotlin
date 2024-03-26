package cz.smarteon.loxone.ktor

import cz.smarteon.loxone.Codec.writeHeader
import cz.smarteon.loxone.message.ApiInfo
import cz.smarteon.loxone.message.MessageHeader
import cz.smarteon.loxone.message.MessageKind.TEXT
import cz.smarteon.loxone.message.TestingLoxValues.API_INFO_MSG_VAL
import cz.smarteon.loxone.message.TestingMessages.okMsg
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.time.Duration.Companion.seconds
import io.ktor.client.plugins.websocket.WebSockets as ClientWebsockets
import io.ktor.server.websocket.WebSockets as ServerWebsockets

@OptIn(ExperimentalCoroutinesApi::class)
class WebsocketLoxoneClientIT : StringSpec({

    "should call simple command" {
        val ctx = startTestWebsocketServer()
        val bgDispatcher = UnconfinedTestDispatcher()
        val client = WebsocketLoxoneClient(ctx.testedClient, dispatcher = bgDispatcher)

        client.callRaw("jdev/cfg/api") shouldBe okMsg("dev/cfg/api", API_INFO_MSG_VAL)
        ctx.received.receive() shouldBe "jdev/cfg/api"

        val response = client.call(ApiInfo.command)
        response.code shouldBe "200"
        response.value shouldBe API_INFO_MSG_VAL
        response.control shouldBe "dev/cfg/api"
        ctx.received.receive() shouldBe "jdev/cfg/api"

        // keepalive is sent every 4 minutes
        bgDispatcher.scheduler.advanceTimeBy(245.seconds)
        ctx.received.receive() shouldBe "keepalive"
    }
})

private suspend fun startTestWebsocketServer(): ClientTestContext {
    lateinit var clientTestContext: ClientTestContext

    ApplicationTestBuilder().apply {
        application { install(ServerWebsockets) }

        val receivedMsgs = Channel<String>(capacity = Channel.BUFFERED)

        routing {
            webSocketRaw(path = "/ws/rfc6455") {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val payload = frame.readText()
                        receivedMsgs.send(payload)
                        when (payload) {
                            "jdev/cfg/api" -> {
                                val text = okMsg("dev/cfg/api", API_INFO_MSG_VAL).encodeToByteArray()
                                send(Frame.Binary(true, writeHeader(MessageHeader(TEXT, false, text.size.toLong()))))
                                send(Frame.Text(true, text))
                            }

                            "keepalive" -> {
                                send(Frame.Binary(true, writeHeader(MessageHeader.KEEP_ALIVE)))
                            }
                        }
                    }
                }
            }
        }

        val client = createClient { install(ClientWebsockets) }
        clientTestContext = ClientTestContext(client, receivedMsgs)
    }

    return clientTestContext
}

private class ClientTestContext(val testedClient: HttpClient, val received: Channel<String>)
