package cz.smarteon.loxkt

import cz.smarteon.loxkt.ktor.KtorHttpLoxoneClient
import cz.smarteon.loxkt.ktor.KtorWebsocketLoxoneClient
import cz.smarteon.loxkt.message.ApiInfo
import cz.smarteon.loxkt.message.TokenPermission
import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.WordSpec
import io.kotest.core.spec.style.wordSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.string.shouldMatch
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

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

/**
 * Acceptance tests for WebSocket-specific functionality.
 * @param client WebSocket client to test
 * @param authenticator authenticator backing the client
 * @param user Loxone username
 */
fun websocketAT(
    client: KtorWebsocketLoxoneClient,
    authenticator: LoxoneTokenAuthenticator,
    user: String
) = wordSpec {
    "KtorWebsocketLoxoneClient" When {
        "WebSocket token" should {
            "be refreshable via refreshjwt" {
                client.callForMsg(ApiInfo.command)
                val refreshed = client.callForMsg(
                    LoxoneCommands.Tokens.refresh(authenticator.tokenHash(client, "refreshjwt"), user)
                )
                refreshed.validUntil shouldBeGreaterThan 0L
                refreshed.token.shouldNotBeNull()
            }
        }
    }
}

/** APP, expert mode, SYS_WS, user management and firmware update, as Doctor asks for them. */
private val ADMIN_TOKEN_PERMISSION = TokenPermission.APP + TokenPermission.EXPERT_MODE + TokenPermission.SYS_WS +
    TokenPermission.USER_MANAGEMENT + TokenPermission.TRIGGER_UPDATE

private val WEB_TOKEN_LIFETIME = 12.hours

/**
 * A token with [ADMIN_TOKEN_PERMISSION], taken with the password, then used, refreshed and killed over HTTP
 * without it, and a WEB token made from it over a WebSocket. Needs an admin user. Every token it takes is
 * killed again.
 */
fun tokenWithoutPasswordAT(endpoint: LoxoneEndpoint, user: String, password: String) = wordSpec {
    "A token without password" should {
        "be used, refreshed, made a WEB token from and killed" {
            val tokens = InMemoryTokenRepository()
            val withPassword = LoxoneProfile(endpoint, LoxoneCredentials(user, password))
            val settings = LoxoneClientSettings(tokenPermission = ADMIN_TOKEN_PERMISSION, killTokenOnClose = false)
            withTokenClient(endpoint, LoxoneTokenAuthenticator(withPassword, tokens, settings)) {
                it.callRaw("jdev/sps/LoxAPPversion3") shouldContain "200"
            }
            val token = tokens.getToken(withPassword).shouldNotBeNull()
            (ADMIN_TOKEN_PERMISSION in TokenPermission(token.rights.shouldNotBeNull())) shouldBe true

            val tokenOnly = LoxoneProfile(endpoint, LoxoneCredentials(user))
            tokens.putToken(tokenOnly, token)
            val authenticator = LoxoneTokenAuthenticator(tokenOnly, tokens)
            withTokenClient(endpoint, authenticator) { client ->
                client.callRaw("jdev/sps/LoxAPPversion3") shouldContain "200"
                authenticator.refreshToken(client)
                tokens.getToken(tokenOnly).shouldNotBeNull().asClue {
                    it.token shouldNotBe token.token
                    it.rights shouldBe token.rights
                }
                client.callRaw("jdev/sps/LoxAPPversion3") shouldContain "200"
                val previous =
                    LoxoneTokenAuthenticator(tokenOnly, InMemoryTokenRepository().apply { putToken(tokenOnly, token) })
                withTokenClient(endpoint, previous) { it.callRaw("jdev/sps/LoxAPPversion3") shouldContain "200" }

                val ws = KtorWebsocketLoxoneClient(endpoint, authenticator)
                val web = try {
                    authenticator.mintToken(ws, TokenPermission.WEB).also {
                        shouldThrow<LoxoneException> { authenticator.mintToken(ws, TokenPermission.CONFIG) }
                            .code shouldBe "412"
                    }
                } finally {
                    ws.close()
                }
                (TokenPermission.WEB in TokenPermission(web.rights.shouldNotBeNull())) shouldBe true
                (web.secondsToExpireFromNow().seconds - WEB_TOKEN_LIFETIME).absoluteValue shouldBeLessThan 1.hours
                val webOnly =
                    LoxoneTokenAuthenticator(tokenOnly, InMemoryTokenRepository().apply { putToken(tokenOnly, web) })
                withTokenClient(endpoint, webOnly) {
                    it.callRaw("jdev/sps/LoxAPPversion3") shouldContain "200"
                    webOnly.killToken(it)
                }

                // Kills the token from before the refresh too, so this run leaves none behind.
                authenticator.killToken(client)
                tokens.getToken(tokenOnly) shouldBe null
                shouldThrow<LoxoneException> { client.callRaw("jdev/sps/LoxAPPversion3") }
            }
        }
    }
}

private suspend fun withTokenClient(
    endpoint: LoxoneEndpoint,
    authenticator: LoxoneTokenAuthenticator,
    block: suspend (KtorHttpLoxoneClient) -> Unit
) {
    val client = KtorHttpLoxoneClient(endpoint, LoxoneAuth.Token(authenticator))
    try {
        block(client)
    } finally {
        client.close()
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

        httpClient = KtorHttpLoxoneClient(endpoint, LoxoneAuth.Token(authenticator))
        websocketClient = KtorWebsocketLoxoneClient(endpoint, authenticator)

        include(commonAT(httpClient))
        include(commonAT(websocketClient))
        include(websocketAT(websocketClient, authenticator, user))
        include(tokenWithoutPasswordAT(endpoint, user, password))
    }

    private fun getLoxEnv(name: String) = "LOX_$name".let { requireNotNull(System.getenv(it)) { "Please set $it env" } }
}
