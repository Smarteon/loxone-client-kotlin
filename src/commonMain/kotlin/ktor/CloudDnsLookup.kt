package cz.smarteon.loxkt.ktor

import cz.smarteon.loxkt.LoxoneEndpoint
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.json.*

private val logger = KotlinLogging.logger {}

private val LOOKUP_TIMEOUT = 10.seconds

/** Tried in this order: `connect.loxonecloud.com` first, `dns.loxonecloud.com` as fallback. */
private val CLOUD_DNS_GATEWAYS = listOf("connect.loxonecloud.com", "dns.loxonecloud.com")

/**
 * Loxone's two Cloud DNS gateways answer a serial lookup (`/getip`) differently, and which one a
 * given Miniserver is registered with isn't knowable in advance:
 * - `connect.loxonecloud.com` gives a ready-made `{"url": "https://<host>:<port>"}`.
 * - `dns.loxonecloud.com` gives `{"DNS-Status": "registered", "IPHTTPS": "<ip-or-[ipv6]>:<port>"}`
 *   — a raw address that has to become `<dashed-ip>.<serial>.dyndns.loxonecloud.com` to match the
 *   certificate the Miniserver actually presents.
 *
 * Both are tried, in that order; null means neither resolved the serial, and the caller should
 * fall back to requesting `dns.loxonecloud.com/<serial>` directly and following its redirect.
 *
 * [customHeaders] goes out with the lookup request, e.g. if Cloud DNS requires something this
 * library has no opinion on.
 */
suspend fun resolveLoxoneCloudDns(
    serial: String,
    customHeaders: Map<String, String> = emptyMap(),
): LoxoneEndpoint? {
    val normalized = serial.replace(":", "").uppercase()
    HttpClient {
            installCustomHeaders(customHeaders)
            install(HttpTimeout) { requestTimeoutMillis = LOOKUP_TIMEOUT.inWholeMilliseconds }
            expectSuccess = false
        }
        .use { http ->
            for (gateway in CLOUD_DNS_GATEWAYS) {
                val endpoint =
                    runCatching { lookup(http, gateway, normalized) }
                        .onFailure { logger.debug(it) { "Cloud DNS lookup at $gateway failed" } }
                        .getOrNull()
                if (endpoint != null) {
                    logger.info {
                        "Resolved $normalized via $gateway -> ${endpoint.host}:${endpoint.port}"
                    }
                    return endpoint
                }
            }
        }
    return null
}

private suspend fun lookup(http: HttpClient, gateway: String, serial: String): LoxoneEndpoint? {
    val response =
        http.get("https://$gateway/getip?snr=$serial&json=true&rnd=${Random.nextInt()}")
    if (response.status != HttpStatusCode.OK) return null
    val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject

    json["url"]?.jsonPrimitive?.contentOrNull?.let {
        return LoxoneEndpoint.fromUrl(it)
    }

    val registered = json["DNS-Status"]?.jsonPrimitive?.contentOrNull == "registered"
    val ipHttps = json["IPHTTPS"]?.jsonPrimitive?.contentOrNull
    if (registered && ipHttps != null) {
        return LoxoneEndpoint.fromUrl(dyndnsUrl(ipHttps, serial))
    }
    return null
}

/** `"[2a01:4f8::1]:64988"` or `"1.2.3.4:443"` → the dyndns hostname a cert actually matches. */
private fun dyndnsUrl(ipHttps: String, serial: String): String {
    val (ip, port) =
        if (ipHttps.startsWith("[")) {
            val closing = ipHttps.indexOf("]:")
            ipHttps.substring(1, closing) to ipHttps.substring(closing + 2)
        } else {
            val idx = ipHttps.lastIndexOf(':')
            ipHttps.substring(0, idx) to ipHttps.substring(idx + 1)
        }
    val dashed = ip.replace(":", "-").replace(".", "-")
    return "https://$dashed.$serial.dyndns.loxonecloud.com:$port"
}
