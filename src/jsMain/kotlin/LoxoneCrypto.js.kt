package cz.smarteon.loxkt

import cz.smarteon.loxkt.LoxoneException

/**
 * JavaScript implementation of RSA encryption.
 * TODO: Implement using Web Crypto API
 */
internal actual fun rsaEncryptBytes(data: ByteArray, publicKeyPem: String): ByteArray {
    throw LoxoneException("RSA encryption is not yet implemented for JavaScript platform")
}
