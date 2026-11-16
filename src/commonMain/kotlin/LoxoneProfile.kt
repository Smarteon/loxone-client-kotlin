package cz.smarteon.loxkt

import kotlin.jvm.JvmOverloads

/**
 * A Miniserver user's credentials.
 *
 * @property[password] null when the client keeps only the user's token ([LoxoneTokenAuthenticator] then never
 * asks for a new one); [LoxoneAuth.Basic] needs it
 */
data class LoxoneCredentials @JvmOverloads constructor(
    val username: String,
    val password: String? = null,
    val visuPassword: String? = null
)

data class LoxoneProfile @JvmOverloads constructor(
    val endpoint: LoxoneEndpoint,
    val credentials: LoxoneCredentials? = null
)
