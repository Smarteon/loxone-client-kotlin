package cz.smarteon.loxone.ktor

import cz.smarteon.loxone.Codec
import cz.smarteon.loxone.Command
import cz.smarteon.loxone.HttpLoxoneClient
import cz.smarteon.loxone.LoxoneAuth
import cz.smarteon.loxone.LoxoneEndpoint
import cz.smarteon.loxone.LoxoneResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlin.jvm.JvmOverloads

class KtorHttpLoxoneClient internal constructor(
    private val endpoint: LoxoneEndpoint,
    private val authentication: LoxoneAuth = LoxoneAuth.None,
    httpClientEngine: HttpClientEngine? = null
) : HttpLoxoneClient {

    @JvmOverloads constructor(
        endpoint: LoxoneEndpoint,
        authentication: LoxoneAuth = LoxoneAuth.None,
    ) : this(endpoint, authentication, null)

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
            authentication.tokenAuthenticator?.ensureAuthenticated(this)
        }

        return httpClient.get {
            commandRequest(command.authenticated) {
                appendPathSegments(command.pathSegments)
            }
        }.body(command.responseType.typeInfo)
    }

    override suspend fun callRaw(command: String): String = callRawForResponse(command).body()

    override suspend fun callRawForData(command: String): ByteArray = callRawForResponse(command).body()

    private suspend fun callRawForResponse(command: String): HttpResponse {
        authentication.tokenAuthenticator?.ensureAuthenticated(this)
        return httpClient.get {
            commandRequest {
                appendEncodedPathSegments(command)
            }
        }
    }

    override suspend fun postRaw(command: String, payload: ByteArray): String {
        authentication.tokenAuthenticator?.ensureAuthenticated(this)
        return httpClient.post {
            commandRequest {
                appendPathSegments(command)
            }
            setBody(payload)
        }.bodyAsText()
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun close() {
        try {
            logger.debug { "Closing authenticator" }
            authentication.tokenAuthenticator?.close(this@KtorHttpLoxoneClient)
        } catch (e: Exception) {
            logger.error(e) { "Error closing authenticator" }
        }
        logger.debug { "Closing http client" }
        httpClient.close()
    }

    private fun HttpRequestBuilder.commandRequest(addAuth: Boolean = true, pathBuilder: URLBuilder.() -> Unit) {
        if (addAuth && authentication is LoxoneAuth.Basic) {
            basicAuth(authentication.username, authentication.password)
        }
        url {
            protocol = if (endpoint.useSsl) URLProtocol.HTTPS else URLProtocol.HTTP
            host = endpoint.host
            appendEncodedPathSegments(endpoint.path)
            pathBuilder()
            if (addAuth && authentication is LoxoneAuth.Token) {
                val authenticator = authentication.authenticator
                parameters.append("autht", authenticator.tokenHash("http-autht"))
                parameters.append("user", authenticator.user)
            }
        }
    }
}
