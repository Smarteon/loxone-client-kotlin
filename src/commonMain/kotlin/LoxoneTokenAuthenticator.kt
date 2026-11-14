package cz.smarteon.loxkt

import cz.smarteon.loxkt.LoxoneCommands.Tokens
import cz.smarteon.loxkt.LoxoneCrypto.loxoneHashing
import cz.smarteon.loxkt.message.Hashing
import cz.smarteon.loxkt.message.Hashing.Companion.commandForUser
import cz.smarteon.loxkt.message.LoxoneMsg
import cz.smarteon.loxkt.message.Token
import cz.smarteon.loxkt.message.TokenState
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmOverloads
import kotlin.properties.Delegates

/**
 * Authenticates [profile]'s user with a Miniserver token: gets one with the password, refreshes it before it
 * expires and authenticates WebSockets with it; HTTP clients sign each request with it (`autht`).
 *
 * Without a password ([LoxoneCredentials.password] null) it works with the token in [repository] only: it uses
 * and refreshes the token, but never asks for a new one (a missing or expired token throws [LoxoneException]
 * before anything is sent). [close] kills only a token this authenticator got with the password itself, never
 * one it was handed in [repository].
 */
class LoxoneTokenAuthenticator @JvmOverloads constructor(
    private val profile: LoxoneProfile,
    private val repository: TokenRepository = DEFAULT_TOKEN_REPO,
    private val settings: LoxoneClientSettings = LoxoneClientSettings()
) {

    private val logger = KotlinLogging.logger {}

    val user: String = requireNotNull(profile.credentials) {
        "Credentials can't be null for authenticator"
    }.username

    private val password: String? = profile.credentials?.password

    private val mutex = Mutex()

    private var token: Token? by Delegates.observable(repository.getToken(profile)) { _, _, newValue ->
        if (newValue != null) {
            logger.info {
                "Got loxone token, valid until: ${newValue.validUntil}, " +
                    "seconds to expire: ${newValue.secondsToExpireFromNow()}"
            }
            repository.putToken(profile, newValue)
        } else {
            repository.removeToken(profile)
        }
    }

    // Websockets that are authenticated
    private val authWebsockets = mutableSetOf<WebsocketLoxoneClient>()

    // Whether this authenticator got the token with the password, rather than from the repository
    private var gotToken = false

    suspend fun ensureAuthenticated(client: LoxoneClient) =
        execConditionalWithMutex({
            !TokenState(token).isUsable ||
                (client is WebsocketLoxoneClient && !authWebsockets.contains(client))
        }) {
            val state = TokenState(token)
            val password = password
            if (state.isExpired && password == null) {
                throw LoxoneException("No valid token of $user and no password to get a new one")
            }
            val hashing = client.callForMsg(commandForUser(user))
            when {
                state.isExpired && password != null -> {
                    logger.debug { "Token expired, requesting new one" }
                    token = client.callForMsg(
                        Tokens.get(
                            loxoneHashing(password, hashing, "getttoken", user),
                            user,
                            settings.tokenPermission,
                            settings.clientId,
                            settings.clientInfo
                        )
                    )
                    gotToken = true
                    logger.debug { "Received token: $token" }
                    if (client is WebsocketLoxoneClient) {
                        authWebsockets.add(client)
                    }
                }

                state.needsRefresh -> {
                    logger.debug { "Token needs refresh, sending refreshjwt" }
                    refresh(client, checkNotNull(token) { "Token must be present to refresh" }, hashing)
                    if (client is WebsocketLoxoneClient) {
                        logger.debug { "Authenticating websocket with refreshed token" }
                        val authResponse = client.callForMsg(Tokens.auth(tokenHash(client, "authenticate"), user))
                        token = checkNotNull(token).merge(authResponse)
                        authWebsockets.add(client)
                    }
                }

                else -> {
                    if (client is WebsocketLoxoneClient) {
                        logger.debug { "Authenticating websocket with token $token" }
                        val authResponse = client.callForMsg(Tokens.auth(tokenHash(client, "authenticate"), user))
                        token = token!!.merge(authResponse)
                        authWebsockets.add(client)
                    }
                }
            }
        }

    /**
     * Refreshes the token now, without the password: the Miniserver issues a new token with a new validity and
     * the same rights, which goes to the repository. The previous token stays valid until it expires or the
     * new one is killed (verified on firmware 17.2), so whoever still holds it isn't cut off.
     *
     * @return the new token
     * @throws LoxoneException when there's no valid token to refresh
     */
    suspend fun refreshToken(client: LoxoneClient): Token {
        execConditionalWithMutex({ true }) {
            val current = token?.takeIf { it.filled && it.secondsToExpireFromNow() > 0 }
                ?: throw LoxoneException("No valid token of $user to refresh")
            refresh(client, current, client.callForMsg(commandForUser(user)))
        }
        return checkNotNull(token) { "Token of $user missing after refresh" }
    }

    private suspend fun refresh(client: LoxoneClient, current: Token, hashing: Hashing) {
        val refreshed = client.callForMsg(Tokens.refresh(LoxoneCrypto.loxoneHmac(current, hashing, "refreshjwt"), user))
        token = current.merge(refreshed)
    }

    /**
     * Kills the token on the Miniserver, over a WebSocket or HTTP, with the tokens it was refreshed from. Over
     * HTTP the request is signed with the killed token's `autht` and answered 200, which is no failed login
     * (firmware 17.2); any request with one of those tokens afterwards is, so nothing may use them any more.
     * The token is forgotten even when no answer comes: over a WebSocket the Miniserver may close the session
     * instead. Does nothing when the token is missing or expires within five minutes.
     */
    suspend fun killToken(client: LoxoneClient) = execConditionalWithMutex({ TokenState(token).isUsable }) {
        logger.debug { "Going to kill token $token" }
        try {
            val code = client.call(Tokens.kill(tokenHash(client, "killtoken"), user)).code
            if (code != LoxoneMsg.CODE_AUTH_FAIL && code != LoxoneMsg.CODE_OK) {
                throw LoxoneException("Miniserver answered killtoken with $code")
            }
            logger.info { "Token killed" }
        } finally {
            token = null
            gotToken = false
            authWebsockets.remove(client)
        }
    }

    /**
     * Kills the token on close ([LoxoneClientSettings.killTokenOnClose]) — only over a WebSocket.
     *
     * The HTTP kill used to run whenever the WebSocket kill failed, i.e. exactly when the connection
     * was lost, and a flaky connection then collected failed logins until the Miniserver blocked the
     * client IP (WebSocket closed with 4003 right after opening) — for every client behind the same
     * NAT. A token that can't be killed over its WebSocket is left to expire instead; [killToken]
     * kills it over HTTP when the caller knows it's no longer needed. Only a token this authenticator got
     * with the password: one handed in through the repository may be held by others too (and without a
     * password there would be no way to get a new one).
     */
    suspend fun close(client: LoxoneClient) {
        if (settings.killTokenOnClose && gotToken && client is WebsocketLoxoneClient) {
            killToken(client)
        }
    }

    suspend fun tokenHash(client: LoxoneClient, operation: String) =
        LoxoneCrypto.loxoneHmac(
            checkNotNull(token) { "Token must be present for hashing" },
            client.callForMsg(commandForUser(user)),
            operation
        )

    private suspend fun execConditionalWithMutex(condition: () -> Boolean, block: suspend () -> Unit) {
        // The block's own refreshjwt and killtoken are authenticated: over HTTP the client signs them with
        // `autht` and asks for authentication first, which must not wait for the lock the block holds.
        if (currentCoroutineContext()[Authenticating]?.authenticator === this) return
        if (condition()) {
            mutex.withLock {
                if (condition()) {
                    withContext(Authenticating(this)) { block() }
                }
            }
        }
    }

    private class Authenticating(val authenticator: LoxoneTokenAuthenticator) :
        AbstractCoroutineContextElement(Authenticating) {
        companion object Key : CoroutineContext.Key<Authenticating>
    }
}
