package cz.smarteon.loxkt.message

import kotlin.jvm.JvmField

/**
 * Permissions a [Token] is asked for: a bit set as `getjwt` takes it. Combine them with [plus]; the
 * Miniserver issues one token with all of them, or none when the user may not have one of them (412).
 *
 * For example `APP + EXPERT_MODE + SYS_WS` (324) gives a token that lives weeks and also opens expert mode,
 * restarts the program and arms the Debug Monitor (verified on firmware 17.2).
 *
 * @property[id] numeric representation as used in Loxone API, comparable with [Token.rights]
 */
@Suppress("MagicNumber")
data class TokenPermission(val id: Int) {

    /** This and [other] permission in one token. */
    operator fun plus(other: TokenPermission) = TokenPermission(id or other.id)

    /** Whether all bits of [other] are in this one, e.g. `TokenPermission.SYS_WS in TokenPermission(rights)`. */
    operator fun contains(other: TokenPermission) = id and other.id == other.id

    companion object {
        /** ADMIN permission. */
        @JvmField val ADMIN = TokenPermission(1)

        /** WEB permission - short token validity (lasts for hours). */
        @JvmField val WEB = TokenPermission(2)

        /** APP permission - long token validity (lasts for weeks). */
        @JvmField val APP = TokenPermission(4)

        /** CONFIG permission - used to connect like Loxone Config application. */
        @JvmField val CONFIG = TokenPermission(12)

        /** Expert mode (`jdev/sps/io/<uuid>/edit/…`), admins only. Alone it caps the token at an hour. */
        @JvmField val EXPERT_MODE = TokenPermission(64)

        /** Expert mode light (`jdev/sps/io/<uuid>/editlight/…`), a user group right; APP and WEB tokens have it. */
        @JvmField val EXPERT_MODE_LIGHT = TokenPermission(1024)

        /** Restarting the program (`dev/sps/restart`) and the Debug Monitor (`dev/sps/log`). */
        @JvmField val SYS_WS = TokenPermission(256)

        /** Managing users and groups. */
        @JvmField val USER_MANAGEMENT = TokenPermission(2048)

        /** Triggering a firmware update. */
        @JvmField val TRIGGER_UPDATE = TokenPermission(32768)
    }
}
