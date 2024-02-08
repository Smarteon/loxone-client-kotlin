package cz.smarteon.loxone

import cz.smarteon.loxone.Codec.bytesToHex
import cz.smarteon.loxone.Codec.concat
import cz.smarteon.loxone.Codec.concatToBytes
import cz.smarteon.loxone.message.Hashing
import cz.smarteon.loxone.message.Token
import io.github.oshai.kotlinlogging.KotlinLogging
import org.kotlincrypto.hash.sha1.SHA1
import org.kotlincrypto.hash.sha2.SHA256
import org.kotlincrypto.macs.hmac.sha1.HmacSHA1
import org.kotlincrypto.macs.hmac.sha2.HmacSHA256

internal object LoxoneCrypto {

    private val logger = KotlinLogging.logger {}

    /**
     * Performs hashing algorithm as required by loxone specification.
     * Use this function when the secret is plain text (i.e. password or visualization password).
     *
     * @param secret to be hashed
     * @param hashing hashing specification
     * @param operation description of the operation the hashing is needed for - just for logging purposes
     * @param loxoneUser to be hashed, can be null
     * @return loxone hash of given parameters
     */
    fun loxoneHashing(
        secret: String,
        hashing: Hashing,
        operation: String,
        loxoneUser: String? = null
    ): String {
        val secretHash = loxoneDigest(secret, hashing)

        logger.trace { "$operation hash: $secretHash" }

        val toFinalHash = loxoneUser?.let { concat(it, secretHash) } ?: secretHash
        return loxoneHmac(toFinalHash, hashing, operation)
    }

    /**
     * Performs hashing algorithm as required by loxone specification.
     * Use this function when only the given secret is already HEX encoded SHA-1 hash (i.e. token).
     *
     * @param secret to be hashed
     * @param hashing hashing specification
     * @param operation description of the operation the hashing is needed for - just for logging purposes
     * @return loxone hmac of given parameters
     */
    @OptIn(ExperimentalStdlibApi::class)
    fun loxoneHmac(secret: String, hashing: Hashing, operation: String): String {
        val mac = when (hashing.hashAlg) {
            null, "SHA1" -> HmacSHA1(hashing.key)
            "SHA256" -> HmacSHA256(hashing.key)
            else -> throw LoxoneException("Unsupported hashing algorithm \"${hashing.hashAlg}\"")
        }
        return bytesToHex(mac.doFinal(secret.encodeToByteArray())).also { finalHash ->
            logger.trace { "$operation final hash: $finalHash" }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun loxoneHmac(token: Token, operation: String): String =
        token.withTokenAndKey { tokenVal, key ->
            bytesToHex(HmacSHA256(key).doFinal(tokenVal.encodeToByteArray())).also { finalHash ->
                logger.trace { "$operation final hash: $finalHash" }
            }
        }

    @OptIn(ExperimentalStdlibApi::class)
    private fun loxoneDigest(secret: String, hashing: Hashing): String {
        val digest = when (hashing.hashAlg) {
            null, "SHA1" -> SHA1()
            "SHA256" -> SHA256()
            else -> throw LoxoneException("Unsupported hashing algorithm \"${hashing.hashAlg}\"")
        }
        return bytesToHex(
            digest.digest(concatToBytes(secret, hashing.salt)),
            HexFormat.UpperCase
        )
    }
}
