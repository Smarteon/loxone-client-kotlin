package cz.smarteon.loxkt.ktor

import cz.smarteon.loxkt.RsaTestFixtures
import cz.smarteon.loxkt.callForMsg
import cz.smarteon.loxkt.message.PublicKey
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class PublicKeyIntegrationTest : StringSpec({

    "should retrieve public key from miniserver" {
        val ctx = startTestWebSocketServer(
            "jdev/sys/getPublicKey" to { _ ->
                val escaped = RsaTestFixtures.TEST_PUBLIC_KEY_PEM.replace("\n", "\\n")
                sendLoxoneResponse(
                    """{"LL":{"control":"dev/sys/getPublicKey","value":{"publicKey":"$escaped"},"code":"200"}}"""
                )
            }
        )
        val bgDispatcher = UnconfinedTestDispatcher()
        val client = KtorWebsocketLoxoneClient(ctx.testedClient, dispatcher = bgDispatcher)

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
