package cz.smarteon.loxkt

import cz.smarteon.loxkt.app.LoxoneAppCommand
import cz.smarteon.loxkt.app.LoxoneAppVersionCommand
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
     * Keep alive command used solely in [cz.smarteon.loxone.WebsocketLoxoneClient] to ensure connection alive
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
            authenticated = false
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
            authenticated = false
        )

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
    }
}
