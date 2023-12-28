package cz.smarteon.loxone

import cz.smarteon.loxone.message.Hashing
import cz.smarteon.loxone.message.Hashing.Companion.commandForUser
import cz.smarteon.loxone.message.Token
import cz.smarteon.loxone.message.Token.Companion.commandGetToken
import cz.smarteon.loxone.message.TokenState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.jvm.JvmOverloads
import kotlin.properties.Delegates

class LoxoneTokenAuthenticator @JvmOverloads constructor(
    private val profile: LoxoneProfile,
    private val repository: TokenRepository = DEFAULT_TOKEN_REPO,
    private val settings: LoxoneClientSettings = LoxoneClientSettings()
) {
    val user: String = requireNotNull(profile.credentials) {
        "Credentials can't be null for authenticator"
    }.username

    private val mutex = Mutex()

    private var hashing: Hashing? = null

    private var token: Token? by Delegates.observable(repository.getToken(profile)) { _, _, newValue ->
        checkNotNull(newValue) {
            "token can't be set to null"
        }
        repository.putToken(profile, newValue)
    }

    suspend fun ensureAuthenticated(client: LoxoneClient) {
        mutex.withLock {
            if (hashing == null) {
                hashing = client.callForMsg(commandForUser(user))
            }
            val state = TokenState(token)
            when {
                state.isExpired -> {
                    token = client.callForMsg(
                        commandGetToken(
                            LoxoneCrypto.loxoneHashing(profile.credentials!!.password, checkNotNull(hashing), "getttoken", user),
                            user,
                            settings.tokenPermission,
                            settings.clientId,
                            settings.clientInfo
                        )
                    )
                    println("got token")
                }

                state.needsRefresh -> {
                    TODO("refresh and merge token")
                }

                else -> {
                    // TODO("send authwithtoken if websockets")
                }
            }

        }
    }

    fun tokenHash(operation: String) = LoxoneCrypto.loxoneHmac(checkNotNull(token), operation)
}
