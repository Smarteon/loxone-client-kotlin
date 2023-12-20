package cz.smarteon.loxone.message

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
) : LoxoneMsgVal {
    companion object {
        @JvmStatic
        fun valueForDecoding(value: String): String = value.replace("'", "\"")
    }
}
