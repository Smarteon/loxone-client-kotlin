package cz.smarteon.loxkt.message

import cz.smarteon.loxkt.Codec.loxJson
import kotlinx.serialization.Serializable

/**
 * A Miniserver user as `jdev/sps/getuser`, `getuserlist2` and `addoredituser` describe it. Only the fields
 * this client uses are mapped. Times are seconds since the Loxone epoch, see [cz.smarteon.loxkt.LoxoneTime].
 *
 * @property userState one of the `STATE_` constants
 * @property expirationAction what the Miniserver does once [validUntil] passes, one of the `EXPIRATION_` constants
 * @property usergroups the user's groups; `getuserlist2` leaves them out and they carry only uuid and name
 */
@Serializable
data class MiniserverUser(
    val uuid: String,
    val name: String,
    val userState: Int = STATE_ENABLED,
    val validUntil: Long? = null,
    val validFrom: Long? = null,
    val expirationAction: Int? = null,
    val isAdmin: Boolean = false,
    val usergroups: List<UserGroup> = emptyList(),
) : LoxoneMsgVal {

    /** Whether the user stops being valid at [validUntil]. */
    val isTimeLimited: Boolean
        get() = userState == STATE_VALID_UNTIL || userState == STATE_VALID_FROM_UNTIL

    companion object {
        const val STATE_ENABLED = 0
        const val STATE_DISABLED = 1
        const val STATE_VALID_UNTIL = 2
        const val STATE_VALID_FROM = 3
        const val STATE_VALID_FROM_UNTIL = 4

        const val EXPIRATION_DEACTIVATE = 0
        const val EXPIRATION_DELETE = 1
    }
}

/**
 * A user group as `jdev/sps/getgrouplist` describes it. Inside a [MiniserverUser] only [uuid] and [name] are
 * filled, so [isAdmin] can't be told from there.
 *
 * @property type one of the `TYPE_` constants
 * @property userRights bit set of the `RIGHT_` constants
 */
@Serializable
data class UserGroup(
    val uuid: String,
    val name: String,
    val type: Int = TYPE_NORMAL,
    val userRights: Long = 0,
) {

    /** Whether membership makes a user an administrator: an admin or Full Access group, or the Loxone Config right. */
    val isAdmin: Boolean
        get() = type == TYPE_ADMIN || type == TYPE_FULL_ACCESS || userRights and RIGHT_LOXONE_CONFIG != 0L

    companion object {
        const val TYPE_NORMAL = 0
        const val TYPE_ADMIN = 1
        const val TYPE_ALL = 2
        const val TYPE_NONE = 3
        const val TYPE_FULL_ACCESS = 4

        const val RIGHT_LOXONE_CONFIG = 0x4L
        const val RIGHT_FTP = 0x8L
        const val RIGHT_TELNET = 0x10L
        const val RIGHT_USER_MANAGEMENT = 0x100L
    }
}

/** All users from `jdev/sps/getuserlist2`; the Miniserver answers with a bare array, held in [items]. */
@Serializable
data class MiniserverUsers(val items: List<MiniserverUser>) : LoxoneMsgVal

/** All groups from `jdev/sps/getgrouplist`; the Miniserver answers with a bare array, held in [items]. */
@Serializable
data class UserGroups(val items: List<UserGroup>) : LoxoneMsgVal

/**
 * The Miniserver may send these values as JSON or as a string holding the JSON (the Loxone app parses
 * `getgrouplist`'s value from a string). Registered by [cz.smarteon.loxkt.LoxoneCommands.Users].
 */
internal fun registerUserDecoders() {
    loxoneMsgValDecoders += MiniserverUser::class to ::unquoteJson
    loxoneMsgValDecoders += MiniserverUsers::class to { """{"items":${unquoteJson(it)}}""" }
    loxoneMsgValDecoders += UserGroups::class to { """{"items":${unquoteJson(it)}}""" }
}

private fun unquoteJson(value: String): String =
    if (value.startsWith('"')) loxJson.decodeFromString<String>(value) else value
