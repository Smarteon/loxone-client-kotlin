package cz.smarteon.loxkt.message

import cz.smarteon.loxkt.Codec.loxJson
import kotlinx.serialization.Serializable

/**
 * What the Miniserver tells about the login code of a user with MFA (firmware 17.3): in a refused `getjwt`
 * ([cz.smarteon.loxkt.LoxoneLoginCodeException]) and in the answer to `otpresend`. It sends only the fields that
 * apply.
 *
 * @property methods the methods the user can log in with, e.g. [EMAIL] and [TOTP]; only when asked with a method
 * @property default the method the Loxone apps offer first; only when asked with a method
 * @property unavailable methods the user can't use now, with the reason, e.g. `notOffered`, `setup`, `blocked` or
 * `noTime` (TOTP while the Miniserver's clock isn't synchronized)
 * @property mail what became of the e-mailed code: `sent`, `alreadySent` (the pending code stays valid), `wait`
 * (asked again within [resendIn] or during a lock) or `noAddress`
 * @property to the masked address the code went to, e.g. `t***@example.com`
 * @property length digits of the code
 * @property expiresIn seconds the e-mailed code stays valid
 * @property resendIn seconds until `otpresend` sends another code
 * @property attemptsLeft wrong codes left: 10 per login for TOTP, 5 per e-mailed code
 * @property reason why a code was refused besides being wrong, e.g. `codeUsed`
 * @property retryAfter seconds the login stays locked
 * @property totpBlockedUntil unix seconds (not the Loxone epoch) until which TOTP is blocked
 */
@Serializable
data class LoginCode(
    val methods: List<String> = emptyList(),
    val default: String? = null,
    val unavailable: Map<String, String> = emptyMap(),
    val mail: String? = null,
    val to: String? = null,
    val length: Int? = null,
    val expiresIn: Int? = null,
    val resendIn: Int? = null,
    val attemptsLeft: Int? = null,
    val reason: String? = null,
    val retryAfter: Int? = null,
    val totpBlockedUntil: Long? = null,
) : LoxoneMsgVal {

    companion object {
        /** The code sent by e-mail. */
        const val EMAIL = "email"

        /** The code from an authenticator app. */
        const val TOTP = "totp"

        private val CODES = setOf("460", "462", "463", "466", "468", "470")

        /** The login code [msg] tells about, or null when it isn't a refused login with a readable `otp`. */
        internal fun of(msg: LoxoneMsg): LoginCode? =
            if (msg.code !in CODES) {
                null
            } else {
                runCatching { loxJson.decodeFromString<OtpAnswer>(unquoteJson(msg.value)).otp }.getOrNull()
            }
    }
}

@Serializable
private class OtpAnswer(val otp: LoginCode)
