package cz.smarteon.loxone.ktor

import cz.smarteon.loxone.Codec.writeHeader
import cz.smarteon.loxone.message.ApiInfo
import cz.smarteon.loxone.message.MessageHeader
import cz.smarteon.loxone.message.MessageKind.TEXT
import cz.smarteon.loxone.message.TestingLoxValues.API_INFO_MSG_VAL
import cz.smarteon.loxone.message.TestingMessages.okMsg
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
import io.ktor.client.plugins.websocket.WebSockets as ClientWebsockets
import io.ktor.server.websocket.WebSockets as ServerWebsockets

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class WebsocketLoxoneClientIT : ShouldSpec({

    should("call simple command").config(coroutineTestScope = true) {
        testWebsocket {
            val client = WebsocketLoxoneClient(testedClient, dispatcher = coroutineContext[CoroutineDispatcher]!!)

            client.callRaw("jdev/cfg/api") shouldBe okMsg("dev/cfg/api", API_INFO_MSG_VAL)

            val response = async(Dispatchers.Default) { client.call(ApiInfo.command) }.await()
            response.code shouldBe "200"
            response.value shouldBe API_INFO_MSG_VAL
            response.control shouldBe "dev/cfg/api"

            received shouldContain "jdev/cfg/api"

            testCoroutineScheduler.advanceTimeBy(5.minutes)
            received shouldContain "keepalive"
        }
    }
})

// We can't share the inner WebsocketLoxoneClient dispatcher with the server part, they would block each other.
// However, to test the client with virtual time we pass the test dispatcher to the client.
// Therefore, we use GlobalScope.launch to run the server part in a separate dispatcher.
@OptIn(DelicateCoroutinesApi::class)
private suspend fun testWebsocket(test: suspend ClientTestContext.() -> Unit) = GlobalScope.launch {
    testApplication {
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

        createClient { install(ClientWebsockets) }.use { ClientTestContext(it, receivedMsgs).test() }
    }
}

class ClientTestContext(val testedClient: HttpClient, val received: List<String>)
