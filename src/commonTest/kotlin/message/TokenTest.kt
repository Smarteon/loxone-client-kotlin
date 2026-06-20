package cz.smarteon.loxkt.message

import cz.smarteon.loxkt.Codec.loxJson
import cz.smarteon.loxkt.message.TestingLoxValues.token
import cz.smarteon.loxkt.message.TestingLoxValues.tokenAuthResponse
import cz.smarteon.loxkt.message.TestingLoxValues.tokenRefreshResponse
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

    "should deserialize auth response" {
        loxJson.decodeFromString<Token>(tokenAuthResponse(342151839)) shouldBe Token(
            null,
            null,
            342151839,
            1666,
            false
        )
    }

    "should merge token" {
        val original = loxJson.decodeFromString<Token>(token(342151839))
        original.merge(Token(validUntil = 342151900, rights = 1667, unsecurePassword = true)) shouldBe Token(
            "8E2AA590E996B321C0E17C3FA9F7A3C17BD376CC",
            byteArrayOf(68, 68, 50),
            342151900,
            1667,
            true
        )
    }

    "should deserialize refresh response" {
        loxJson.decodeFromString<Token>(tokenRefreshResponse(342151839)) shouldBe Token(
            "REFRESHED_TOKEN_VALUE",
            null,
            342151839,
            null,
            false
        )
    }

    "should merge refresh response preserving rights" {
        val original = Token("origToken", byteArrayOf(1), 342151839, 1666, false)
        val refreshResponse = loxJson.decodeFromString<Token>(tokenRefreshResponse(342151900))
        original.merge(refreshResponse) shouldBe Token(
            "REFRESHED_TOKEN_VALUE",
            byteArrayOf(1),
            342151900,
            1666,
            false
        )
    }
})
