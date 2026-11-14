package cz.smarteon.loxkt

import cz.smarteon.loxkt.LoxoneEndpoint.Companion.local
import cz.smarteon.loxkt.message.Hashing.Companion.commandForUser
import cz.smarteon.loxkt.message.LoxoneMsg
import cz.smarteon.loxkt.message.TestingLoxValues.HASHING
import cz.smarteon.loxkt.message.TestingLoxValues.token
import cz.smarteon.loxkt.message.TestingLoxValues.tokenAuthResponse
import cz.smarteon.loxkt.message.TestingLoxValues.tokenRefreshResponse
import cz.smarteon.loxkt.message.Token
import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class LoxoneTokenAuthenticatorTest : ShouldSpec({

    context("with valid token") {
        val token = Token("token", byteArrayOf(1), TimeUtils.currentLoxoneSeconds().plus(310), 0, false)
        val (authenticator, repository) = authenticatorOf(token)
        val client = wsClient()

        should("authenticate with token") {
            val verification = client.stubMsg("authwithtoken", tokenAuthResponse(TimeUtils.currentLoxoneSeconds().plus(320)))

            authenticator.ensureAuthenticated(client)

            repository.getToken(PROFILE).shouldNotBeNull().asClue {
                it.token shouldBe token.token
                it.key shouldBe token.key
                it.validUntil shouldBeGreaterThan token.validUntil
            }
            verification.matches.size shouldBe 1
        }

        should("not kill a token it was handed on close") {
            val verification = client.stubMsg("killtoken", "", code = "401")

            authenticator.close(client)

            repository.getToken(PROFILE).shouldNotBeNull()
            verification.matches.size shouldBe 0
        }
    }

    context("with no token") {
        val (authenticator, repository) = authenticatorOf()
        val client = wsClient()

        should("request new token and authenticate websocket") {
            val validUntil = TimeUtils.currentLoxoneSeconds().plus(400)
            val getVerification = client.stubMsg("getjwt", token(validUntil))

            authenticator.ensureAuthenticated(client)

            repository.getToken(PROFILE).shouldNotBeNull().asClue {
                it.token shouldBe "8E2AA590E996B321C0E17C3FA9F7A3C17BD376CC"
                it.validUntil shouldBe validUntil
            }
            getVerification.matches.size shouldBe 1
        }

        should("kill the token it got on close") {
            val verification = client.stubMsg("killtoken", "", code = "401")

            authenticator.close(client)

            repository.getToken(PROFILE) shouldBe null
            verification.matches.size shouldBe 1
        }
    }

    context("killing a token without an answer") {
        val token = Token("token", byteArrayOf(1), TimeUtils.currentLoxoneSeconds().plus(4000), 0, false)
        val (authenticator, repository) = authenticatorOf(token)
        val client = wsClient()

        should("forget it all the same") {
            // over a WebSocket the Miniserver may close the session instead of answering
            shouldThrow<IllegalStateException> { authenticator.killToken(client) }

            repository.getToken(PROFILE) shouldBe null
        }
    }

    context("with refresh-needed token") {
        val token = Token("oldTokenValue", byteArrayOf(1), TimeUtils.currentLoxoneSeconds().plus(200), 0, false)
        val (authenticator, repository) = authenticatorOf(token)
        val client = wsClient()

        should("refresh token and authenticate websocket") {
            val newValidUntil = TimeUtils.currentLoxoneSeconds().plus(500)
            val refreshVerification = client.stubMsg("refreshjwt", tokenRefreshResponse(newValidUntil))
            val authVerification = client.stubMsg("authwithtoken", tokenAuthResponse(TimeUtils.currentLoxoneSeconds().plus(510)))

            authenticator.ensureAuthenticated(client)

            repository.getToken(PROFILE).shouldNotBeNull().asClue {
                it.token shouldBe "REFRESHED_TOKEN_VALUE"
                it.key shouldBe token.key
                it.validUntil shouldBeGreaterThan newValidUntil
            }
            refreshVerification.matches.size shouldBe 1
            authVerification.matches.size shouldBe 1
        }
    }

    context("with refresh-needed token and http client") {
        val token = Token("oldTokenValue", byteArrayOf(1), TimeUtils.currentLoxoneSeconds().plus(200), 0, false)
        val (authenticator, repository) = authenticatorOf(token)
        val client = httpClient()

        should("refresh token without authwithtoken") {
            val newValidUntil = TimeUtils.currentLoxoneSeconds().plus(500)
            val refreshVerification = client.stubMsg("refreshjwt", tokenRefreshResponse(newValidUntil))

            authenticator.ensureAuthenticated(client)

            repository.getToken(PROFILE).shouldNotBeNull().asClue {
                it.token shouldBe "REFRESHED_TOKEN_VALUE"
                it.key shouldBe token.key
                it.validUntil shouldBe newValidUntil
            }
            refreshVerification.matches.size shouldBe 1
        }
    }

    context("with already authenticated websocket") {
        val (authenticator, repository) = authenticatorOf(
            Token("token", byteArrayOf(1), TimeUtils.currentLoxoneSeconds().plus(400), 0, false)
        )
        val client = wsClient()

        should("not re-authenticate on second call") {
            client.stubMsg("authwithtoken", tokenAuthResponse(TimeUtils.currentLoxoneSeconds().plus(450)))

            authenticator.ensureAuthenticated(client)
            authenticator.ensureAuthenticated(client)

            repository.getToken(PROFILE).shouldNotBeNull()
        }
    }

    context("closing an HTTP client") {
        val initialToken = Token("token", byteArrayOf(1), TimeUtils.currentLoxoneSeconds().plus(400), 0, false)
        val (authenticator, repository) = authenticatorOf(initialToken)
        val client = httpClient()

        should("not kill token over HTTP") {
            // a closing HTTP client may only have lost its connection: the token is left to expire
            val verification = client.stubMsg("killtoken", "", code = "401")

            authenticator.close(client)

            repository.getToken(PROFILE) shouldBe initialToken
            verification.matches.size shouldBe 0
        }
    }

    context("without password") {
        val tokenOnly = LoxoneProfile(PROFILE.endpoint, LoxoneCredentials(USER))

        should("use and keep the stored token") {
            val token = Token("token", byteArrayOf(1), TimeUtils.currentLoxoneSeconds().plus(400), 0, false)
            val repository = InMemoryTokenRepository().also { it.putToken(tokenOnly, token) }
            val authenticator = LoxoneTokenAuthenticator(tokenOnly, repository)
            val client = wsClient()
            val auth = client.stubMsg("authwithtoken", tokenAuthResponse(TimeUtils.currentLoxoneSeconds().plus(450)))
            val kill = client.stubMsg("killtoken", "", code = "401")

            authenticator.ensureAuthenticated(client)
            authenticator.close(client)

            auth.matches.size shouldBe 1
            kill.matches.size shouldBe 0
            repository.getToken(tokenOnly).shouldNotBeNull().token shouldBe "token"
        }

        should("refuse to authenticate with no valid token, sending nothing") {
            val expired = Token("token", byteArrayOf(1), TimeUtils.currentLoxoneSeconds().plus(30), 0, false)
            val repository = InMemoryTokenRepository().also { it.putToken(tokenOnly, expired) }
            val client = MockLoxoneClient()
            val getKey = client.stubMsg("getkey2", HASHING)

            shouldThrow<LoxoneException> { LoxoneTokenAuthenticator(tokenOnly, repository).ensureAuthenticated(client) }
            getKey.matches.size shouldBe 0
        }

        should("refresh the token on request") {
            val token = Token("oldTokenValue", byteArrayOf(1), TimeUtils.currentLoxoneSeconds().plus(4000), 4, false)
            val repository = InMemoryTokenRepository().also { it.putToken(tokenOnly, token) }
            val client = httpClient()
            val newValidUntil = TimeUtils.currentLoxoneSeconds().plus(9000)
            client.stubMsg("refreshjwt", tokenRefreshResponse(newValidUntil))

            LoxoneTokenAuthenticator(tokenOnly, repository).refreshToken(client)

            repository.getToken(tokenOnly).shouldNotBeNull().asClue {
                it.token shouldBe "REFRESHED_TOKEN_VALUE"
                it.validUntil shouldBe newValidUntil
                it.rights shouldBe 4
            }
        }
    }

    context("with killTokenOnClose disabled") {
        val initialToken = Token("token", byteArrayOf(1), TimeUtils.currentLoxoneSeconds().plus(400), 0, false)
        val (authenticator, repository) = authenticatorOf(initialToken, LoxoneClientSettings(killTokenOnClose = false))

        should("not kill token on close") {
            authenticator.close(MockLoxoneClient())

            repository.getToken(PROFILE) shouldBe initialToken
        }
    }

}) {
    companion object {
        const val USER = "someUserName"
        val PROFILE = LoxoneProfile(local("192.168.7.77"), LoxoneCredentials(USER, "pass"))

        internal fun wsClient() = MockLoxoneClient().apply {
            stubCall(LoxoneMsg("jdev/sys/getkey2/$USER", "200", HASHING)) { it == commandForUser(USER) }
        }

        internal fun httpClient() = MockHttpLoxoneClient().apply {
            stubCall(LoxoneMsg("jdev/sys/getkey2/$USER", "200", HASHING)) { it == commandForUser(USER) }
        }

        internal fun authenticatorOf(
            token: Token? = null,
            settings: LoxoneClientSettings = LoxoneClientSettings()
        ): Pair<LoxoneTokenAuthenticator, InMemoryTokenRepository> {
            val repository = InMemoryTokenRepository().also { if (token != null) it.putToken(PROFILE, token) }
            return LoxoneTokenAuthenticator(PROFILE, repository, settings) to repository
        }
    }
}
