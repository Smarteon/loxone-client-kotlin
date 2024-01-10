package cz.smarteon.loxone.message

import cz.smarteon.loxone.CommandSupplier
import cz.smarteon.loxone.LoxoneMsgCommand
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiKeyInfo(
    @SerialName("snr") val mac: String,
    val version: String,
    val key: String,
    val address: String,
    val isInTrust: Boolean,
    @SerialName("local") val isLocal: Boolean,
    val certTLD: String?,
    val httpsStatus: Int?
) : LoxoneMsgVal {

    companion object : CommandSupplier<LoxoneMsg, LoxoneMsgCommand<ApiKeyInfo>> {

        init {
            loxoneMsgValDecoders += ApiKeyInfo::class to {
                it.trim('"').replace("'", "\"")
            }
        }

        override val command = cfgCommand<ApiKeyInfo>("apiKey")
    }
}
