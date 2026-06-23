package cz.smarteon.loxkt.ktor

import cz.smarteon.loxkt.LoxoneEndpoint
import cz.smarteon.loxkt.message.ApiInfo
import cz.smarteon.loxkt.message.TestingLoxValues.API_INFO_MSG_VAL
import cz.smarteon.loxkt.message.TestingMessages.okMsg
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class WebsocketLoxoneClientIT : StringSpec({

    "should call simple command" {
        val ctx = startTestWebSocketServer(
            "jdev/cfg/api" to { _ -> sendLoxoneResponse(okMsg("dev/cfg/api", API_INFO_MSG_VAL)) }
        )
        val bgDispatcher = UnconfinedTestDispatcher()
        val client = KtorWebsocketLoxoneClient(ctx.testedClient, dispatcher = bgDispatcher)

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

        client.close()
    }

    "can be constructed via the public endpoint constructor and closed without connecting" {
        // exercises the public constructor (builds its own HttpClient); no session is opened
        val client = KtorWebsocketLoxoneClient(LoxoneEndpoint.local("127.0.0.1"))
        client.close()
    }
})
