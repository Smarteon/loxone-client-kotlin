package cz.smarteon.loxone

import cz.smarteon.loxone.message.EmptyLoxoneMsgVal
import cz.smarteon.loxone.message.Token
import cz.smarteon.loxone.message.TokenPermission
import io.kotest.assertions.asClue
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class LoxoneCommandsTest : ShouldSpec({

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
        }
    }

    should("create authwithtoken command") {
        LoxoneCommands.Tokens.auth("hash", "user").asClue {
            it.pathSegments shouldBe listOf("authwithtoken", "hash", "user")
            it.valueType shouldBe Token::class
            it.authenticated shouldBe false
            it.expectedCode shouldBe "200"
        }
    }
})
