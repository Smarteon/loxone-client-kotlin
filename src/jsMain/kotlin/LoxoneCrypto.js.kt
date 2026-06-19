package cz.smarteon.loxkt

// RSA PKCS1v1.5 encryption is not available in the Web Crypto API (intentionally excluded from the spec).
internal actual fun rsaEncryptBytes(data: ByteArray, publicKeyPem: String): ByteArray =
    throw LoxoneException("RSA encryption is not supported on the JavaScript platform")
