package cz.smarteon.loxone.message

import kotlinx.serialization.Serializable

@Serializable
data class Hashing(
    @Serializable(HexSerializer::class) val key: ByteArray,
    val salt: String,
    val hashAlg: String
) : LoxoneMsgVal {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Hashing

        if (!key.contentEquals(other.key)) return false
        if (salt != other.salt) return false
        if (hashAlg != other.hashAlg) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.contentHashCode()
        result = 31 * result + salt.hashCode()
        result = 31 * result + hashAlg.hashCode()
        return result
    }

    companion object {
        fun commandForUser(user: String) = sysCommand<Hashing>("getkey2", user)
    }
}
