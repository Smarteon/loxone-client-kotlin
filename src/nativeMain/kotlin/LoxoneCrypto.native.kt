package cz.smarteon.loxkt

// TODO: implement via OpenSSL C interop (RSA_public_encrypt with RSA_PKCS1_PADDING)
internal actual fun rsaEncryptBytes(data: ByteArray, publicKeyPem: String): ByteArray =
    throw LoxoneException("RSA encryption is not yet implemented for Native platform")
