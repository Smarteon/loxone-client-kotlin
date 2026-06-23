package cz.smarteon.loxkt

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.RSA
import dev.whyoleg.cryptography.algorithms.SHA256

@OptIn(DelicateCryptographyApi::class)
internal actual fun rsaEncryptBytes(data: ByteArray, publicKeyPem: String): ByteArray =
    CryptographyProvider.Default
        .get(RSA.PKCS1)
        .publicKeyDecoder(SHA256)
        .decodeFromByteArrayBlocking(RSA.PublicKey.Format.PEM.Generic, publicKeyPem.encodeToByteArray())
        .encryptor()
        .encryptBlocking(data)

private fun aesCipher(key: ByteArray) =
    CryptographyProvider.Default
        .get(AES.CBC)
        .keyDecoder()
        .decodeFromByteArrayBlocking(AES.Key.Format.RAW, key)
        .cipher(padding = false)

@OptIn(DelicateCryptographyApi::class)
internal actual fun aesEncryptBytes(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray =
    aesCipher(key).encryptWithIvBlocking(iv, data)

@OptIn(DelicateCryptographyApi::class)
internal actual fun aesDecryptBytes(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray =
    aesCipher(key).decryptWithIvBlocking(iv, data)

// cryptography-kotlin 0.6.0 exposes no general CSPRNG, so we derive random bytes from freshly
// generated AES-256 keys (the key generator is backed by the platform secure RNG).
// java.security.SecureRandom is available on JVM, but a single nativeJvmMain source set
// avoids duplicating aesEncryptBytes/aesDecryptBytes/rsaEncryptBytes across jvmMain/nativeMain.
// The per-connection overhead is negligible: secureRandomBytes is only called once during session
// key setup.
internal actual fun secureRandomBytes(size: Int): ByteArray {
    val generator = CryptographyProvider.Default.get(AES.CBC).keyGenerator(AES.Key.Size.B256)
    val out = ByteArray(size)
    var offset = 0
    while (offset < size) {
        val block = generator.generateKeyBlocking().encodeToByteArrayBlocking(AES.Key.Format.RAW)
        val take = minOf(block.size, size - offset)
        block.copyInto(out, offset, 0, take)
        offset += take
    }
    return out
}
