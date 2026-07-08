package cz.smarteon.loxkt.js

import cz.smarteon.loxkt.LoxoneAuth
import cz.smarteon.loxkt.LoxoneCredentials
import cz.smarteon.loxkt.LoxoneEndpoint
import cz.smarteon.loxkt.LoxoneProfile
import cz.smarteon.loxkt.LoxoneTokenAuthenticator
import cz.smarteon.loxkt.callForMsg
import cz.smarteon.loxkt.ktor.KtorHttpLoxoneClient
import cz.smarteon.loxkt.ktor.KtorWebsocketLoxoneClient
import cz.smarteon.loxkt.message.ApiInfo
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldMatch

/**
 * JS acceptance test against a **real Miniserver** — mirrors the JVM
 * [cz.smarteon.loxkt.LoxoneClientAT], flattened for the JS runner and reading credentials from
 * `process.env`. Gated on `LOX_ADDRESS` / `LOX_USER` / `LOX_PASS`; registers no tests when unset,
 * so `check` stays green offline.
 *
 * ```
 * LOX_ADDRESS=https://your-miniserver LOX_USER=user LOX_PASS=pass ./gradlew jsBrowserTest
 * ```
 * Browser env injection and the CORS/TLS setup are covered in docs/js-interop-design.md.
 */
class LoxoneClientAcceptanceJsTest : StringSpec({

    val address = loxEnv(loxAddressEnv)
    val user = loxEnv(loxUserEnv)
    val password = loxEnv(loxPassEnv)

    if (address != null && user != null && password != null) {
        val endpoint = LoxoneEndpoint.fromUrl(address)
        val authenticator = LoxoneTokenAuthenticator(LoxoneProfile(endpoint, LoxoneCredentials(user, password)))
        val http = KtorHttpLoxoneClient(endpoint, LoxoneAuth.Token(authenticator))
        val ws = KtorWebsocketLoxoneClient(endpoint, authenticator)

        afterSpec {
            http.close()
            ws.close()
        }

        "http callRaw jdev/cfg/api returns 200" {
            http.callRaw("jdev/cfg/api") shouldContain "200"
        }

        "http call ApiInfo returns code 200" {
            http.call(ApiInfo.command).code shouldBe "200"
        }

        // Exercises the full WebSocket connect path — public-key fetch, command-encryption key
        // exchange (node-forge RSA) and token authentication — which is exactly what failed under
        // ES modules before the crypto fix.
        "ws callForMsg ApiInfo returns a version" {
            ws.callForMsg(ApiInfo.command).version shouldMatch ".*\\d+\\.\\d+\\.\\d+.*"
        }
    }
})

// Each `process.env.<NAME>` read is a static token so webpack DefinePlugin can replace it at build
// time for the browser bundle; `|| null` collapses `undefined` (Node, var unset) to a real null.
private val loxAddressEnv: String? get() = js("process.env.LOX_ADDRESS || null").unsafeCast<String?>()
private val loxUserEnv: String? get() = js("process.env.LOX_USER || null").unsafeCast<String?>()
private val loxPassEnv: String? get() = js("process.env.LOX_PASS || null").unsafeCast<String?>()

private fun loxEnv(value: String?): String? = value?.takeIf { it.isNotEmpty() }
