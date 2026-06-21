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

private val cryptoJs: dynamic get() = js("require('crypto-js')")

private fun aesConfig(iv: ByteArray): dynamic {
    val cfg = js("({})")
    cfg.iv = cryptoJs.enc.Hex.parse(iv.toHex())
    cfg.mode = cryptoJs.mode.CBC
    cfg.padding = cryptoJs.pad.NoPadding
    return cfg
}

@OptIn(ExperimentalStdlibApi::class)
internal actual fun aesEncryptBytes(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
    val keyWa = cryptoJs.enc.Hex.parse(key.toHex())
    val dataWa = cryptoJs.enc.Hex.parse(data.toHex())
    val encrypted: dynamic = cryptoJs.AES.encrypt(dataWa, keyWa, aesConfig(iv))
    return (encrypted.ciphertext.toString(cryptoJs.enc.Hex) as String).hexToByteArray()
}

@OptIn(ExperimentalStdlibApi::class)
internal actual fun aesDecryptBytes(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
    val keyWa = cryptoJs.enc.Hex.parse(key.toHex())
    val cipherParams = cryptoJs.lib.CipherParams.create(js("({})"))
    cipherParams.ciphertext = cryptoJs.enc.Hex.parse(data.toHex())
    val decrypted: dynamic = cryptoJs.AES.decrypt(cipherParams, keyWa, aesConfig(iv))
    return (decrypted.toString(cryptoJs.enc.Hex) as String).hexToByteArray()
}

@OptIn(ExperimentalStdlibApi::class)
internal actual fun secureRandomBytes(size: Int): ByteArray =
    (cryptoJs.lib.WordArray.random(size).toString(cryptoJs.enc.Hex) as String).hexToByteArray()

@OptIn(ExperimentalStdlibApi::class)
private fun ByteArray.toHex(): String = toHexString()
