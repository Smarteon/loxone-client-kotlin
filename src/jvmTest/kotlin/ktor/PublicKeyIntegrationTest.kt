package cz.smarteon.loxkt.ktor

import cz.smarteon.loxkt.Codec.writeHeader
import cz.smarteon.loxkt.callForMsg
import cz.smarteon.loxkt.message.MessageHeader
import cz.smarteon.loxkt.message.MessageKind.TEXT
import cz.smarteon.loxkt.message.PublicKey
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import io.ktor.client.plugins.websocket.WebSockets as ClientWebsockets
import io.ktor.server.websocket.WebSockets as ServerWebsockets

@OptIn(ExperimentalCoroutinesApi::class)
class PublicKeyIntegrationTest : StringSpec({

    val testPublicKey = """
        -----BEGIN PUBLIC KEY-----
        MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAr3wViebzGQCZbK2FrW6c
        bpiSbHFXyfGRCnnrGqRiflpK0YE29ODWdla0B9rXYQTbHTI2xbXdGTE1o/Ji9z8u
        7IkNjLE6nVAdCNveITAGeU1hOT72o1jKYTPRO3ABT2A/PGQvRAhohJ/qOqaK+nqm
        i2YdzZpozON6EijMb90pMz2KPCb6QAyBrlwf0HC1PCyaXRc1AeZs79y/gT+AcGys
        9lq817df8bBA9E19ZipQGuMfU0UhvudygTBHIp32tdfGbNTfu0GEm3baSxyZIiQG
        xoE+kb6vevhq7qZdBcb+fidcbFJpdt3QjQymlKA16CoLDNXAvtVD8iQARfGpZJ4q
        WwIDAQAB
        -----END PUBLIC KEY-----
    """.trimIndent()

    "should retrieve public key from miniserver" {
        val ctx = startTestPublicKeyWebsocketServer(testPublicKey)
        val bgDispatcher = UnconfinedTestDispatcher()
        val client = KtorWebsocketLoxoneClient(ctx.testedClient, dispatcher = bgDispatcher)

        // Test the command
        val msg = client.call(PublicKey.command)
        val publicKeyValue = client.callForMsg(PublicKey.command)
        
        msg.code shouldBe "200"
        msg.control shouldBe "dev/sys/getPublicKey"
        publicKeyValue.publicKey shouldContain "BEGIN PUBLIC KEY"
        publicKeyValue.publicKey shouldContain "END PUBLIC KEY"
        ctx.received.receive() shouldBe "jdev/sys/getPublicKey"
        ctx.received.receive() shouldBe "jdev/sys/getPublicKey"

        client.close()
    }
})

private suspend fun startTestPublicKeyWebsocketServer(publicKey: String): PublicKeyClientTestContext {
    lateinit var clientTestContext: PublicKeyClientTestContext

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
                            "jdev/sys/getPublicKey" -> {
                                val escapedKey = publicKey.replace("\n", "\\n")
                                val responseJson = """
                                    {
                                        "LL": {
                                            "control": "dev/sys/getPublicKey",
                                            "value": {
                                                "publicKey": "$escapedKey"
                                            },
                                            "code": "200"
                                        }
                                    }
                                """.trimIndent()
                                val text = responseJson.encodeToByteArray()
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
        clientTestContext = PublicKeyClientTestContext(client, receivedMsgs)
    }

    return clientTestContext
}

private class PublicKeyClientTestContext(val testedClient: HttpClient, val received: Channel<String>)
