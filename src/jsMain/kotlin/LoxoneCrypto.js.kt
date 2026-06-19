package cz.smarteon.loxkt

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
internal actual fun rsaEncryptBytes(data: ByteArray, publicKeyPem: String): ByteArray {
    val jsData = data.toTypedArray()
    // String.fromCharCode.apply is safe here: RSA-2048/PKCS1v1.5 limits plaintext to 245 bytes
    val binaryStr: String = js("String.fromCharCode.apply(null, jsData)").unsafeCast<String>()
    val enc: dynamic = js("new (require('jsencrypt').JSEncrypt)()")
    enc.setPublicKey(publicKeyPem)
    // encrypt() returns string | false — false means invalid key or data too large
    val base64: dynamic = enc.encrypt(binaryStr)
    if (base64 == false) throw LoxoneException("RSA encryption failed — check key format and data size")
    return Base64.decode(base64 as String)
}
