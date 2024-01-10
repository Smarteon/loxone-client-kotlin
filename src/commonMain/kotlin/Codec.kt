package cz.smarteon.loxone

import kotlinx.serialization.json.Json

object Codec {

    private const val SEPARATOR = ':'

    val loxJson = Json {
        ignoreUnknownKeys = true
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun hexToBytes(hex: String): ByteArray = hex.hexToByteArray(HexFormat.UpperCase)

    @OptIn(ExperimentalStdlibApi::class)
    fun bytesToHex(bytes: ByteArray, format: HexFormat = HexFormat.Default): String
        = bytes.toHexString(format)

    internal fun concat(first: String, second: String): String {
        return first + SEPARATOR + second
    }

    internal fun concatToBytes(first: String, second: String): ByteArray {
        return concat(first, second).encodeToByteArray()
    }

}
