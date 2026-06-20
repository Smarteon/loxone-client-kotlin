package cz.smarteon.loxkt.message

import cz.smarteon.loxkt.Codec.loxJson
import cz.smarteon.loxkt.message.TestingLoxValues.token
import cz.smarteon.loxkt.message.TestingLoxValues.tokenAuthResponse
import cz.smarteon.loxkt.message.TestingLoxValues.tokenRefreshResponse
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.encodeToString

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

    "should merge equal token returning same instance" {
        val t = loxJson.decodeFromString<Token>(token(342151839))
        t.merge(t) shouldBe t
    }

    "should merge token with non-null incoming key" {
        val noKey = Token(null, null, 342151839, null, false)
        val withKey = loxJson.decodeFromString<Token>(token(342151900))
        noKey.merge(withKey).key shouldBe withKey.key
    }

    "should not equal tokens differing by field" {
        val base = loxJson.decodeFromString<Token>(token(342151839))
        base shouldNotBe base.copy(key = byteArrayOf(1))
        base shouldNotBe base.copy(validUntil = 999L)
        base shouldNotBe base.copy(rights = 999)
        base shouldNotBe base.copy(unsecurePassword = true)
    }

    "should equal same reference and not equal null or other type" {
        val t = loxJson.decodeFromString<Token>(token(342151839))
        (t == t) shouldBe true
        t.equals(null) shouldBe false
        t.equals("not a token") shouldBe false
    }

    "should compute consistent hashCode" {
        val full = loxJson.decodeFromString<Token>(token(342151839))
        val nullFields = loxJson.decodeFromString<Token>(tokenAuthResponse(342151839))
        val nullRights = loxJson.decodeFromString<Token>(tokenRefreshResponse(342151839))
        full.hashCode() shouldBe full.hashCode()
        full.hashCode() shouldNotBe nullFields.hashCode()
        nullFields.hashCode() shouldNotBe 0
        nullRights.hashCode() shouldNotBe 0
    }

    "should serialize and deserialize roundtrip" {
        val t = loxJson.decodeFromString<Token>(tokenAuthResponse(342151839))
        loxJson.decodeFromString<Token>(loxJson.encodeToString(t)) shouldBe t
    }
})
