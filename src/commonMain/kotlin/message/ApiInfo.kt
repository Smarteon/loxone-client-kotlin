package cz.smarteon.loxkt.message

import cz.smarteon.loxkt.CommandSupplier
import cz.smarteon.loxkt.LoxoneMsgCommand
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
