package cz.smarteon.loxkt

import cz.smarteon.loxkt.LoxoneCommands.Tokens
import cz.smarteon.loxkt.LoxoneCrypto.loxoneHashing
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

class LoxoneTokenAuthenticator @JvmOverloads constructor(
    private val profile: LoxoneProfile,
    private val repository: TokenRepository = DEFAULT_TOKEN_REPO,
    private val settings: LoxoneClientSettings = LoxoneClientSettings()
) {

    private val logger = KotlinLogging.logger {}

    val user: String = requireNotNull(profile.credentials) {
        "Credentials can't be null for authenticator"
    }.username

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

    suspend fun ensureAuthenticated(client: LoxoneClient) =
        execConditionalWithMutex({
            !TokenState(token).isUsable ||
                (client is WebsocketLoxoneClient && !authWebsockets.contains(client))
        }) {
            val hashing = client.callForMsg(commandForUser(user))
            val state = TokenState(token)
            when {
                state.isExpired -> {
                    logger.debug { "Token expired, requesting new one" }
                    token = client.callForMsg(
                        Tokens.get(
                            loxoneHashing(profile.credentials!!.password, hashing, "getttoken", user),
                            user,
                            settings.tokenPermission,
                            settings.clientId,
                            settings.clientInfo
                        )
                    )
                    logger.debug { "Received token: $token" }
                    if (client is WebsocketLoxoneClient) {
                        authWebsockets.add(client)
                    }
                }

                state.needsRefresh -> {
                    logger.debug { "Token needs refresh, sending refreshjwt" }
                    val currentToken = checkNotNull(token) { "Token must be present to refresh" }
                    val refreshed = client.callForMsg(
                        Tokens.refresh(LoxoneCrypto.loxoneHmac(currentToken, hashing, "refreshjwt"), user)
                    )
                    token = currentToken.merge(refreshed)
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
     * Kills the token on the Miniserver, over a WebSocket or HTTP. Over HTTP the request is signed with the
     * killed token's `autht` and answered 200, which is no failed login (firmware 17.2); any request with the
     * token afterwards is one, so nothing may use it any more.
     */
    suspend fun killToken(client: LoxoneClient) = execConditionalWithMutex({ TokenState(token).isUsable }) {
        logger.debug { "Going to kill token $token" }
        val code = client.call(Tokens.kill(tokenHash(client, "killtoken"), user)).code
        if (code != LoxoneMsg.CODE_AUTH_FAIL && code != LoxoneMsg.CODE_OK) {
            throw LoxoneException("Miniserver answered killtoken with $code")
        }
        logger.info { "Token killed" }
        token = null
        authWebsockets.remove(client)
    }

    /**
     * Kills the token on close ([LoxoneClientSettings.killTokenOnClose]) — only over a WebSocket.
     *
     * The HTTP kill used to run whenever the WebSocket kill failed, i.e. exactly when the connection
     * was lost, and a flaky connection then collected failed logins until the Miniserver blocked the
     * client IP (WebSocket closed with 4003 right after opening) — for every client behind the same
     * NAT. A token that can't be killed over its WebSocket is left to expire instead; [killToken]
     * kills it over HTTP when the caller knows it's no longer needed.
     */
    suspend fun close(client: LoxoneClient) {
        if (settings.killTokenOnClose && client is WebsocketLoxoneClient) {
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
