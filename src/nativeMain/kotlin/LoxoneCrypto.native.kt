package cz.smarteon.loxkt

import cz.smarteon.loxkt.LoxoneException

/**
 * Native (Linux) implementation of RSA encryption.
 * TODO: Implement using OpenSSL or similar native library
 */
internal actual fun rsaEncryptBytes(data: ByteArray, publicKeyPem: String): ByteArray {
    throw LoxoneException("RSA encryption is not yet implemented for Native platform")
}
