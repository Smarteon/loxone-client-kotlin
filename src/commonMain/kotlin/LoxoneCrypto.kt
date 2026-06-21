package cz.smarteon.loxkt

import cz.smarteon.loxkt.Codec.bytesToHex
import cz.smarteon.loxkt.Codec.concat
import cz.smarteon.loxkt.Codec.concatToBytes
import cz.smarteon.loxkt.message.Hashing
import cz.smarteon.loxkt.message.Token
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import org.kotlincrypto.hash.sha1.SHA1
import org.kotlincrypto.hash.sha2.SHA256
import org.kotlincrypto.macs.hmac.sha1.HmacSHA1
import org.kotlincrypto.macs.hmac.sha2.HmacSHA256

internal expect fun rsaEncryptBytes(data: ByteArray, publicKeyPem: String): ByteArray

/**
 * AES-256-CBC encryption with no padding applied by the platform. The [data] must be a multiple of
 * the 16-byte AES block size - [LoxoneCrypto] applies ZeroBytePadding before calling this.
 */
internal expect fun aesEncryptBytes(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray

/** AES-256-CBC decryption counterpart of [aesEncryptBytes], also without padding handling. */
internal expect fun aesDecryptBytes(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray

/** Returns [size] cryptographically secure random bytes. */
internal expect fun secureRandomBytes(size: Int): ByteArray

internal object LoxoneCrypto {

    private val logger = KotlinLogging.logger {}

    private const val AES_KEY_SIZE = 32
    private const val AES_IV_SIZE = 16
    private const val AES_BLOCK_SIZE = 16
    private const val SALT_SIZE = 2
    private const val PEM_LINE_LENGTH = 64
    private val PEM_HEADER_FOOTER = Regex("-+(?:BEGIN|END) (?:CERTIFICATE|PUBLIC KEY)-+")
    private val WHITESPACE = Regex("\\s")

    @OptIn(ExperimentalEncodingApi::class)
    internal fun rsaEncrypt(data: String, publicKeyPem: String): String =
        Base64.encode(rsaEncryptBytes(data.encodeToByteArray(), publicKeyPem))

    /**
     * Normalizes the public key returned by `jdev/sys/getPublicKey` into a standard
     * `-----BEGIN PUBLIC KEY-----` PEM. The Miniserver wraps the SubjectPublicKeyInfo in
     * `-----BEGIN CERTIFICATE-----` markers (and may omit line breaks), which the RSA decoders
     * don't accept as-is, so we strip the markers/whitespace and re-wrap the base64 body.
     */
    internal fun normalizePublicKeyPem(raw: String): String {
        val base64 = raw.replace(PEM_HEADER_FOOTER, "").replace(WHITESPACE, "")
        val wrapped = base64.chunked(PEM_LINE_LENGTH).joinToString("\n")
        return "-----BEGIN PUBLIC KEY-----\n$wrapped\n-----END PUBLIC KEY-----"
    }

    /**
     * AES-256-CBC encrypts the given [plaintext] using [key] and [iv], applying Loxone's
     * ZeroBytePadding (pad to a 16-byte boundary with `0x00`), and returns the Base64 encoded result.
     */
    @OptIn(ExperimentalEncodingApi::class)
    internal fun aesEncrypt(plaintext: String, key: ByteArray, iv: ByteArray): String {
        val bytes = plaintext.encodeToByteArray()
        val padding = (AES_BLOCK_SIZE - bytes.size % AES_BLOCK_SIZE) % AES_BLOCK_SIZE
        val padded = if (padding == 0) bytes else bytes.copyOf(bytes.size + padding)
        return Base64.encode(aesEncryptBytes(padded, key, iv))
    }

    /**
     * Decrypts the Base64 encoded AES-256-CBC [base64Cipher] using [key] and [iv] and strips the
     * trailing ZeroBytePadding. Counterpart of [aesEncrypt].
     */
    @OptIn(ExperimentalEncodingApi::class)
    internal fun aesDecrypt(base64Cipher: String, key: ByteArray, iv: ByteArray): String =
        aesDecryptBytes(Base64.decode(base64Cipher), key, iv).decodeToString().trimEnd('\u0000')

    /** Generates a random 32-byte (256-bit) AES key. */
    internal fun generateAesKey(): ByteArray = secureRandomBytes(AES_KEY_SIZE)

    /** Generates a random 16-byte AES initialization vector. */
    internal fun generateAesIv(): ByteArray = secureRandomBytes(AES_IV_SIZE)

    /**
     * Generates a random salt as a hex string used for command encryption (anti-replay).
     * Note this is not the user salt obtained via the getkey2 command.
     */
    internal fun generateSalt(): String = bytesToHex(secureRandomBytes(SALT_SIZE))

    /**
     * Builds the RSA-encrypted session key to exchange with the Miniserver. The AES [key] and [iv]
     * are hex encoded and joined as `"{keyHex}:{ivHex}"`, then RSA encrypted with [publicKeyPem].
     */
    internal fun createSessionKey(key: ByteArray, iv: ByteArray, publicKeyPem: String): String =
        rsaEncrypt(concat(bytesToHex(key), bytesToHex(iv)), publicKeyPem)

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

    fun loxoneHmac(token: Token, hashing: Hashing, operation: String): String =
        loxoneHmac(checkNotNull(token.token) { "Can't hash non-filled token" }, hashing, operation)

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
