package cz.smarteon.loxone

import cz.smarteon.loxone.ktor.HttpLoxoneClient
import cz.smarteon.loxone.ktor.WebsocketLoxoneClient
import cz.smarteon.loxone.message.ApiInfo
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.WordSpec
import io.kotest.core.spec.style.wordSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldMatch

/**
 * Common acceptance tests for all LoxoneClient types
 * @param loxoneClient LoxoneClient to test
 */
fun commonAT(loxoneClient: LoxoneClient) = wordSpec {
    "LoxoneClient of type ${loxoneClient::class.simpleName}" should {
        "callRaw jdev/cfg/api" {
            loxoneClient.callRaw("jdev/cfg/api") shouldContain "200"
        }
        "call ApiInfo" {
            loxoneClient.call(ApiInfo.command).code shouldBe "200"
        }
        "callForMsg ApiInfo" {
            loxoneClient.callForMsg(ApiInfo.command).version shouldMatch ".*\\d+\\.\\d+\\.\\d+.*"
        }
    }
}

class LoxoneClientAT : WordSpec() {
    private val httpClient: HttpLoxoneClient
    private val websocketClient: WebsocketLoxoneClient

    override suspend fun afterSpec(spec: Spec) {
        super.afterSpec(spec)
        httpClient.close()
        websocketClient.close()
    }

    init {
        val address = getLoxEnv("ADDRESS")
        val user = getLoxEnv("USER")
        val password = getLoxEnv("PASS")

        // TODO adjust LoxoneEndpoint to parse address properly
        val endpoint = LoxoneEndpoint(address, useSsl = false)
        val authenticator = LoxoneTokenAuthenticator(LoxoneProfile(endpoint, LoxoneCredentials(user, password)))

        httpClient = HttpLoxoneClient(endpoint, authenticator)
        websocketClient = WebsocketLoxoneClient(endpoint, authenticator)

        // TODO websocket client is failing when both clients are tested
        // it's probably related to authentication, however works fine in examples
        include(commonAT(httpClient))
        include(commonAT(websocketClient))
    }

    private fun getLoxEnv(name: String) = "LOX_$name".let { requireNotNull(System.getenv(it)) { "Please set $it env" } }
}
