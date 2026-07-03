package cz.smarteon.loxkt

import kotlin.js.JsModule

@JsModule("node-forge")
internal external val forge: dynamic

@JsModule("crypto-js")
internal external val cryptoJs: dynamic

internal actual fun rsaEncryptBytes(data: ByteArray, publicKeyPem: String): ByteArray {
    val jsData = data.toTypedArray()
    // fromCharCode.apply is bounded: RSA-2048/PKCS1v1.5 caps plaintext at 245 bytes.
    val binaryStr: String = js("String.fromCharCode.apply(null, jsData)").unsafeCast<String>()
    val publicKey = forge.pki.publicKeyFromPem(publicKeyPem)
    // node-forge defaults to the RSAES-PKCS1-V1_5 scheme Loxone expects.
    val encrypted: String = publicKey.encrypt(binaryStr).unsafeCast<String>()
    return ByteArray(encrypted.length) { encrypted[it].code.toByte() }
}

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
