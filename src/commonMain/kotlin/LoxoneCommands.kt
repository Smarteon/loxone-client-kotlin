package cz.smarteon.loxkt

import cz.smarteon.loxkt.LoxoneCommands.App.version
import cz.smarteon.loxkt.app.LoxoneAppCommand
import cz.smarteon.loxkt.app.LoxoneAppVersionCommand
import cz.smarteon.loxkt.app.StatisticUnit
import cz.smarteon.loxkt.message.EmptyLoxoneMsgVal
import cz.smarteon.loxkt.message.LoxoneMsg
import cz.smarteon.loxkt.message.SimpleLoxoneMsgCommand
import cz.smarteon.loxkt.message.Token
import cz.smarteon.loxkt.message.TokenPermission
import cz.smarteon.loxkt.message.sysCommand
import kotlin.jvm.JvmStatic

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
         */
        @JvmStatic
        fun refresh(tokenHash: String, user: String) =
            sysCommand<Token>("refreshjwt", tokenHash, user, authenticated = false)

        /**
         * Command to kill token.
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
