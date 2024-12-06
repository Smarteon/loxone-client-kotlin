package cz.smarteon.loxkt.message

import cz.smarteon.loxkt.LoxoneTime
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val token: String? = null,
    @Serializable(HexSerializer::class) val key: ByteArray? = null,
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

    /**
     * Merges the given token to this one and returns the merged token. The [Token.token] and [Token.key] are taken
     * from given token only if they are not null, otherwise the values from this token are used. Other properties are
     * always taken from given token.
     *
     * @param other token to merge
     * @return new merged token
     */
    fun merge(other: Token): Token =
        if (this == other) {
            this
        } else {
            Token(
                token = other.token ?: token,
                key = other.key ?: key,
                validUntil = other.validUntil,
                rights = other.rights,
                unsecurePassword = other.unsecurePassword
            )
        }

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
}
