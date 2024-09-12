package cz.smarteon.loxone

import cz.smarteon.loxone.LoxoneEndpoint.Companion.local
import cz.smarteon.loxone.message.Hashing.Companion.commandForUser
import cz.smarteon.loxone.message.LoxoneMsg
import cz.smarteon.loxone.message.TestingLoxValues.HASHING
import cz.smarteon.loxone.message.TestingLoxValues.tokenAuthResponse
import cz.smarteon.loxone.message.Token
import io.kotest.assertions.asClue
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class LoxoneTokenAuthenticatorTest : ShouldSpec({

    context("with valid token") {
        val profile = LoxoneProfile(local("192.168.7.77"), LoxoneCredentials(USER, "pass"))
        val token = Token("token", byteArrayOf(1), TimeUtils.currentLoxoneSeconds().plus(310), 0, false)
        val repository = InMemoryTokenRepository().apply { putToken(profile, token) }

        val authenticator = LoxoneTokenAuthenticator(profile, repository)

        val client = MockLoxoneClient()
        client.stubCall(LoxoneMsg("jdev/sys/getkey2/$USER", "200", HASHING)) { it == commandForUser(USER) }

        should("authenticate with token") {
            val verification = client.stubCall(LoxoneMsg("authwithtoken", "200", tokenAuthResponse(TimeUtils.currentLoxoneSeconds().plus(320)))) {
                it.pathSegments.contains("authwithtoken")
            }

            authenticator.ensureAuthenticated(client)

            repository.getToken(profile).shouldNotBeNull().asClue {
                it.token shouldBe token.token
                it.key shouldBe token.key
                it.validUntil shouldBeGreaterThan token.validUntil
            }
            verification.matches.size shouldBe 1
        }

        should("kill token on close") {
            val verification = client.stubCall(LoxoneMsg("killtoken", "401", "")) {
                it.pathSegments.contains("killtoken")
            }

            authenticator.close(client)

            repository.getToken(profile) shouldBe null
            verification.matches.size shouldBe 1
        }
    }
}) {
    companion object {
        const val USER = "someUserName"
    }
}
