package cz.smarteon.loxkt

import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher

/**
 * JVM implementation of RSA encryption.
 * Uses Java's built-in crypto with RSA/ECB/PKCS1Padding.
 */
internal actual fun rsaEncryptBytes(data: ByteArray, publicKeyPem: String): ByteArray {
    // Parse PEM format - remove header, footer, and whitespace
    val publicKeyContent = publicKeyPem
        .replace("-----BEGIN PUBLIC KEY-----", "")
        .replace("-----END PUBLIC KEY-----", "")
        .replace("\\s".toRegex(), "")
    
    // Decode Base64 to get DER format
    val keyBytes = Base64.getDecoder().decode(publicKeyContent)
    
    // Create public key from X.509 encoded format
    val keySpec = X509EncodedKeySpec(keyBytes)
    val keyFactory = KeyFactory.getInstance("RSA")
    val publicKey = keyFactory.generatePublic(keySpec)
    
    // Encrypt using RSA with ECB mode and PKCS1 padding
    val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
    cipher.init(Cipher.ENCRYPT_MODE, publicKey)
    
    return cipher.doFinal(data)
}
