package cz.smarteon.loxone.message

import cz.smarteon.loxone.CommandSupplier
import cz.smarteon.loxone.LoxoneMsgCommand
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiInfo(
    @SerialName("snr") val mac: String,
    val version: String,
    val hasEventSlots: Boolean?,
    val isInTrust: Boolean?,
    @SerialName("local") val isLocal: Boolean?
) : LoxoneMsgVal {
    companion object : CommandSupplier<LoxoneMsg, LoxoneMsgCommand<ApiInfo>> {

        init {
            loxoneMsgValDecoders += ApiInfo::class to {
                it.trim('"').replace("'", "\"")
            }
        }

        override val command = cfgCommand<ApiInfo>("api")
    }
}
