package cz.smarteon.loxkt

import cz.smarteon.loxkt.message.LoginCode

/**
 * @property code the Miniserver's answer code when it refused a command, e.g. `412` for rights the user may not
 * have.
 */
open class LoxoneException(message: String, cause: Throwable? = null, val code: String? = null) :
    Exception(message, cause)

/**
 * The Miniserver issues no token without the second factor of a user with MFA (firmware 17.3): it answered `getjwt`
 * with [code] and told about the code in [loginCode].
 *
 * - `460`: a code is needed. Asked with a method, [LoginCode.methods] and [LoginCode.default] say which the user
 *   has; asked without one or with [LoginCode.EMAIL], the code went by e-mail ([LoginCode.mail], [LoginCode.to]).
 * - `462`: the user has no e-mail for the code (17.3 betas up to 17.3.11.12, later 468).
 * - `463`: wrong code, [LoginCode.attemptsLeft] tries left; [LoginCode.reason] `codeUsed` for a TOTP code of a
 *   window already used, which isn't counted.
 * - `466`: locked for [LoginCode.retryAfter] seconds after 10 wrong codes in a row, even for the right code.
 * - `468`: the user has no method this request can use: TOTP only and asked without a method, or none at all.
 * - `470`: TOTP blocked until [LoginCode.totpBlockedUntil] after 30 wrong codes.
 *
 * Each wrong code counts towards the lock, so a code is sent once per code the user gave.
 */
class LoxoneLoginCodeException(code: String, val loginCode: LoginCode) :
    LoxoneException("Miniserver answered $code to a login that needs a second factor", code = code)
