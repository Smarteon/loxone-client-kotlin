package cz.smarteon.loxone.message

import cz.smarteon.loxone.Codec.JSON
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmStatic

@Serializable
data class ApiInfo(
    @SerialName("snr") val mac: String,
    val version: String,
    val hasEventSlots: Boolean?,
    val isInTrust: Boolean?,
    @SerialName("local") val isLocal: Boolean?
) {
    companion object {
        @JvmStatic
        fun fromMessageValue(value: String): ApiInfo = JSON.decodeFromString(value.replace("'", "\""))
    }
}
