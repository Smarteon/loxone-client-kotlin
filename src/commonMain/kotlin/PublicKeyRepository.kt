package cz.smarteon.loxkt

import co.touchlab.stately.collections.ConcurrentMutableMap
import cz.smarteon.loxkt.message.PublicKey

/** Repository for storing and retrieving Miniserver RSA public keys per [LoxoneProfile]. */
interface PublicKeyRepository {
    fun getPublicKey(profile: LoxoneProfile): PublicKey?
    fun putPublicKey(profile: LoxoneProfile, publicKey: PublicKey)
    fun removePublicKey(profile: LoxoneProfile)
}

/** In-memory implementation of [PublicKeyRepository]. */
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

internal val DEFAULT_PUBLIC_KEY_REPO: PublicKeyRepository by lazy { InMemoryPublicKeyRepository() }
