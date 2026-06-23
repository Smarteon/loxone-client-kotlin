package cz.smarteon.loxkt

import cz.smarteon.loxkt.message.EmptyLoxoneMsgVal
import cz.smarteon.loxkt.message.Token
import cz.smarteon.loxkt.message.TokenPermission
import io.kotest.assertions.asClue
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class LoxoneCommandsTest : ShouldSpec({

    should("default commands to not encrypted") {
        // KEEP_ALIVE does not override Command.encrypted, exercising the interface default
        LoxoneCommands.KEEP_ALIVE.encrypted shouldBe false
    }

    should("create kill token command") {
        LoxoneCommands.Tokens.kill("hash", "user").asClue {
            it.pathSegments shouldBe listOf("jdev", "sys", "killtoken", "hash", "user")
            it.valueType shouldBe EmptyLoxoneMsgVal::class
            it.expectedCode shouldBe "401"
        }
    }

    should("create gettoken command") {
        LoxoneCommands.Tokens.get("hash", "user", TokenPermission.WEB).asClue {
            it.pathSegments shouldBe listOf(
                "jdev",
                "sys",
                "getjwt",
                "hash",
                "user",
                "2",
                "df184362-73fc-5d3e-ab0ec7c1c3e5bb2e",
                "loxoneKotlin"
            )
            it.valueType shouldBe Token::class
            it.authenticated shouldBe false
            it.expectedCode shouldBe "200"
            it.encrypted shouldBe true
        }
    }

    should("create authwithtoken command") {
        LoxoneCommands.Tokens.auth("hash", "user").asClue {
            it.pathSegments shouldBe listOf("authwithtoken", "hash", "user")
            it.valueType shouldBe Token::class
            it.authenticated shouldBe false
            it.expectedCode shouldBe "200"
            it.encrypted shouldBe true
        }
    }

    should("create refresh token command") {
        LoxoneCommands.Tokens.refresh("hash", "user").asClue {
            it.pathSegments shouldBe listOf("jdev", "sys", "refreshjwt", "hash", "user")
            it.valueType shouldBe Token::class
            it.authenticated shouldBe false
            it.expectedCode shouldBe "200"
        }
    }
})
