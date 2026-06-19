package cz.smarteon.loxkt

import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher

internal actual fun rsaEncryptBytes(data: ByteArray, publicKeyPem: String): ByteArray {
    val pemBody = publicKeyPem
        .replace("-----BEGIN PUBLIC KEY-----", "")
        .replace("-----END PUBLIC KEY-----", "")
        .replace("\\s".toRegex(), "")

    val keyBytes = Base64.getDecoder().decode(pemBody)
    val publicKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(keyBytes))

    return Cipher.getInstance("RSA/ECB/PKCS1Padding").run {
        init(Cipher.ENCRYPT_MODE, publicKey)
        doFinal(data)
    }
}
