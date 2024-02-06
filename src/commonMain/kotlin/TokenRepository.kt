package cz.smarteon.loxone

import co.touchlab.stately.collections.ConcurrentMutableMap
import cz.smarteon.loxone.message.Token

interface TokenRepository {
    /**
     * Get the token for profile from repository.
     * @param profile Loxone profile
     * @return token for given profile, null if there is no such token
     */
    fun getToken(profile: LoxoneProfile): Token?

    /**
     * Put the token for given profile to the repository.
     * @param profile Loxone profile
     * @param token token to be put
     */
    fun putToken(profile: LoxoneProfile, token: Token)

    /**
     * Remove the token for given profile from the repository.
     * @param profile Loxone profile
     */
    fun removeToken(profile: LoxoneProfile)
}

class InMemoryTokenRepository : TokenRepository {

    private val storage = ConcurrentMutableMap<LoxoneProfile, Token>()
    override fun getToken(profile: LoxoneProfile): Token? = storage[profile]

    override fun putToken(profile: LoxoneProfile, token: Token) {
        storage[profile] = token
    }

    override fun removeToken(profile: LoxoneProfile) {
        storage.remove(profile)
    }
}

internal val DEFAULT_TOKEN_REPO: TokenRepository by lazy { InMemoryTokenRepository() }
