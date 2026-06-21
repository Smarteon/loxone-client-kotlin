package cz.smarteon.loxkt

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Validates [LoxoneCrypto]'s AES against the JDK as an independent oracle, ensuring the multiplatform
 * implementation produces standard AES-256-CBC output with Loxone's ZeroBytePadding.
 */
@OptIn(ExperimentalEncodingApi::class)
class LoxoneCryptoAesJvmTest : ShouldSpec({

    val key = ByteArray(32) { it.toByte() }
    val iv = ByteArray(16) { (it + 1).toByte() }

    fun jdkDecryptNoPadding(base64: String): ByteArray =
        Cipher.getInstance("AES/CBC/NoPadding").run {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            doFinal(Base64.decode(base64))
        }

    context("AES interop with the JDK") {
        should("produce ciphertext the JDK decrypts to the zero-padded plaintext") {
            val plaintext = "salt/AB12/jdev/sps/io/AI1/on"
            val decrypted = jdkDecryptNoPadding(LoxoneCrypto.aesEncrypt(plaintext, key, iv))
            decrypted.decodeToString().trimEnd('\u0000') shouldBe plaintext
            // ZeroBytePadding pads to the block boundary
            (decrypted.size % 16) shouldBe 0
        }

        should("decrypt ciphertext produced by the JDK") {
            val plaintext = "0123456789ABCDEF0123456789ABCDEF" // 32 bytes, block aligned
            val ciphertext = Cipher.getInstance("AES/CBC/NoPadding").run {
                init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
                Base64.encode(doFinal(plaintext.encodeToByteArray()))
            }
            LoxoneCrypto.aesDecrypt(ciphertext, key, iv) shouldBe plaintext
        }
    }
})
