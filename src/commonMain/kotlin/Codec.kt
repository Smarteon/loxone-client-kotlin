package cz.smarteon.loxone

import kotlinx.serialization.json.Json

object Codec {

    val loxJson = Json {
        ignoreUnknownKeys = true
    }
}
