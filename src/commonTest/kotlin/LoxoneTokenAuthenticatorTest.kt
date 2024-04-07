package cz.smarteon.loxone

import cz.smarteon.loxone.LoxoneEndpoint.Companion.local
import cz.smarteon.loxone.message.EmptyLoxoneMsgVal
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
import org.kodein.mock.Mocker
import org.kodein.mock.UsesMocks

@UsesMocks(WebsocketLoxoneClient::class)
class LoxoneTokenAuthenticatorTest : ShouldSpec({

    context("with valid token") {
        val profile = LoxoneProfile(local("192.168.7.77"), LoxoneCredentials(USER, "pass"))
        val token = Token("token", byteArrayOf(1), TimeUtils.currentLoxoneSeconds().plus(310), 0, false)
        val repository = InMemoryTokenRepository().apply { putToken(profile, token) }

        val authenticator = LoxoneTokenAuthenticator(profile, repository)

        val mocker = Mocker()
        val client: WebsocketLoxoneClient = MockWebsocketLoxoneClient(mocker)
        mocker.everySuspending {
            client.call(commandForUser(USER))
        } returns LoxoneMsg("jdev/sys/getkey2/$USER", "200", HASHING)


        should("authenticate with token") {
            mocker.everySuspending {
                client.call(isLoxMsgCmdContaining<Token>("authwithtoken"))
            } returns LoxoneMsg("authwithtoken", "200", tokenAuthResponse(TimeUtils.currentLoxoneSeconds().plus(320)))

            authenticator.ensureAuthenticated(client)

            repository.getToken(profile).shouldNotBeNull().asClue {
                it.token shouldBe token.token
                it.key shouldBe token.key
                it.validUntil shouldBeGreaterThan token.validUntil
            }
            mocker.verifyWithSuspend(exhaustive = false) { client.call(isNotNull<LoxoneMsgCommand<Token>>()) }
        }

        should("kill token on close") {
            mocker.everySuspending {
                client.call(isLoxMsgCmdContaining<EmptyLoxoneMsgVal>("killtoken"))
            } returns LoxoneMsg("killtoken", "401", "")

            authenticator.close(client)

            repository.getToken(profile) shouldBe null
            mocker.verifyWithSuspend(exhaustive = false) { client.call(isNotNull<LoxoneMsgCommand<EmptyLoxoneMsgVal>>()) }
        }
    }
}) {
    companion object {
        const val USER = "someUserName"
    }
}
