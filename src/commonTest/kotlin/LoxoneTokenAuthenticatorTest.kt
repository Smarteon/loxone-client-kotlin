package cz.smarteon.loxone

import cz.smarteon.loxone.LoxoneEndpoint.Companion.local
import cz.smarteon.loxone.message.EmptyLoxoneMsgVal
import cz.smarteon.loxone.message.LoxoneMsg
import cz.smarteon.loxone.message.Token
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.kodein.mock.Mocker

class LoxoneTokenAuthenticatorTest : ShouldSpec({

    should("kill token on close") {
        val profile = LoxoneProfile(local("192.168.7.77"), LoxoneCredentials("user", "pass"))
        val token = Token("token", byteArrayOf(1), TimeUtils.currentLoxoneSeconds().plus(310), 0, false)
        val repository = InMemoryTokenRepository().apply { putToken(profile, token) }

        val authenticator = LoxoneTokenAuthenticator(profile, repository)

        val mocker = Mocker()
        val client: LoxoneClient = MockLoxoneClient(mocker)
        mocker.everySuspending {
            client.call(isNotNull<LoxoneMsgCommand<EmptyLoxoneMsgVal>>())
        } returns LoxoneMsg("killtoken", "401", "")

        authenticator.close(client)

        repository.getToken(profile) shouldBe null
        mocker.verifyWithSuspend { client.call(isNotNull<LoxoneMsgCommand<EmptyLoxoneMsgVal>>()) }
    }
})
