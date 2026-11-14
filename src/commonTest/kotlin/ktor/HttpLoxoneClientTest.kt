package cz.smarteon.loxkt.ktor

import cz.smarteon.loxkt.InMemoryTokenRepository
import cz.smarteon.loxkt.LoxoneAuth
import cz.smarteon.loxkt.LoxoneCredentials
import cz.smarteon.loxkt.LoxoneEndpoint
import cz.smarteon.loxkt.LoxoneEndpoint.Companion.local
import cz.smarteon.loxkt.LoxoneProfile
import cz.smarteon.loxkt.LoxoneTokenAuthenticator
import cz.smarteon.loxkt.TimeUtils
import cz.smarteon.loxkt.message.ApiInfo
import cz.smarteon.loxkt.message.LoxoneMsgVal
import cz.smarteon.loxkt.message.TestingLoxValues.API_INFO_MSG_VAL
import cz.smarteon.loxkt.message.TestingLoxValues.HASHING
import cz.smarteon.loxkt.message.TestingLoxValues.token
import cz.smarteon.loxkt.message.TestingLoxValues.tokenRefreshResponse
import cz.smarteon.loxkt.message.TestingMessages.htmlError
import cz.smarteon.loxkt.message.TestingMessages.okMsg
import cz.smarteon.loxkt.message.Token
import cz.smarteon.loxkt.message.sysCommand
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

class HttpLoxoneClientTest : WordSpec({

    val mockEngine = MockEngine { request ->
        fun respondJson(body: String) = respond(body, headers = headersOf("Content-Type", "application/json"))
        fun respondHtmlError(status: HttpStatusCode) =
            respondError(status, htmlError(status), headersOf("Content-Type", "text/html"))

        val path = request.url.encodedPath
        when {
            path == "/jdev/cfg/api" -> respondJson(okMsg("dev/cfg/api", API_INFO_MSG_VAL))

            path == "/jdev/sys/tokenAuthTest" -> {
                if (request.url.encodedQuery.contains("autht")) {
                    respondJson(okMsg("dev/sys/tokenAuthTest", "authenticated"))
                } else {
                    respondHtmlError(HttpStatusCode.Unauthorized)
                }
            }

            path == "/jdev/sys/basicAuthTest" -> {
                if (request.headers.contains("Authorization")) {
                    respondJson(okMsg("dev/sys/basicAuthTest", "authenticated"))
                } else {
                    respondHtmlError(HttpStatusCode.Unauthorized)
                }
            }

            path == "/jdev/sys/getkey2/user" -> respondJson(okMsg("dev/sys/getkey2/user", HASHING))

            path.startsWith("/jdev/sys/getjwt") -> {
                val validUntil = TimeUtils.currentLoxoneSeconds().plus(60)
                respondJson(okMsg(path.substring(1), token(validUntil)))
            }

            request.method == HttpMethod.Post && path.startsWith("/dev/fsput") -> {
                val payloadSize = request.body.toByteArray().size
                respondJson(okMsg(path.substring(1), payloadSize.toString()))
            }

            path == "/jdev/fsget/test" -> respond("success".toByteArray(), status = HttpStatusCode.OK)

            path.startsWith("/testRedirect") -> {
                respondRedirect("http://test.xyz/${path.substringAfter("Redirect/")}")
            }

            else -> respondHtmlError(HttpStatusCode.NotFound)
        }
    }

    "not authenticated client" should {
        val client = KtorHttpLoxoneClient(local("10.0.1.77"), LoxoneAuth.None, mockEngine)

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

        suspend fun withClient(
            auth: LoxoneAuth = LoxoneAuth.Basic(profile),
            block: suspend KtorHttpLoxoneClient.() -> Unit
        ) {
            val client = KtorHttpLoxoneClient(endpoint, auth, mockEngine)
            client.block()
            client.close()
        }

        "call authenticated by token" {
            withClient(LoxoneAuth.Token(LoxoneTokenAuthenticator(profile))) {
                val response = call(sysCommand<LoxoneMsgVal>("tokenAuthTest"))
                response.code shouldBe "200"
            }
        }

        "refresh and kill a token with autht" {
            val engine = MockEngine { request ->
                val path = request.url.encodedPath
                val signed = request.url.parameters.contains("autht")
                fun ok(value: String) =
                    respond(okMsg(path.substring(1), value), headers = headersOf("Content-Type", "application/json"))
                when {
                    path == "/jdev/sys/getkey2/user" -> ok(HASHING)
                    path.startsWith("/jdev/sys/refreshjwt/") && signed ->
                        ok(tokenRefreshResponse(TimeUtils.currentLoxoneSeconds().plus(1000)))
                    path.startsWith("/jdev/sys/killtoken/") && signed -> ok("\"\"")
                    path == "/jdev/sys/tokenAuthTest" && signed -> ok("\"authenticated\"")
                    else -> respondError(HttpStatusCode.Unauthorized)
                }
            }
            val repository = InMemoryTokenRepository()
            repository.putToken(profile, Token("old", byteArrayOf(1), TimeUtils.currentLoxoneSeconds().plus(200), 4, false))
            val authenticator = LoxoneTokenAuthenticator(profile, repository)
            val client = KtorHttpLoxoneClient(endpoint, LoxoneAuth.Token(authenticator), engine)
            try {
                withTimeout(10.seconds) { client.call(sysCommand<LoxoneMsgVal>("tokenAuthTest")) }.code shouldBe "200"
                repository.getToken(profile)?.token shouldBe "REFRESHED_TOKEN_VALUE"

                authenticator.killToken(client)
                repository.getToken(profile) shouldBe null
            } finally {
                client.close()
            }
            engine.requestHistory.map { it.url.encodedPath.split('/')[3] }.filter { it != "getkey2" } shouldBe
                listOf("refreshjwt", "tokenAuthTest", "killtoken")
        }

        "call authenticated by basic auth" {
            withClient {
                val response = call(sysCommand<LoxoneMsgVal>("basicAuthTest"))
                response.code shouldBe "200"
            }
        }

        suspend fun requestsToLogIn(at: LoxoneEndpoint): Int {
            val engine = MockEngine { request ->
                if (request.headers.contains("Authorization")) {
                    respond(
                        okMsg("dev/sys/basicAuthTest", "authenticated"),
                        headers = headersOf("Content-Type", "application/json")
                    )
                } else {
                    respondError(
                        HttpStatusCode.Unauthorized,
                        headers = headersOf("WWW-Authenticate", "Basic realm=\"Loxone\"")
                    )
                }
            }
            val client = KtorHttpLoxoneClient(at, LoxoneAuth.Basic(profile), engine)
            try {
                client.callRaw("jdev/sys/basicAuthTest")
            } finally {
                client.close()
            }
            return engine.requestHistory.size
        }

        "send basic auth on the first request over https" {
            requestsToLogIn(LoxoneEndpoint("miniserver.example")) shouldBe 1
        }

        "send basic auth only when asked over plain http" {
            requestsToLogIn(endpoint) shouldBe 2
        }

        "send basic auth on the first request after a redirect to https" {
            val engine = MockEngine { request ->
                when {
                    !request.url.protocol.isSecure() ->
                        respondRedirect("https://miniserver.example${request.url.encodedPath}")
                    request.headers.contains("Authorization") -> respond(
                        okMsg("dev/sys/basicAuthTest", "authenticated"),
                        headers = headersOf("Content-Type", "application/json")
                    )
                    else -> respondError(
                        HttpStatusCode.Unauthorized,
                        headers = headersOf("WWW-Authenticate", "Basic realm=\"Loxone\"")
                    )
                }
            }
            val client = KtorHttpLoxoneClient(endpoint, LoxoneAuth.Basic(profile), engine)
            try {
                client.callRaw("jdev/sys/basicAuthTest")
            } finally {
                client.close()
            }
            engine.requestHistory.map { it.url.host to it.headers.contains("Authorization") } shouldBe listOf(
                "10.0.1.77" to false,
                "miniserver.example" to true,
            )
        }

        "try a wrong password no more times than before" {
            val engine = MockEngine {
                respondError(
                    HttpStatusCode.Unauthorized,
                    headers = headersOf("WWW-Authenticate", "Basic realm=\"Loxone\"")
                )
            }
            val client = KtorHttpLoxoneClient(LoxoneEndpoint("miniserver.example"), LoxoneAuth.Basic(profile), engine)
            try {
                shouldThrow<ClientRequestException> { client.callRaw("jdev/sys/basicAuthTest") }
            } finally {
                client.close()
            }
            engine.requestHistory.size shouldBe 2
        }

        "call basic auth with redirect" {
            withClient {
                val response = callRaw("testRedirect/jdev/sys/basicAuthTest")
                response shouldBe okMsg("dev/sys/basicAuthTest", "authenticated")
            }
        }

        "call post" {
            withClient {
                val response = postRaw("dev/fsput/test", "test".toByteArray())
                response shouldBe okMsg("dev/fsput/test", "4")
            }
        }

        "call raw for data" {
            withClient {
                val response = callRawForData("jdev/fsget/test")
                response shouldBe "success".toByteArray()
            }
        }
    }

})
