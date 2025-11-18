package cz.smarteon.loxkt

import co.touchlab.stately.collections.ConcurrentMutableMap
import cz.smarteon.loxkt.message.PublicKey

/**
 * Repository for storing and retrieving Miniserver RSA public keys.
 */
interface PublicKeyRepository {
    /**
     * Get the public key for the given profile from repository.
     * @param profile Loxone profile
     * @return public key for given profile, null if there is no such key
     */
    fun getPublicKey(profile: LoxoneProfile): PublicKey?

    /**
     * Put the public key for given profile to the repository.
     * @param profile Loxone profile
     * @param publicKey public key to be stored
     */
    fun putPublicKey(profile: LoxoneProfile, publicKey: PublicKey)

    /**
     * Remove the public key for given profile from the repository.
     * @param profile Loxone profile
     */
    fun removePublicKey(profile: LoxoneProfile)
}

/**
 * In-memory implementation of [PublicKeyRepository].
 */
class InMemoryPublicKeyRepository : PublicKeyRepository {

    private val storage = ConcurrentMutableMap<LoxoneProfile, PublicKey>()

    override fun getPublicKey(profile: LoxoneProfile): PublicKey? = storage[profile]

    override fun putPublicKey(profile: LoxoneProfile, publicKey: PublicKey) {
        storage[profile] = publicKey
    }

    override fun removePublicKey(profile: LoxoneProfile) {
        storage.remove(profile)
    }
}

/**
 * Default public key repository instance.
 */
internal val DEFAULT_PUBLIC_KEY_REPO: PublicKeyRepository by lazy { InMemoryPublicKeyRepository() }
