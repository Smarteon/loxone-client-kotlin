package cz.smarteon.loxone

import cz.smarteon.loxone.message.EmptyLoxoneMsgVal
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
})
