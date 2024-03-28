package cz.smarteon.loxone.ktor

import cz.smarteon.loxone.LoxoneCredentials
import cz.smarteon.loxone.LoxoneEndpoint.Companion.local
import cz.smarteon.loxone.LoxoneProfile
import cz.smarteon.loxone.LoxoneTokenAuthenticator
import cz.smarteon.loxone.TimeUtils
import cz.smarteon.loxone.message.ApiInfo
import cz.smarteon.loxone.message.LoxoneMsgVal
import cz.smarteon.loxone.message.TestingLoxValues.API_INFO_MSG_VAL
import cz.smarteon.loxone.message.TestingLoxValues.HASHING
import cz.smarteon.loxone.message.TestingLoxValues.token
import cz.smarteon.loxone.message.TestingMessages.htmlError
import cz.smarteon.loxone.message.TestingMessages.okMsg
import cz.smarteon.loxone.message.sysCommand
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.http.*

class HttpLoxoneClientTest : WordSpec({

    val mockEngine = MockEngine {request ->
        fun respondJson(body: String) = respond(body, headers = headersOf("Content-Type", "application/json"))
        fun respondHtmlError(status: HttpStatusCode) =
            respondError(status, htmlError(status), headersOf("Content-Type", "text/html"))

        val path = request.url.encodedPath
        when {
            path == "/jdev/cfg/api" -> respondJson(okMsg("dev/cfg/api", API_INFO_MSG_VAL))

            path == "/jdev/sys/authTest" -> {
                if (request.url.encodedQuery.contains("autht")) {
                    respondJson(okMsg("dev/sys/authTest", "authenticated"))
                } else {
                    respondHtmlError(HttpStatusCode.Unauthorized)
                }
            }

            path == "/jdev/sys/getkey2/user" -> respondJson(okMsg("dev/sys/getkey2/user", HASHING))

            path.startsWith("/jdev/sys/getjwt") -> {
                val validUntil = TimeUtils.currentLoxoneSeconds().plus(60)
                respondJson(okMsg(path, token(validUntil)))
            }

            else -> respondHtmlError(HttpStatusCode.NotFound)
        }
    }

    "not authenticated client" should {
        val client = HttpLoxoneClient(local("10.0.1.77"), null, mockEngine)

        "call raw" {
            client.callRaw("jdev/cfg/api") shouldBe okMsg("dev/cfg/api", API_INFO_MSG_VAL)
        }

        "call" {
            val response = client.call(ApiInfo.command)
            response.code shouldBe "200"
            response.value shouldBe API_INFO_MSG_VAL
            response.control shouldBe "dev/cfg/api"
        }

        "fail call authenticated" {
            shouldThrow<ClientRequestException> {
                client.call(sysCommand<LoxoneMsgVal>("authTest"))
            }
        }

        client.close()
    }

    "authenticated client" should {
        val endpoint = local("10.0.1.77")
        val profile = LoxoneProfile(endpoint, LoxoneCredentials("user", "pass"))
        val client = HttpLoxoneClient(endpoint, LoxoneTokenAuthenticator(profile), mockEngine)

        "call authenticated" {
            val response = client.call(sysCommand<LoxoneMsgVal>("authTest"))
            response.code shouldBe "200"
        }

        client.close()
    }

})
