package cz.smarteon.loxkt.message

import cz.smarteon.loxkt.CommandSupplier
import cz.smarteon.loxkt.LoxoneMsgCommand
import kotlinx.serialization.Serializable

/** Miniserver RSA public key (X.509/PEM format) used for command encryption. */
@Serializable
data class PublicKey(
    val publicKey: String
) : LoxoneMsgVal {
    companion object : CommandSupplier<LoxoneMsg, LoxoneMsgCommand<PublicKey>> {
        override val command = sysCommand<PublicKey>("getPublicKey", authenticated = false)
    }
}
