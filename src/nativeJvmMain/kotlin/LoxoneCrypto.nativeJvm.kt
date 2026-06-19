package cz.smarteon.loxkt

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
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
