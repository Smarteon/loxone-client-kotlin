package cz.smarteon.loxkt.message

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class PublicKeyTest : ShouldSpec({

    context("PublicKey command") {
        should("have correct command path") {
            PublicKey.command.pathSegments shouldBe listOf("jdev", "sys", "getPublicKey")
        }

        should("not require authentication") {
            PublicKey.command.authenticated shouldBe false
        }

        should("have correct value type") {
            PublicKey.command.valueType shouldBe PublicKey::class
        }
    }

    context("PublicKey data class") {
        should("store public key string") {
            val pem = "-----BEGIN PUBLIC KEY-----\nMIIBIjAN...\n-----END PUBLIC KEY-----"
            PublicKey(pem).publicKey shouldBe pem
        }

        should("deserialize from JSON") {
            val publicKey = Json.decodeFromString<PublicKey>("""{"publicKey":"test-pem-content"}""")
            publicKey.publicKey shouldBe "test-pem-content"
        }

        should("serialize to JSON") {
            val json = Json.encodeToString(PublicKey.serializer(), PublicKey("my-key"))
            json shouldBe """{"publicKey":"my-key"}"""
        }
    }
})
