package cz.smarteon.loxone

/**
 * Represents Loxone authentication method.
 */
sealed interface LoxoneAuth {
    /**
     * No authentication.
     */
    data object None : LoxoneAuth

    /**
     * Token based authentication. Can be used both for HTTP and WebSocket.
     * @param[authenticator] Token authenticator.
     */
    class Token(val authenticator: LoxoneTokenAuthenticator) : LoxoneAuth

    /**
     * Basic authentication. Only suitable for HTTP.
     * @param[username] Loxone username.
     * @param[password] Loxone password.
     */
    class Basic(val username: String, val password: String) : LoxoneAuth {
        constructor(credentials: LoxoneCredentials) : this(credentials.username, credentials.password)
        constructor(profile: LoxoneProfile) : this(
            requireNotNull(profile.credentials) { "Credentials are required for Basic authentication" }
        )
    }

    /**
     * Returns token authenticator if this authentication is token based.
     */
    val tokenAuthenticator: LoxoneTokenAuthenticator?
        get() = when (this) {
            is Token -> authenticator
            else -> null
        }
}
