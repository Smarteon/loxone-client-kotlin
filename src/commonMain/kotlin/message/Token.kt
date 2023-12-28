package cz.smarteon.loxone.message

import cz.smarteon.loxone.LoxoneTime
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmStatic


/**
 * Represents Loxone authentication token.
 *
 * @roperty[token] The actual token value. May be null in case of response to refresh token or `authwithtoken`
 * @property[key] The token key value. May be null in case of response to refresh token or authwithtoken.
 * @property[validUntil] Seconds since loxone epoch (1.1.2009) to which the token is valid.
 * @property[rights]
 * @property[unsecurePassword]
 */
@Serializable
data class Token(
    val token: String?,
    @Serializable(HexSerializer::class) val key: ByteArray?,
    val validUntil: Long,
    @SerialName("tokenRights") val rights: Int,
    @SerialName("unsecurePass") val unsecurePassword: Boolean
) : LoxoneMsgVal {

    val filled = token != null && key != null

    /**
     * Seconds remaining to token expiry.
     * @return seconds to expire
     */
    fun secondsToExpireFromNow() = LoxoneTime.getUnixEpochSeconds(validUntil) - Clock.System.now().epochSeconds

    fun <T> withTokenAndKey(block: (String, ByteArray) -> T): T =
        if (filled)
            block(token!!, key!!)
        else
            throw IllegalStateException("Can't invoke block(token, key) on nonfilled token")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Token

        if (token != other.token) return false
        if (!key.contentEquals(other.key)) return false
        if (validUntil != other.validUntil) return false
        if (rights != other.rights) return false
        if (unsecurePassword != other.unsecurePassword) return false

        return true
    }

    override fun hashCode(): Int {
        var result = token?.hashCode() ?: 0
        result = 31 * result + (key?.contentHashCode() ?: 0)
        result = 31 * result + validUntil.hashCode()
        result = 31 * result + rights
        result = 31 * result + unsecurePassword.hashCode()
        result = 31 * result + filled.hashCode()
        return result
    }


    companion object {

        @JvmStatic
        fun commandGetToken(
            tokenHash: String,
            user: String,
            permission: TokenPermission,
            clientId: String,
            clientInfo: String
        ) =
            sysCommand<Token>(
                "getjwt",
                tokenHash,
                user,
                permission.id.toString(),
                clientId,
                clientInfo,
                authenticated = false
            )
    }
}
