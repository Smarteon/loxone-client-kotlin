package cz.smarteon.loxkt.ktor

import cz.smarteon.loxkt.CommandEncryption
import cz.smarteon.loxkt.LoxoneException
import cz.smarteon.loxkt.RsaTestFixtures
import cz.smarteon.loxkt.message.EmptyLoxoneMsgVal
import cz.smarteon.loxkt.message.SimpleLoxoneMsgCommand
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import java.net.URLDecoder
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * End-to-end command encryption test: a simulated Miniserver performs the RSA key exchange and
 * AES-decrypts the wrapped commands, asserting the inner command, salt rotation, and fenc responses.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CommandEncryptionIT : StringSpec({

    fun publicKeyResponse(): String {
        // Mimic the Miniserver: the SubjectPublicKeyInfo base64 is wrapped in CERTIFICATE markers
        // and returned as a plain string value (single line, no PEM line breaks).
        val base64 = RsaTestFixtures.TEST_PUBLIC_KEY_PEM
            .replace(Regex("-+(?:BEGIN|END) PUBLIC KEY-+"), "")
            .replace(Regex("\\s"), "")
        val certWrapped = "-----BEGIN CERTIFICATE-----$base64-----END CERTIFICATE-----"
        return """{"LL":{"control":"dev/sys/getPublicKey","value":"$certWrapped","code":"200"}}"""
    }

    "REQUEST mode performs key exchange, wraps commands as enc, and rotates the salt" {
        val miniserver = MiniserverCryptoSimulator()
        val decryptedCommands = mutableListOf<String>()

        val ctx = startTestWebSocketServer(
            httpPublicKeyJson = publicKeyResponse(),
            fallback = { payload ->
                when {
                    payload.startsWith("jdev/sys/keyexchange/") -> {
                        miniserver.acceptKeyExchange(payload.substringAfter("jdev/sys/keyexchange/"))
                        sendLoxoneResponse("""{"LL":{"control":"dev/sys/keyexchange","value":"","code":"200"}}""")
                    }
                    payload.startsWith("jdev/sys/enc/") -> {
                        decryptedCommands += miniserver.decryptCommand(payload.substringAfter("jdev/sys/enc/"))
                        sendLoxoneResponse("""{"LL":{"control":"enc","value":"ok","code":"200"}}""")
                    }
                }
            }
        )
        val client = KtorWebsocketLoxoneClient(
            ctx.testedClient,
            commandEncryption = CommandEncryption.REQUEST,
            dispatcher = UnconfinedTestDispatcher()
        )

        client.callRaw("jdev/sps/io/AI1/on")
        client.callRaw("jdev/sps/io/AI2/off")
        client.callRaw("jdev/sps/io/AI3/on")

        // getPublicKey is fetched over HTTP, so the first websocket frame is the key exchange
        ctx.received.receive() shouldStartWith "jdev/sys/keyexchange/"
        repeat(3) { ctx.received.receive() shouldStartWith "jdev/sys/enc/" }

        // first command carries the initial salt; each subsequent command rotates, using the
        // previous command's nextSalt as its prevSalt (proves the salt sequence advances correctly)
        decryptedCommands[0] shouldBe "salt/${miniserver.salts[0]}/jdev/sps/io/AI1/on"
        decryptedCommands[1] shouldBe
            "nextSalt/${miniserver.salts[0]}/${miniserver.salts[1]}/jdev/sps/io/AI2/off"
        decryptedCommands[2] shouldBe
            "nextSalt/${miniserver.salts[1]}/${miniserver.salts[2]}/jdev/sps/io/AI3/on"

        client.close()
    }

    "REQUEST_RESPONSE mode decrypts the fenc response" {
        val miniserver = MiniserverCryptoSimulator()

        val ctx = startTestWebSocketServer(
            httpPublicKeyJson = publicKeyResponse(),
            fallback = { payload ->
                when {
                    payload.startsWith("jdev/sys/keyexchange/") -> {
                        miniserver.acceptKeyExchange(payload.substringAfter("jdev/sys/keyexchange/"))
                        sendLoxoneResponse("""{"LL":{"control":"dev/sys/keyexchange","value":"","code":"200"}}""")
                    }
                    payload.startsWith("jdev/sys/fenc/") -> {
                        miniserver.decryptCommand(payload.substringAfter("jdev/sys/fenc/"))
                        val plainJson = """{"LL":{"control":"fenc","value":"secret","code":"200"}}"""
                        sendLoxoneResponse(miniserver.encryptResponse(plainJson))
                    }
                }
            }
        )
        val client = KtorWebsocketLoxoneClient(
            ctx.testedClient,
            commandEncryption = CommandEncryption.REQUEST_RESPONSE,
            dispatcher = UnconfinedTestDispatcher()
        )

        client.callRaw("jdev/sps/io/AI1/state") shouldBe
            """{"LL":{"control":"fenc","value":"secret","code":"200"}}"""

        client.close()
    }

    "NONE mode still encrypts commands flagged encrypted while leaving normal commands plain" {
        val miniserver = MiniserverCryptoSimulator()
        val decryptedCommands = mutableListOf<String>()

        val ctx = startTestWebSocketServer(
            httpPublicKeyJson = publicKeyResponse(),
            fallback = { payload ->
                when {
                    payload.startsWith("jdev/sys/keyexchange/") -> {
                        miniserver.acceptKeyExchange(payload.substringAfter("jdev/sys/keyexchange/"))
                        sendLoxoneResponse("""{"LL":{"control":"dev/sys/keyexchange","value":"","code":"200"}}""")
                    }
                    payload.startsWith("jdev/sys/enc/") -> {
                        decryptedCommands += miniserver.decryptCommand(payload.substringAfter("jdev/sys/enc/"))
                        sendLoxoneResponse("""{"LL":{"control":"enc","value":"ok","code":"200"}}""")
                    }
                    else -> sendLoxoneResponse("""{"LL":{"control":"$payload","value":"ok","code":"200"}}""")
                }
            }
        )
        // NONE mode (default) - a command flagged encrypted must still be encrypted (e.g. token commands)
        val client = KtorWebsocketLoxoneClient(ctx.testedClient, dispatcher = UnconfinedTestDispatcher())

        val encryptedCmd = SimpleLoxoneMsgCommand(
            listOf("jdev", "sys", "gettoken", "HASH"),
            EmptyLoxoneMsgVal::class,
            authenticated = false,
            encrypted = true
        )
        client.call(encryptedCmd)
        client.callRaw("jdev/cfg/api")

        // the encrypted command triggers key exchange even in NONE mode, and goes out as enc
        ctx.received.receive() shouldStartWith "jdev/sys/keyexchange/"
        ctx.received.receive() shouldStartWith "jdev/sys/enc/"
        // the normal command is sent verbatim (not encrypted)
        ctx.received.receive() shouldBe "jdev/cfg/api"
        decryptedCommands[0] shouldBe "salt/${miniserver.salts[0]}/jdev/sys/gettoken/HASH"

        client.close()
    }

    "key exchange failure surfaces as LoxoneException" {
        val ctx = startTestWebSocketServer(
            httpPublicKeyJson = publicKeyResponse(),
            fallback = { payload ->
                if (payload.startsWith("jdev/sys/keyexchange/")) {
                    sendLoxoneResponse("""{"LL":{"control":"keyexchange","value":"","code":"401"}}""")
                }
            }
        )
        val client = KtorWebsocketLoxoneClient(
            ctx.testedClient,
            commandEncryption = CommandEncryption.REQUEST,
            dispatcher = UnconfinedTestDispatcher()
        )
        shouldThrow<LoxoneException> { client.callRaw("jdev/cfg/api") }
        client.close()
    }

    "public key fetch failure surfaces as LoxoneException" {
        val ctx = startTestWebSocketServer(
            httpPublicKeyJson = """{"LL":{"control":"getPublicKey","value":"","code":"500"}}""",
            fallback = { }
        )
        val client = KtorWebsocketLoxoneClient(
            ctx.testedClient,
            commandEncryption = CommandEncryption.REQUEST,
            dispatcher = UnconfinedTestDispatcher()
        )
        shouldThrow<LoxoneException> { client.callRaw("jdev/cfg/api") }
        client.close()
    }
})

/** Simulates the Miniserver side of command encryption using the RSA test fixtures and the JDK AES. */
private class MiniserverCryptoSimulator {
    // Single volatile holder so the key + iv are published together (safe publication across the
    // server's frame-handling coroutine, which may hop threads between suspensions).
    @Volatile private var keyIv: Pair<ByteArray, ByteArray>? = null
    val salts = mutableListOf<String>()

    /** Recovers the AES key + iv from the RSA-encrypted session key (raw Base64, not url-encoded). */
    fun acceptKeyExchange(sessionKeyBase64: String) {
        val sessionKey = RsaTestFixtures.decryptWithPrivateKey(sessionKeyBase64)
        val (keyHex, ivHex) = sessionKey.split(":")
        keyIv = keyHex.hexToBytes() to ivHex.hexToBytes()
    }

    /** Decrypts an url-encoded, Base64 AES cipher and records the salt, returning the inner command. */
    fun decryptCommand(encodedCipher: String): String {
        val plain = aes(Cipher.DECRYPT_MODE, Base64.getDecoder().decode(urlDecode(encodedCipher)))
            .decodeToString().trimEnd('\u0000')
        // plaintext is "salt/{salt}/{cmd}" or "nextSalt/{prevSalt}/{nextSalt}/{cmd}"
        val parts = plain.split("/")
        if (parts[0] == "salt") salts += parts[1] else salts += parts[2]
        return plain
    }

    /** AES-encrypts the response JSON (ZeroBytePadding) and Base64 encodes it for the fenc channel. */
    fun encryptResponse(json: String): String {
        val bytes = json.encodeToByteArray()
        val padded = bytes.copyOf(bytes.size + (16 - bytes.size % 16) % 16)
        return Base64.getEncoder().encodeToString(aes(Cipher.ENCRYPT_MODE, padded))
    }

    private fun aes(mode: Int, data: ByteArray): ByteArray {
        val (key, iv) = checkNotNull(keyIv) { "key exchange not completed before command" }
        return Cipher.getInstance("AES/CBC/NoPadding").run {
            init(mode, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            doFinal(data)
        }
    }

    private fun urlDecode(value: String): String = URLDecoder.decode(value, "UTF-8")

    @OptIn(ExperimentalStdlibApi::class)
    private fun String.hexToBytes(): ByteArray = hexToByteArray()
}
