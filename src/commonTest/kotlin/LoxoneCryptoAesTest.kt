package cz.smarteon.loxkt

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeEmpty
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class LoxoneCryptoAesTest : ShouldSpec({

    // 32-byte key / 16-byte IV (deterministic test vectors)
    val key = ByteArray(32) { it.toByte() }
    val iv = ByteArray(16) { (it + 1).toByte() }

    context("AES-256-CBC encryption") {
        should("roundtrip block-aligned plaintext") {
            val plaintext = "0123456789ABCDEF" // exactly 16 bytes
            LoxoneCrypto.aesDecrypt(LoxoneCrypto.aesEncrypt(plaintext, key, iv), key, iv) shouldBe plaintext
        }

        should("roundtrip non-block-aligned plaintext (ZeroBytePadding)") {
            val plaintext = "salt/AB12/jdev/sps/io/AI1/on"
            LoxoneCrypto.aesDecrypt(LoxoneCrypto.aesEncrypt(plaintext, key, iv), key, iv) shouldBe plaintext
        }

        should("produce Base64 output that differs from the plaintext") {
            val plaintext = "salt/AB12/jdev/sps/io/AI1/on"
            val encrypted = LoxoneCrypto.aesEncrypt(plaintext, key, iv)
            encrypted.shouldNotBeEmpty()
            encrypted shouldNotBe plaintext
            // ciphertext is padded to the 16-byte block size
            (Base64.decode(encrypted).size % 16) shouldBe 0
        }

        should("be deterministic for a fixed key and iv") {
            val plaintext = "salt/AB12/jdev/sps/io/AI1/on"
            LoxoneCrypto.aesEncrypt(plaintext, key, iv) shouldBe LoxoneCrypto.aesEncrypt(plaintext, key, iv)
        }
    }

    context("key material generation") {
        should("generate a 32-byte AES key") {
            LoxoneCrypto.generateAesKey().size shouldBe 32
        }

        should("generate a 16-byte IV") {
            LoxoneCrypto.generateAesIv().size shouldBe 16
        }

        should("generate random keys") {
            LoxoneCrypto.generateAesKey() shouldNotBe LoxoneCrypto.generateAesKey()
        }

        should("generate a non-empty hex salt") {
            LoxoneCrypto.generateSalt().shouldNotBeEmpty()
        }
    }
})
