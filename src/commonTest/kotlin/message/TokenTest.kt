package cz.smarteon.loxone.message

import cz.smarteon.loxone.Codec.loxJson
import cz.smarteon.loxone.message.TestingLoxValues.token
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TokenTest : StringSpec({

    "should deserialize" {
        loxJson.decodeFromString<Token>(token(342151839)) shouldBe Token(
            "8E2AA590E996B321C0E17C3FA9F7A3C17BD376CC",
            byteArrayOf(68, 68, 50),
            342151839,
            1666,
            false
        )
    }
})
