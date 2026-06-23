package cz.smarteon.loxkt

import cz.smarteon.loxkt.message.TokenPermission
import kotlin.jvm.JvmOverloads

data class LoxoneClientSettings @JvmOverloads constructor(
    val tokenPermission: TokenPermission = TokenPermission.APP,
    val clientId: String = DEFAULT_CLIENT_ID,
    val clientInfo: String = DEFAULT_CLIENT_INFO,
    val killTokenOnClose: Boolean = true
) {
    companion object {
        const val DEFAULT_CLIENT_ID = "df184362-73fc-5d3e-ab0ec7c1c3e5bb2e"
        const val DEFAULT_CLIENT_INFO = "loxoneKotlin"
    }
}

/**
 * Controls how commands are encrypted before being sent to the Miniserver.
 *
 * Note that commands which the Miniserver only accepts encrypted (e.g. token acquisition, see
 * [Command.encrypted]) are always encrypted regardless of this setting.
 */
enum class CommandEncryption {
    /** No general command encryption; only commands explicitly requiring it are encrypted. */
    NONE,

    /** Encrypt every command via `jdev/sys/enc/`; responses are returned as plain text. */
    REQUEST,

    /**
     * Encrypt every command via `jdev/sys/fenc/`; responses are AES-encrypted and decrypted by the client.
     *
     * Not suitable for commands returning binary/large payloads (images, files, `LoxAPP3.json`): the
     * Miniserver only supports `enc` for those, and the encrypted-response path expects a text frame.
     */
    REQUEST_RESPONSE
}
