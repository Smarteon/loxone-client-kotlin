package cz.smarteon.loxkt.ktor

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*

/**
 * Loxone Cloud DNS (and possibly a Miniserver itself) can start requiring request headers this
 * library has no opinion on — what counts as a legitimate client is Loxone's call, not this
 * library's. [customHeaders] lets a caller supply whatever headers its situation needs (e.g. to
 * look like a particular client) without this library hardcoding any one app's identity.
 */
internal fun HttpClientConfig<*>.installCustomHeaders(customHeaders: Map<String, String>) {
    if (customHeaders.isEmpty()) return
    defaultRequest { customHeaders.forEach { (name, value) -> header(name, value) } }
}
