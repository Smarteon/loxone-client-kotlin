package cz.smarteon.loxkt

import cz.smarteon.loxkt.LoxoneCommands.App.version
import cz.smarteon.loxkt.app.LoxoneAppCommand
import cz.smarteon.loxkt.app.LoxoneAppVersionCommand
import cz.smarteon.loxkt.app.StatisticUnit
import cz.smarteon.loxkt.message.EmptyLoxoneMsgVal
import cz.smarteon.loxkt.message.Hashing
import cz.smarteon.loxkt.message.LoxoneMsg
import cz.smarteon.loxkt.message.MiniserverUser
import cz.smarteon.loxkt.message.MiniserverUsers
import cz.smarteon.loxkt.message.SimpleLoxoneMsgCommand
import cz.smarteon.loxkt.message.Token
import cz.smarteon.loxkt.message.TokenPermission
import cz.smarteon.loxkt.message.UserGroups
import cz.smarteon.loxkt.message.registerUserDecoders
import cz.smarteon.loxkt.message.spsCommand
import cz.smarteon.loxkt.message.sysCommand
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.time.Instant

/**
 * Central registry of Loxone commands.
 *
 * Please note that primarily, the functions for commands construction are accessible on the related message classes.
 * @see[Command]
 * @see[LoxoneMsgCommand]
 * @see[CommandSupplier]
 */
object LoxoneCommands {

    /**
     * Keep alive command used solely in [cz.smarteon.loxkt.WebsocketLoxoneClient] to ensure connection alive
     * functionality.
     */
    val KEEP_ALIVE = object : NoResponseCommand("keepalive") {}

    /**
     * Commands related to tokens management.
     */
    object Tokens {

        /**
         * Command to get new token.
         * @param credentialsHash The credentials hash obtained by [LoxoneCrypto.loxoneHashing]
         * from [LoxoneCredentials].
         * @param user The user to get the token for.
         * @param permission The permission of the token.
         * @param clientId Unique identifier of this client.
         * @param clientInfo Information about this client.
         */
        @JvmStatic
        fun get(
            credentialsHash: String,
            user: String,
            permission: TokenPermission,
            clientId: String = LoxoneClientSettings.DEFAULT_CLIENT_ID,
            clientInfo: String = LoxoneClientSettings.DEFAULT_CLIENT_INFO
        ) = sysCommand<Token>(
            "getjwt",
            credentialsHash,
            user,
            permission.id.toString(),
            clientId,
            clientInfo,
            authenticated = false,
            // getjwt must be sent over the encrypted channel; a Miniserver declines plain getjwt with 400
            encrypted = true
        )

        /**
         * Command to get a new token signed with an existing one instead of the password (`?tokenHash=true`).
         * The Miniserver issues only rights the existing token has and refuses more with 412.
         * @param tokenHash HMAC-SHA256 of the existing token, keyed with a fresh `jdev/sys/getkey`.
         * @param user The user of the existing token.
         * @param permission The permission of the new token.
         * @param clientId Unique identifier of this client.
         * @param clientInfo Information about this client.
         */
        @JvmStatic
        fun getWithToken(
            tokenHash: String,
            user: String,
            permission: TokenPermission,
            clientId: String = LoxoneClientSettings.DEFAULT_CLIENT_ID,
            clientInfo: String = LoxoneClientSettings.DEFAULT_CLIENT_INFO
        ): LoxoneMsgCommand<Token> = object : LoxoneMsgCommand<Token> by get(
            tokenHash,
            user,
            permission,
            clientId,
            clientInfo
        ) {
            override val query = mapOf("tokenHash" to "true")
        }

        /**
         * Command to authenticate with token.
         * @param tokenHash The token hash to authenticate with.
         * @param user The user to authenticate as.
         */
        @JvmStatic
        fun auth(tokenHash: String, user: String) = SimpleLoxoneMsgCommand(
            listOf("authwithtoken", tokenHash, user),
            Token::class,
            authenticated = false,
            encrypted = true
        )

        /**
         * Command to refresh token. Uses the modern `refreshjwt` endpoint (protocol ≥ 10.2).
         * The [tokenHash] must be HMAC of the current token string using a fresh `getkey2` result.
         * Authenticated, as the Miniserver answers it over HTTP only with `autht` (401 without).
         */
        @JvmStatic
        fun refresh(tokenHash: String, user: String) = sysCommand<Token>("refreshjwt", tokenHash, user)

        /**
         * Command to kill token. Over a WebSocket the Miniserver answers 401 (the session's token is dead
         * now); over HTTP, signed with the killed token's `autht`, it answers 200.
         * @param tokenHash The token hash to kill.
         * @param user The user to kill the token for.
         */
        @JvmStatic
        fun kill(tokenHash: String, user: String) = sysCommand<EmptyLoxoneMsgVal>(
            "killtoken",
            tokenHash,
            user,
            expectedCode = LoxoneMsg.CODE_AUTH_FAIL
        )
    }

    /**
     * Commands of the Miniserver's user management. The logged-in user needs the user management right;
     * adding a user to an admin group needs an administrator.
     */
    object Users {

        init {
            registerUserDecoders()
        }

        /** All users, without their groups. */
        @JvmStatic
        fun list() = spsCommand<MiniserverUsers>("getuserlist2")

        /** All user groups with their type and rights. */
        @JvmStatic
        fun groups() = spsCommand<UserGroups>("getgrouplist")

        /** The full configuration of the user with [uuid], groups included. */
        @JvmStatic
        fun get(uuid: String) = spsCommand<MiniserverUser>("getuser", uuid)

        /**
         * Creates a user [name] in the groups [groupUuids], answering with the new user. The user has no password
         * until [setPassword] and may not change it from the apps.
         *
         * With [validUntil] the user is valid until then; afterwards the Miniserver deletes the user when
         * [deleteOnExpiry], deactivates it otherwise (deletion needs firmware 14.2.5.16+).
         *
         * The name is 1–64 letters, digits and `._-`, not only dots: the HTTP client percent-encodes path segments and
         * the WebSocket client does not, so any other character would reach the Miniserver differently per client,
         * here and in [Hashing.commandForUser]. `@` stays out because the Miniserver reads `name@serial` as a Trust
         * user.
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            name: String,
            groupUuids: List<String>,
            validUntil: Instant? = null,
            deleteOnExpiry: Boolean = false
        ): LoxoneMsgCommand<MiniserverUser> {
            require(USER_NAME.matches(name)) { "User name \"$name\" may hold only 1–64 letters, digits and ._-" }
            return spsCommand<MiniserverUser>("addoredituser", createJson(name, groupUuids, validUntil, deleteOnExpiry))
        }

        private fun createJson(name: String, groupUuids: List<String>, validUntil: Instant?, deleteOnExpiry: Boolean) =
            buildJsonObject {
                put("name", name)
                put("changePassword", false)
                if (validUntil == null) {
                    put("userState", MiniserverUser.STATE_ENABLED)
                } else {
                    put("userState", MiniserverUser.STATE_VALID_UNTIL)
                    put("validUntil", validUntil.epochSeconds - LoxoneTime.LOXONE_EPOCH_BEGIN)
                    put(
                        "expirationAction",
                        if (deleteOnExpiry) MiniserverUser.EXPIRATION_DELETE else MiniserverUser.EXPIRATION_DEACTIVATE
                    )
                }
                // Group uuids as plain strings, as the Loxone app sends them (`_getChangedUser`).
                putJsonArray("usergroups") { groupUuids.forEach { add(it) } }
            }.toString()

        private val USER_NAME = Regex("""(?!\.+$)[A-Za-z0-9._-]{1,64}""")

        /**
         * Sets the password of the user with [uuid]. Only its hash is sent, salted with [hashing], which
         * [Hashing.commandForUser] returns for that user. A WebSocket client sends it encrypted; the HTTP client
         * can't, so over plain `http://` the hash, which can stand in for the password, is as readable as the
         * Basic credentials of every request, and Ktor's request logging at DEBUG writes it in the URL.
         */
        @JvmStatic
        fun setPassword(uuid: String, password: String, hashing: Hashing) = SimpleLoxoneMsgCommand(
            listOf("jdev", "sps", "updateuserpwdh", uuid, LoxoneCrypto.loxoneDigest(password, hashing)),
            EmptyLoxoneMsgVal::class,
            encrypted = true
        )

        /** Deletes the user with [uuid] and closes its connections; the last administrator can't be deleted. */
        @JvmStatic
        fun delete(uuid: String) = spsCommand<EmptyLoxoneMsgVal>("deleteuser", uuid)
    }

    /**
     * Commands related to app management.
     */
    object App {

        /**
         * Command to download the complete app (LoxAPP3.json).
         * This contains all controls, rooms, categories, and Miniserver configuration.
         *
         * The app can be quite large and should be cached locally.
         * Use [version] to check if the cached version is still valid.
         *
         * @return Command to download app
         */
        @JvmStatic
        fun get() = LoxoneAppCommand

        /**
         * Command to check the version/last modified date of the app.
         * Use this to check if your cached app is still up to date.
         *
         * @return Command to get app version
         */
        @JvmStatic
        fun version() = LoxoneAppVersionCommand

        /**
         * Command to enable binary status updates.
         * After this command is sent, the Miniserver will send binary event tables
         * (VALUE, TEXT, DAYTIMER, WEATHER) through the WebSocket connection.
         *
         * This command must be sent after authentication to receive state updates.
         *
         * @return Command to enable binary status updates
         */
        @JvmStatic
        fun enableBinStatusUpdate() = SimpleLoxoneMsgCommand(
            listOf("jdev", "sps", "enablebinstatusupdate"),
            EmptyLoxoneMsgVal::class,
            authenticated = true
        )
    }

    /**
     * Commands related to statistics.
     */
    object Statistics {

        /**
         * Build the command path for V2 raw statistic data.
         * Returns binary data with recorded values during the specified timespan.
         * @param controlUuid UUID of the control
         * @param timeRange Time range as pair of unix UTC timestamps (from to until, inclusive)
         * @param groupId Which statistic group is requested
         * @param unit Data point unit (all, hour, day, month, year)
         * @param outputName Optional output name filter
         */
        @JvmStatic
        fun getStatisticRaw(
            controlUuid: String,
            timeRange: Pair<Long, Long>,
            groupId: Int,
            unit: StatisticUnit = StatisticUnit.ALL,
            outputName: String? = null
        ): String = statisticPath(controlUuid, "raw", timeRange, unit.value, groupId, outputName)

        /**
         * Build the command path for V2 diff (preprocessed) statistic data.
         * Returns summed differences between recorded values per [unit].
         * @param controlUuid UUID of the control
         * @param timeRange Time range as pair of unix UTC timestamps (from to until, inclusive)
         * @param groupId Which statistic group is requested
         * @param unit Data point grouping resolution
         * @param outputName Optional output name filter
         */
        @JvmStatic
        fun getStatisticDiff(
            controlUuid: String,
            timeRange: Pair<Long, Long>,
            groupId: Int,
            unit: StatisticUnit = StatisticUnit.DAY,
            outputName: String? = null
        ): String = statisticPath(controlUuid, "diff", timeRange, unit.value, groupId, outputName)

        @Suppress("LongParameterList")
        private fun statisticPath(
            controlUuid: String,
            mode: String,
            timeRange: Pair<Long, Long>,
            unit: String,
            groupId: Int,
            outputName: String?
        ): String = buildString {
            append("dev/sps/getStatistic/$controlUuid/$mode/${timeRange.first}/${timeRange.second}/$unit/$groupId")
            if (outputName != null) append("/$outputName")
        }

        /**
         * V1 HTTP path for XML statistic data (via `stats/` prefix).
         * Returns XML with entries in the form `<S T="YYYY-MM-DD HH:MM:SS" V="float"/>`.
         * Timestamps are in local Miniserver time.
         * Uses the control UUID (not the output UUID).
         * @param controlUuid UUID of the control
         * @param date Date in format YYYYMM or YYYYMMDD
         */
        @JvmStatic
        fun statisticDataXml(controlUuid: String, date: String): String =
            "stats/statisticdata.xml/$controlUuid/$date"
    }
}
