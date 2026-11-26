package cz.smarteon.loxkt

import cz.smarteon.loxkt.ktor.KtorHttpLoxoneClient
import cz.smarteon.loxkt.ktor.KtorWebsocketLoxoneClient
import cz.smarteon.loxkt.message.ApiInfo
import cz.smarteon.loxkt.message.Hashing
import cz.smarteon.loxkt.message.LoginCode
import cz.smarteon.loxkt.message.LoxoneMsg
import cz.smarteon.loxkt.message.MiniserverUser
import cz.smarteon.loxkt.message.SimpleLoxoneMsgCommand
import cz.smarteon.loxkt.message.TokenPermission
import cz.smarteon.loxkt.message.UserGroup
import cz.smarteon.loxkt.message.unquoteJson
import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.WordSpec
import io.kotest.core.spec.style.wordSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.string.shouldMatch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.kotlincrypto.macs.hmac.sha1.HmacSHA1
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Common acceptance tests for all LoxoneClient types
 * @param loxoneClient LoxoneClient to test
 */
fun commonAT(loxoneClient: LoxoneClient) = wordSpec {
    "LoxoneClient of type ${loxoneClient::class.simpleName}" should {
        "callRaw jdev/cfg/api" {
            loxoneClient.callRaw("jdev/cfg/api") shouldContain "200"
        }
        "call ApiInfo" {
            loxoneClient.call(ApiInfo.command).code shouldBe "200"
        }
        "callForMsg ApiInfo" {
            loxoneClient.callForMsg(ApiInfo.command).version shouldMatch ".*\\d+\\.\\d+\\.\\d+.*"
        }
    }
}

/**
 * Acceptance tests for WebSocket-specific functionality.
 * @param client WebSocket client to test
 * @param authenticator authenticator backing the client
 * @param user Loxone username
 */
fun websocketAT(
    client: KtorWebsocketLoxoneClient,
    authenticator: LoxoneTokenAuthenticator,
    user: String
) = wordSpec {
    "KtorWebsocketLoxoneClient" When {
        "WebSocket token" should {
            "be refreshable via refreshjwt" {
                client.callForMsg(ApiInfo.command)
                val refreshed = client.callForMsg(
                    LoxoneCommands.Tokens.refresh(authenticator.tokenHash(client, "refreshjwt"), user)
                )
                refreshed.validUntil shouldBeGreaterThan 0L
                refreshed.token.shouldNotBeNull()
            }
        }
    }
}

/** APP, expert mode, SYS_WS, user management and firmware update, as Doctor asks for them. */
private val ADMIN_TOKEN_PERMISSION = TokenPermission.APP + TokenPermission.EXPERT_MODE + TokenPermission.SYS_WS +
    TokenPermission.USER_MANAGEMENT + TokenPermission.TRIGGER_UPDATE

private val WEB_TOKEN_LIFETIME = 12.hours

/**
 * A token with [ADMIN_TOKEN_PERMISSION], taken with the password, then used, refreshed and killed over HTTP
 * without it, and a WEB token made from it over a WebSocket. Needs an admin user. Every token it takes is
 * killed again.
 */
fun tokenWithoutPasswordAT(endpoint: LoxoneEndpoint, user: String, password: String) = wordSpec {
    "A token without password" should {
        "be used, refreshed, made a WEB token from and killed" {
            val tokens = InMemoryTokenRepository()
            val withPassword = LoxoneProfile(endpoint, LoxoneCredentials(user, password))
            val settings = LoxoneClientSettings(tokenPermission = ADMIN_TOKEN_PERMISSION, killTokenOnClose = false)
            withTokenClient(endpoint, LoxoneTokenAuthenticator(withPassword, tokens, settings)) {
                it.callRaw("jdev/sps/LoxAPPversion3") shouldContain "200"
            }
            val token = tokens.getToken(withPassword).shouldNotBeNull()
            (ADMIN_TOKEN_PERMISSION in TokenPermission(token.rights.shouldNotBeNull())) shouldBe true

            val tokenOnly = LoxoneProfile(endpoint, LoxoneCredentials(user))
            tokens.putToken(tokenOnly, token)
            val authenticator = LoxoneTokenAuthenticator(tokenOnly, tokens)
            withTokenClient(endpoint, authenticator) { client ->
                client.callRaw("jdev/sps/LoxAPPversion3") shouldContain "200"
                authenticator.refreshToken(client)
                tokens.getToken(tokenOnly).shouldNotBeNull().asClue {
                    it.token shouldNotBe token.token
                    it.rights shouldBe token.rights
                }
                client.callRaw("jdev/sps/LoxAPPversion3") shouldContain "200"
                val previous =
                    LoxoneTokenAuthenticator(tokenOnly, InMemoryTokenRepository().apply { putToken(tokenOnly, token) })
                withTokenClient(endpoint, previous) { it.callRaw("jdev/sps/LoxAPPversion3") shouldContain "200" }

                val ws = KtorWebsocketLoxoneClient(endpoint, authenticator)
                val web = try {
                    authenticator.mintToken(ws, TokenPermission.WEB).also {
                        shouldThrow<LoxoneException> { authenticator.mintToken(ws, TokenPermission.CONFIG) }
                            .code shouldBe "412"
                    }
                } finally {
                    ws.close()
                }
                (TokenPermission.WEB in TokenPermission(web.rights.shouldNotBeNull())) shouldBe true
                (web.secondsToExpireFromNow().seconds - WEB_TOKEN_LIFETIME).absoluteValue shouldBeLessThan 1.hours
                val webOnly =
                    LoxoneTokenAuthenticator(tokenOnly, InMemoryTokenRepository().apply { putToken(tokenOnly, web) })
                withTokenClient(endpoint, webOnly) {
                    it.callRaw("jdev/sps/LoxAPPversion3") shouldContain "200"
                    webOnly.killToken(it)
                }

                // Kills the token from before the refresh too, so this run leaves none behind.
                authenticator.killToken(client)
                tokens.getToken(tokenOnly) shouldBe null
                shouldThrow<LoxoneException> { client.callRaw("jdev/sps/LoxAPPversion3") }
            }
        }
    }
}

private suspend fun withTokenClient(
    endpoint: LoxoneEndpoint,
    authenticator: LoxoneTokenAuthenticator,
    block: suspend (KtorHttpLoxoneClient) -> Unit
) {
    val client = KtorHttpLoxoneClient(endpoint, LoxoneAuth.Token(authenticator))
    try {
        block(client)
    } finally {
        client.close()
    }
}

/** APP, user management and setting a user's password, as [userManagementAT] needs them. */
private val USER_TOKEN_PERMISSION =
    TokenPermission.APP + TokenPermission.USER_MANAGEMENT + TokenPermission.CHANGE_PWD

/**
 * Acceptance tests for user management over both clients, on a token of their own with [USER_TOKEN_PERMISSION],
 * which the WebSocket client kills when closed. They create a user valid for 30 minutes, which the Miniserver
 * deletes on its own should the test fail before deleting it. Needs a user with the user management right.
 */
fun userManagementAT(endpoint: LoxoneEndpoint, user: String, password: String) = wordSpec {
    val authenticator = LoxoneTokenAuthenticator(
        LoxoneProfile(endpoint, LoxoneCredentials(user, password)),
        InMemoryTokenRepository(),
        LoxoneClientSettings(tokenPermission = USER_TOKEN_PERMISSION)
    )
    "User management" should {
        "create a time-limited user over HTTP that can log in, then delete it" {
            withTokenClient(endpoint, authenticator) { manageTemporaryUser(it, endpoint) }
        }
        "create a time-limited user over WebSocket that can log in, then delete it" {
            val client = KtorWebsocketLoxoneClient(endpoint, authenticator)
            try {
                manageTemporaryUser(client, endpoint)
            } finally {
                client.close()
            }
        }
    }
}

private suspend fun manageTemporaryUser(client: LoxoneClient, endpoint: LoxoneEndpoint) {
    val group =
        client.callForMsg(LoxoneCommands.Users.groups()).items.firstOrNull {
            it.type == UserGroup.TYPE_NORMAL && !it.isAdmin
        }
    if (group == null) println("No normal group to create the user in: checking without one")
    val name = "loxkt-at-${Random.nextInt(0x100000, 0xFFFFFF).toString(16)}"
    val password = "At-${Random.nextLong(1L shl 40, Long.MAX_VALUE).toString(36)}"
    // 30 minutes stay in the future only if the Miniserver reads validUntil as UTC, as the Loxone app does;
    // a Miniserver in UTC or less than 30 minutes ahead of it can't tell the two apart.
    val validUntil = Clock.System.now() + 30.minutes

    val created = client.callForMsg(LoxoneCommands.Users.create(name, listOfNotNull(group?.uuid), validUntil, true))
    try {
        val hashing = client.callForMsg(Hashing.commandForUser(name))
        client.callForMsg(LoxoneCommands.Users.setPassword(created.uuid, password, hashing))
        val asCreated = KtorHttpLoxoneClient(endpoint, LoxoneAuth.Basic(name, password))
        try {
            asCreated.callRaw("jdev/sps/LoxAPPversion3") shouldContain "200"
        } finally {
            asCreated.close()
        }
        client.callForMsg(LoxoneCommands.Users.get(created.uuid)).asClue {
            it.name shouldBe name
            it.isTimeLimited shouldBe true
            it.validUntil shouldBe validUntil.epochSeconds - LoxoneTime.LOXONE_EPOCH_BEGIN
            it.expirationAction shouldBe MiniserverUser.EXPIRATION_DELETE
            group?.let { g -> it.usergroups.map { ug -> ug.uuid } shouldContain g.uuid }
        }
    } finally {
        // Not to hide the failure above; the list check below still catches a user left behind.
        runCatching { client.callForMsg(LoxoneCommands.Users.delete(created.uuid)) }
    }
    client.callForMsg(LoxoneCommands.Users.list()).items.map { it.uuid } shouldNotContain created.uuid
}

/**
 * Logging in users with MFA (firmware 17.3.11.23+) with no one to read a code, over a WebSocket as Doctor does: a
 * user without e-mail registers an authenticator app over the API (`jdev/sys/mfasetup`, as Config 17.3 does) and
 * logs in with its codes; a user at example.com gets a code nobody reads. Both are valid for 30 minutes and deleted
 * at the end. Needs a user with the user management right.
 */
fun mfaLoginAT(endpoint: LoxoneEndpoint, user: String, password: String) = wordSpec {
    val admin = LoxoneTokenAuthenticator(
        LoxoneProfile(endpoint, LoxoneCredentials(user, password)),
        InMemoryTokenRepository(),
        LoxoneClientSettings(tokenPermission = USER_TOKEN_PERMISSION)
    )
    "A user with MFA" should {
        "log in with a code from an authenticator app" {
            withTokenClient(endpoint, admin) { withMfaUser(it, endpoint, email = null) { p -> loginWithTotp(p) } }
        }
        "get a code by e-mail and be told to wait for another" {
            withTokenClient(endpoint, admin) {
                withMfaUser(it, endpoint, email = "loxkt-at@example.com") { profile ->
                    val authenticator = LoxoneTokenAuthenticator(profile, InMemoryTokenRepository())
                    authenticator.refusal(endpoint, LoginCode.EMAIL, null).loginCode.asClue { code ->
                        code.mail shouldBe "sent"
                        code.to.shouldNotBeNull() shouldContain "***"
                        code.resendIn.shouldNotBeNull() shouldBeGreaterThan 0
                    }
                    authenticator.onNewWebsocket(endpoint) { ws -> authenticator.resendLoginCode(ws) }.asClue { code ->
                        code.mail shouldBe "wait"
                        code.resendIn.shouldNotBeNull() shouldBeGreaterThan 0
                    }
                }
            }
        }
    }
}

/**
 * A user with a password and MFA switched on, with [email], for [block]. Before MFA, it logs in asked with a
 * method as it would without one.
 */
private suspend fun withMfaUser(
    admin: LoxoneClient,
    endpoint: LoxoneEndpoint,
    email: String?,
    block: suspend (LoxoneProfile) -> Unit
) {
    val group =
        admin.callForMsg(LoxoneCommands.Users.groups()).items.firstOrNull {
            it.type == UserGroup.TYPE_NORMAL && !it.isAdmin
        }
    val name = "loxkt-mfa-${Random.nextInt(0x100000, 0xFFFFFF).toString(16)}"
    val password = "At-${Random.nextLong(1L shl 40, Long.MAX_VALUE).toString(36)}"
    val created = admin.callForMsg(
        LoxoneCommands.Users.create(name, listOfNotNull(group?.uuid), Clock.System.now() + 30.minutes, true)
    )
    try {
        admin.callForMsg(
            LoxoneCommands.Users.setPassword(created.uuid, password, admin.callForMsg(Hashing.commandForUser(name)))
        )
        val profile = LoxoneProfile(endpoint, LoxoneCredentials(name, password))
        val tokens = InMemoryTokenRepository()
        val noAuth = KtorHttpLoxoneClient(endpoint)
        try {
            LoxoneTokenAuthenticator(profile, tokens).ensureAuthenticated(noAuth, LoginCode.TOTP, null)
        } finally {
            noAuth.close()
        }
        tokens.getToken(profile).shouldNotBeNull()

        val mfa = buildJsonObject {
            put("uuid", created.uuid)
            put("name", name)
            put("twoStepLogin", 1)
            if (email != null) put("email", email)
        }.toString()
        admin.callForMsg(SimpleLoxoneMsgCommand(listOf("jdev", "sps", "addoredituser", mfa), MiniserverUser::class))
        admin.callForMsg(LoxoneCommands.Users.get(created.uuid)).isTimeLimited shouldBe true
        block(profile)
    } finally {
        runCatching { admin.callForMsg(LoxoneCommands.Users.delete(created.uuid)) }
    }
}

@OptIn(ExperimentalEncodingApi::class)
private suspend fun loginWithTotp(profile: LoxoneProfile) {
    val credentials = requireNotNull(profile.credentials)
    val user = credentials.username
    val password = requireNotNull(credentials.password)
    val tokens = InMemoryTokenRepository()
    val authenticator = LoxoneTokenAuthenticator(profile, tokens)
    val http = KtorHttpLoxoneClient(profile.endpoint)
    try {
        // No e-mail and no app yet: no method a request without one can use.
        authenticator.refusal(profile.endpoint, null, null).code shouldBe "468"

        // Register the app as Config 17.3 does: a ticket from getjwt, the secret, a code confirming it.
        val hashing = http.callForMsg(Hashing.commandForUser(user))
        val offer = http.call(
            LoxoneCommands.Tokens.get(
                LoxoneCrypto.loxoneHashing(password, hashing, "at", user),
                user,
                TokenPermission.APP,
                method = LoginCode.TOTP
            )
        )
        offer.code shouldBe "460"
        val ticket = otpOf(offer.value).getValue("setupTicket").jsonPrimitive.content
        val nonce = Base64.UrlSafe.encode(Random.nextBytes(32)).trimEnd('=')
        val begin = Codec.loxJson.decodeFromString<LoxoneMsg>(http.callRaw("jdev/sys/mfasetup/totp/begin/$ticket/$nonce"))
        val secret = otpOf(begin.value).getValue("secret").jsonPrimitive.content
        val confirmKey = http.callForMsg(Hashing.commandForUser(user)).copy(hashAlg = "SHA256")
        val confirmed = http.callRaw(
            "jdev/sys/mfasetup/totp/confirm/$ticket/${LoxoneCrypto.loxoneHmac(totp(secret), confirmKey, "at")}"
        )
        Codec.loxJson.decodeFromString<LoxoneMsg>(confirmed).code shouldBe "200"

        authenticator.refusal(profile.endpoint, LoginCode.TOTP, null).asClue {
            it.code shouldBe "460"
            it.loginCode.methods shouldContain LoginCode.TOTP
            it.loginCode.default shouldBe LoginCode.TOTP
        }
        val valid = (-1..1).map { totp(secret, Clock.System.now() + (it * 30).seconds) }
        val wrong = (0..9).map { "00000$it" }.first { it !in valid }
        authenticator.refusal(profile.endpoint, LoginCode.TOTP, wrong).asClue {
            it.code shouldBe "463"
            it.loginCode.attemptsLeft.shouldNotBeNull()
        }
        // The next window's code: the one that confirmed the app is used up. Closing kills the token.
        authenticator.onNewWebsocket(profile.endpoint) { ws ->
            authenticator.ensureAuthenticated(ws, LoginCode.TOTP, totp(secret, Clock.System.now() + 30.seconds))
            tokens.getToken(profile).shouldNotBeNull()
            ws.callRaw("jdev/sps/LoxAPPversion3") shouldContain "200"
        }
    } finally {
        http.close()
    }
}

/** [block] on a new WebSocket: the Miniserver closes the one it refused a login on (firmware 17.3.11.23). */
private suspend fun <T> LoxoneTokenAuthenticator.onNewWebsocket(
    endpoint: LoxoneEndpoint,
    block: suspend (KtorWebsocketLoxoneClient) -> T
): T {
    val ws = KtorWebsocketLoxoneClient(endpoint, this)
    try {
        return block(ws)
    } finally {
        ws.close()
    }
}

/** How the Miniserver refuses a login asked with [method] and [code], over a new WebSocket. */
private suspend fun LoxoneTokenAuthenticator.refusal(endpoint: LoxoneEndpoint, method: String?, code: String?) =
    shouldThrow<LoxoneLoginCodeException> { onNewWebsocket(endpoint) { ensureAuthenticated(it, method, code) } }

/** The `otp` object of a Miniserver answer's [value], or the answer itself when there is none. */
private fun otpOf(value: String): JsonObject {
    val answer = Codec.loxJson.parseToJsonElement(unquoteJson(value)).jsonObject
    return answer["otp"] as? JsonObject ?: answer
}

/** RFC 6238 code of a base32 [secret]: SHA1, 6 digits, 30 s, as the Miniserver's authenticator apps make them. */
private fun totp(secret: String, at: Instant = Clock.System.now()): String {
    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    var buffer = 0
    var bits = 0
    val key = buildList {
        for (c in secret.uppercase().trimEnd('=')) {
            buffer = (buffer shl 5 or alphabet.indexOf(c)) and 0xFFFF
            bits += 5
            if (bits >= 8) {
                bits -= 8
                add((buffer shr bits).toByte())
            }
        }
    }.toByteArray()
    val counter = at.epochSeconds / 30
    val mac = HmacSHA1(key).doFinal(ByteArray(8) { (counter shr (56 - 8 * it)).toByte() })
    val offset = mac.last().toInt() and 0x0f
    val number = (0..3).fold(0) { acc, i -> acc shl 8 or (mac[offset + i].toInt() and 0xff) } and 0x7fffffff
    return (number % 1_000_000).toString().padStart(6, '0')
}

class LoxoneClientAT : WordSpec() {
    private val httpClient: KtorHttpLoxoneClient
    private val websocketClient: KtorWebsocketLoxoneClient

    override suspend fun afterSpec(spec: Spec) {
        super.afterSpec(spec)
        httpClient.close()
        websocketClient.close()
    }

    init {
        val address = getLoxEnv("ADDRESS")
        val user = getLoxEnv("USER")
        val password = getLoxEnv("PASS")

        val endpoint = LoxoneEndpoint.fromUrl(address)
        val authenticator = LoxoneTokenAuthenticator(LoxoneProfile(endpoint, LoxoneCredentials(user, password)))

        httpClient = KtorHttpLoxoneClient(endpoint, LoxoneAuth.Token(authenticator))
        websocketClient = KtorWebsocketLoxoneClient(endpoint, authenticator)

        include(commonAT(httpClient))
        include(commonAT(websocketClient))
        include(websocketAT(websocketClient, authenticator, user))
        include(tokenWithoutPasswordAT(endpoint, user, password))
        include(userManagementAT(endpoint, user, password))
        include(mfaLoginAT(endpoint, user, password))
    }

    private fun getLoxEnv(name: String) = "LOX_$name".let { requireNotNull(System.getenv(it)) { "Please set $it env" } }
}
