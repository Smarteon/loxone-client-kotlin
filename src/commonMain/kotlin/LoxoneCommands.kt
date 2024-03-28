package cz.smarteon.loxone

import cz.smarteon.loxone.message.EmptyLoxoneMsgVal
import cz.smarteon.loxone.message.LoxoneMsg
import cz.smarteon.loxone.message.sysCommand
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
     * Keep alive command used solely in [cz.smarteon.loxone.ktor.WebsocketLoxoneClient] to ensure connection alive
     * functionality.
     */
    val KEEP_ALIVE = object : NoResponseCommand("keepalive") {}

    /**
     * Commands related to tokens management.
     */
    object Tokens {

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
}
