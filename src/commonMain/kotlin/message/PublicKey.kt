package cz.smarteon.loxkt.message

import cz.smarteon.loxkt.CommandSupplier
import cz.smarteon.loxkt.LoxoneMsgCommand
import kotlinx.serialization.Serializable

/**
 * Represents the Miniserver's RSA public key used for command encryption.
 * The key is in X.509 encoded format (PEM format).
 *
 * @property publicKey The RSA public key in PEM format (X.509 encoded).
 */
@Serializable
data class PublicKey(
    val publicKey: String
) : LoxoneMsgVal {
    companion object : CommandSupplier<LoxoneMsg, LoxoneMsgCommand<PublicKey>> {
        override val command = sysCommand<PublicKey>("getPublicKey", authenticated = false)
    }
}
