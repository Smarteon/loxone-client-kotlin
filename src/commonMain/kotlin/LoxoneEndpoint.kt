package cz.smarteon.loxkt

import io.ktor.http.*
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * Loxone connection endpoint representation.
 *
 * @param host Loxone host name or IP address, without protocol prefix, port or path.
 * @param port Loxone port, default is 443 (HTTPS).
 * @param useSsl Whether to use SSL, default is true.
 * @param path Loxone path, url encoded, default is empty string.
 */
data class LoxoneEndpoint @JvmOverloads constructor(
    val host: String,
    val port: Int = HTTPS_PORT,
    val useSsl: Boolean = true,
    val path: String = ""
) {

    init {
        require(host.isNotBlank()) { "Host must not be blank" }
        require(!host.contains(":")) { "Host must not contain port or protocol" }
    }
    companion object {
        private const val HTTP_PORT = 80
        private const val HTTPS_PORT = 443

        /**
         * Creates Loxone endpoint by parsing URL.
         * @param url Loxone URL.
         */
        @JvmStatic
        fun fromUrl(url: String): LoxoneEndpoint {
            val parsed = URLBuilder().takeFrom(url)
            val port = when {
                parsed.port > 0 -> parsed.port
                parsed.protocol.isSecure() -> HTTPS_PORT
                else -> HTTP_PORT
            }
            return LoxoneEndpoint(parsed.host, port, parsed.protocol.isSecure(), parsed.encodedPath)
        }

        /**
         * Creates Loxone endpoint for local connection.
         *
         * @param address Loxone IP address.
         * @param port Loxone port, default is 80 (HTTP).
         * @param path Loxone path, url encoded, default is empty string.
         */
        @JvmStatic @JvmOverloads
        fun local(address: String, port: Int = HTTP_PORT, path: String = ""): LoxoneEndpoint {
            require(hostIsIp(address)) { "Local address must be IP" }
            return LoxoneEndpoint(address, port, false, path)
        }

        /**
         * Creates Loxone endpoint for public domain connection.
         *
         * @param domain Loxone host domain.
         * @param port Loxone port, default is 443 (HTTPS).
         * @param path Loxone path, url encoded, default is empty string.
         */
        @JvmStatic @JvmOverloads
        fun public(domain: String, port: Int = HTTPS_PORT, path: String = ""): LoxoneEndpoint {
            require(!hostIsIp(domain)) { "Public domain must not be IP" }
            return LoxoneEndpoint(domain, port, true, path)
        }
    }
}
