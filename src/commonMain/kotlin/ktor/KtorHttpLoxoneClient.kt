package cz.smarteon.loxone.ktor

import cz.smarteon.loxone.Codec
import cz.smarteon.loxone.Command
import cz.smarteon.loxone.HttpLoxoneClient
import cz.smarteon.loxone.LoxoneEndpoint
import cz.smarteon.loxone.LoxoneResponse
import cz.smarteon.loxone.LoxoneTokenAuthenticator
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlin.jvm.JvmOverloads

class KtorHttpLoxoneClient internal constructor(
    private val endpoint: LoxoneEndpoint,
    private val authenticator: LoxoneTokenAuthenticator? = null,
    httpClientEngine: HttpClientEngine? = null
) : HttpLoxoneClient {

    @JvmOverloads constructor(
        endpoint: LoxoneEndpoint,
        authenticator: LoxoneTokenAuthenticator? = null
    ) : this(endpoint, authenticator, null)

    private val logger = KotlinLogging.logger {}

    private val httpClient = httpClientEngine?.let { HttpClient(it) { configure() } } ?: HttpClient { configure() }

    private fun HttpClientConfig<*>.configure() {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Codec.loxJson)
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    this@KtorHttpLoxoneClient.logger.debug { message }
                }
            }
            level = LogLevel.ALL
        }
    }

    override suspend fun <RESPONSE : LoxoneResponse> call(command: Command<RESPONSE>): RESPONSE {
        if (command.authenticated) {
            authenticator?.ensureAuthenticated(this)
        }

        return httpClient.get {
            commandRequest(command.authenticated) {
                appendPathSegments(command.pathSegments)
            }
        }.body(command.responseType.typeInfo)
    }

    override suspend fun callRaw(command: String): String {
        authenticator?.ensureAuthenticated(this)
        return httpClient.get {
            commandRequest {
                appendEncodedPathSegments(command)
            }
        }.body()
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun close() {
        try {
            logger.debug { "Closing authenticator" }
            authenticator?.close(this@KtorHttpLoxoneClient)
        } catch (e: Exception) {
            logger.error(e) { "Error closing authenticator" }
        }
        logger.debug { "Closing http client" }
        httpClient.close()
    }

    private fun HttpRequestBuilder.commandRequest(addAuth: Boolean = true, pathBuilder: URLBuilder.() -> Unit) {
        url {
            protocol = if (endpoint.useSsl) URLProtocol.HTTPS else URLProtocol.HTTP
            host = endpoint.host
            appendEncodedPathSegments(endpoint.path)
            pathBuilder()
            if (addAuth && authenticator != null) {
                parameters.append("autht", authenticator.tokenHash("http-autht"))
                parameters.append("user", authenticator.user)
            }
        }
    }
}
