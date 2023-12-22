package cz.smarteon.loxone

import kotlinx.serialization.json.Json

object Codec {

    val loxJson = Json {
        ignoreUnknownKeys = true
    }

    fun hexToBytes(hex: String): ByteArray {
        check(hex.length % 2 == 0) { "Must have an even length" }

        return hex.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

}
