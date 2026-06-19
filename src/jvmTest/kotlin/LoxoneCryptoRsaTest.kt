package cz.smarteon.loxkt

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeEmpty
import java.util.Base64

class LoxoneCryptoRsaTest : ShouldSpec({

    context("RSA encryption") {
        should("encrypt data with public key") {
            val encrypted = LoxoneCrypto.rsaEncrypt("Hello, Loxone!", RsaTestFixtures.TEST_PUBLIC_KEY_PEM)
            encrypted.shouldNotBeEmpty()
            encrypted shouldNotBe "Hello, Loxone!"
        }

        should("produce Base64 encoded output") {
            val encrypted = LoxoneCrypto.rsaEncrypt("test:message", RsaTestFixtures.TEST_PUBLIC_KEY_PEM)
            kotlin.runCatching { Base64.getDecoder().decode(encrypted) }.isSuccess shouldBe true
        }

        should("encrypt and decrypt roundtrip") {
            val testData = "AES256key:InitVector16"
            val encrypted = LoxoneCrypto.rsaEncrypt(testData, RsaTestFixtures.TEST_PUBLIC_KEY_PEM)
            RsaTestFixtures.decryptWithPrivateKey(encrypted) shouldBe testData
        }

        should("produce different output for same input due to PKCS1 random padding") {
            val encrypted1 = LoxoneCrypto.rsaEncrypt("test", RsaTestFixtures.TEST_PUBLIC_KEY_PEM)
            val encrypted2 = LoxoneCrypto.rsaEncrypt("test", RsaTestFixtures.TEST_PUBLIC_KEY_PEM)
            encrypted1 shouldNotBe encrypted2
            RsaTestFixtures.decryptWithPrivateKey(encrypted1) shouldBe "test"
            RsaTestFixtures.decryptWithPrivateKey(encrypted2) shouldBe "test"
        }

        should("encrypt session key in Loxone format") {
            val sessionKey = "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF" +
                ":FEDCBA9876543210FEDCBA9876543210"
            val encrypted = LoxoneCrypto.rsaEncrypt(sessionKey, RsaTestFixtures.TEST_PUBLIC_KEY_PEM)
            RsaTestFixtures.decryptWithPrivateKey(encrypted) shouldBe sessionKey
        }
    }
})
