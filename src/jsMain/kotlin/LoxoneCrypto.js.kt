package cz.smarteon.loxkt

import kotlin.js.JsModule

// ES-module imports for the npm crypto packages. The build emits an ES module (useEsModules()),
// so `require(...)` is undefined at runtime (both in browsers and Node ESM). Declaring the modules
// as `@JsModule` externals makes the compiler emit `import` statements instead. Both packages are
// CJS/UMD whose default export carries the whole API surface under ESM interop, so both are kept
// `dynamic`. node-forge and crypto-js resolve cleanly under Node ESM and in browser bundlers.
@JsModule("node-forge")
internal external val forge: dynamic

@JsModule("crypto-js")
internal external val cryptoJs: dynamic

internal actual fun rsaEncryptBytes(data: ByteArray, publicKeyPem: String): ByteArray {
    val jsData = data.toTypedArray()
    // node-forge operates on binary strings (one char per byte). String.fromCharCode.apply is safe
    // here: RSA-2048/PKCS1v1.5 limits plaintext to 245 bytes.
    val binaryStr: String = js("String.fromCharCode.apply(null, jsData)").unsafeCast<String>()
    val publicKey = forge.pki.publicKeyFromPem(publicKeyPem)
    // encrypt() defaults to the RSAES-PKCS1-V1_5 scheme Loxone expects; returns a binary string.
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
