package cz.smarteon.loxkt.message

import cz.smarteon.loxkt.LoxoneCommands.Tokens
import cz.smarteon.loxkt.LoxoneException
import cz.smarteon.loxkt.LoxoneLoginCodeException
import cz.smarteon.loxkt.MockLoxoneClient
import cz.smarteon.loxkt.callForMsg
import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldNotBeInstanceOf

/** Answers to `getjwt` as firmware 17.3.11.12 and 17.3.11.23 sent them (2026-11-26). */
class LoginCodeTest : ShouldSpec({

    should("read the refusals of a login without the right second factor") {
        listOf(
            Triple(
                "460",
                """{"otp":{"mail":"sent","to":"m***@example.com","length":6,"expiresIn":600,"resendIn":30}}""",
                LoginCode(mail = "sent", to = "m***@example.com", length = 6, expiresIn = 600, resendIn = 30)
            ),
            Triple(
                "460",
                """{"otp":{"methods":["totp","email"],"default":"totp","unavailable":{},"length":6}}""",
                LoginCode(methods = listOf("totp", "email"), default = "totp", length = 6)
            ),
            // a user with no method yet is offered to set one up, which this library doesn't do
            Triple(
                "460",
                """{"otp":{"methods":[],"unavailable":{},"setup":["email","totp"],"setupTicket":"0f1e","length":6}}""",
                LoginCode(length = 6)
            ),
            Triple("462", """{"otp":{"mail":"noAddress"}}""", LoginCode(mail = "noAddress")),
            Triple("463", """{"otp":{"attemptsLeft":4}}""", LoginCode(attemptsLeft = 4)),
            Triple("463", """{"otp":{"attemptsLeft":9,"reason":"codeUsed"}}""", LoginCode(attemptsLeft = 9, reason = "codeUsed")),
            Triple("466", """{"otp":{"retryAfter":900}}""", LoginCode(retryAfter = 900)),
            Triple("468", """{"otp":{}}""", LoginCode()),
            Triple("470", """{"otp":{"totpBlockedUntil":1795774309}}""", LoginCode(totpBlockedUntil = 1795774309)),
            // the same as a string holding the JSON
            Triple("466", """"{\"otp\":{\"retryAfter\":900}}"""", LoginCode(retryAfter = 900)),
        ).forEach { (code, value, loginCode) ->
            value.asClue {
                refusedLogin(code, value).shouldBeInstanceOf<LoxoneLoginCodeException>().asClue {
                    it.code shouldBe code
                    it.loginCode shouldBe loginCode
                }
            }
        }
    }

    should("leave an unreadable or other refusal a plain LoxoneException") {
        listOf("460" to "\"\"", "463" to """{"otp":"x"}""", "401" to """{"otp":{}}""").forEach { (code, value) ->
            refusedLogin(code, value).asClue {
                it.shouldNotBeInstanceOf<LoxoneLoginCodeException>()
                it.code shouldBe code
            }
        }
    }
})

private suspend fun refusedLogin(code: String, value: String): LoxoneException {
    val client = MockLoxoneClient().apply { stubMsg("getjwt", value, code) }
    return shouldThrow<LoxoneException> { client.callForMsg(Tokens.get("hash", "user", TokenPermission.APP)) }
}
