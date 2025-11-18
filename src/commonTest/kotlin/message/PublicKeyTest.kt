package cz.smarteon.loxkt.message

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class PublicKeyTest : ShouldSpec({

    context("PublicKey command") {
        should("have correct command path") {
            val command = PublicKey.command
            
            command.pathSegments shouldBe listOf("jdev", "sys", "getPublicKey")
        }

        should("not require authentication") {
            val command = PublicKey.command
            
            command.authenticated shouldBe false
        }

        should("expect PublicKey value type") {
            val command = PublicKey.command
            
            command.valueType shouldBe PublicKey::class
        }
    }

    context("PublicKey data class") {
        should("store public key") {
            val pem = """
                -----BEGIN PUBLIC KEY-----
                MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
                -----END PUBLIC KEY-----
            """.trimIndent()
            
            val publicKey = PublicKey(pem)
            
            publicKey.publicKey shouldBe pem
        }
    }
})
