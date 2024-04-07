package cz.smarteon.loxone

import cz.smarteon.loxone.ktor.KtorHttpLoxoneClient
import cz.smarteon.loxone.ktor.KtorWebsocketLoxoneClient
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
    private val httpClient: KtorHttpLoxoneClient
    private val websocketClient: KtorWebsocketLoxoneClient

    override suspend fun afterSpec(spec: Spec) {
        super.afterSpec(spec)
        httpClient.close()
        websocketClient.close()
    }

    init {
        val address = getLoxEnv("ADDRESS")
        val user = getLoxEnv("USER")
        val password = getLoxEnv("PASS")

        val endpoint = LoxoneEndpoint.fromUrl(address)
        val authenticator = LoxoneTokenAuthenticator(LoxoneProfile(endpoint, LoxoneCredentials(user, password)))

        httpClient = KtorHttpLoxoneClient(endpoint, authenticator)
        websocketClient = KtorWebsocketLoxoneClient(endpoint, authenticator)

        include(commonAT(httpClient))
        include(commonAT(websocketClient))
    }

    private fun getLoxEnv(name: String) = "LOX_$name".let { requireNotNull(System.getenv(it)) { "Please set $it env" } }
}
