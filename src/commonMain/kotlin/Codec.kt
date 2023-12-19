package cz.smarteon.loxone

import kotlinx.serialization.json.Json

object Codec {

    val JSON = Json {
        ignoreUnknownKeys = true
    }
}
