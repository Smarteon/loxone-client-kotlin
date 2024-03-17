package cz.smarteon.loxone.ktor

import cz.smarteon.loxone.Codec
import cz.smarteon.loxone.message.ApiInfo
import cz.smarteon.loxone.message.MessageHeader
import cz.smarteon.loxone.message.MessageKind.TEXT
import cz.smarteon.loxone.message.TestingLoxValues.API_INFO_MSG_VAL
import cz.smarteon.loxone.message.TestingMessages.okMsg
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import io.ktor.client.plugins.websocket.WebSockets as ClientWebsockets
import io.ktor.server.websocket.WebSockets as ServerWebsockets

class WebsocketLoxoneClientIT : StringSpec({

    "should call simple command" {
        testWebsocket {
            val client = WebsocketLoxoneClient(testedClient)

            client.callRaw("jdev/cfg/api") shouldBe okMsg("dev/cfg/api", API_INFO_MSG_VAL)

            val response = client.call(ApiInfo.command)
            response.code shouldBe "200"
            response.value shouldBe API_INFO_MSG_VAL
            response.control shouldBe "dev/cfg/api"

            received shouldContain "jdev/cfg/api"
            received shouldContain "keepalive"

        }
    }

})

private suspend fun testWebsocket(test: suspend ClientTestContext.() -> Unit) = testApplication {
    application { install(ServerWebsockets) }

    val receivedMsgs = mutableListOf<String>()

    routing {
        webSocketRaw(path = "/ws/rfc6455") {
            incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    val payload = frame.readText()
                    receivedMsgs.add(payload)
                    when (payload) {
                        "jdev/cfg/api" -> {
                            val text = okMsg("dev/cfg/api", API_INFO_MSG_VAL).encodeToByteArray()
                            send(Frame.Binary(true, Codec.writeHeader(MessageHeader(TEXT, false, text.size.toLong()))))
                            send(Frame.Text(true, text))
                        }
                    }
                }
            }
        }
    }

    createClient { install(ClientWebsockets) }.use { ClientTestContext(it, receivedMsgs).test() }
}

class ClientTestContext(val testedClient: HttpClient, val received: List<String>)
