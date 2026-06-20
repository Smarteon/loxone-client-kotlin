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

        should("kill token on close") {
            val verification = client.stubMsg("killtoken", "", code = "401")

            authenticator.close(client)

            repository.getToken(PROFILE) shouldBe null
            verification.matches.size shouldBe 1
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
