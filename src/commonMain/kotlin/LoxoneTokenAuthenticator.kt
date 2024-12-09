package cz.smarteon.loxkt

import cz.smarteon.loxkt.LoxoneCommands.Tokens
import cz.smarteon.loxkt.LoxoneCrypto.loxoneHashing
import cz.smarteon.loxkt.message.Hashing.Companion.commandForUser
import cz.smarteon.loxkt.message.Token
import cz.smarteon.loxkt.message.TokenState
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
                    TODO("refresh and merge token")
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

    suspend fun killToken(client: LoxoneClient) = execConditionalWithMutex({ TokenState(token).isUsable }) {
        logger.debug { "Going to kill token $token" }
        client.callForMsg(Tokens.kill(tokenHash(client, "killtoken"), user))
        logger.info { "Token killed" }
        token = null
        authWebsockets.remove(client)
    }

    suspend fun close(client: LoxoneClient) {
        if (settings.killTokenOnClose) {
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
        if (condition()) {
            mutex.withLock {
                if (condition()) {
                    block()
                }
            }
        }
    }
}
