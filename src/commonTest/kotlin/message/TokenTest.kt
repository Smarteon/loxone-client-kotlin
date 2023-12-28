package cz.smarteon.loxone.message

import cz.smarteon.loxone.Codec.loxJson
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TokenTest : StringSpec({

    "should deserialize" {
        //language=JSON
        val tokenJson = """
            {
              "token": "8E2AA590E996B321C0E17C3FA9F7A3C17BD376CC",
              "key": "444432",
              "validUntil": 342151839,
              "tokenRights": 1666,
              "unsecurePass": false
            }
        """.trimIndent()

        loxJson.decodeFromString<Token>(tokenJson) shouldBe Token(
            "8E2AA590E996B321C0E17C3FA9F7A3C17BD376CC",
            byteArrayOf(68, 68, 50),
            342151839,
            1666,
            false
        )
    }
})
