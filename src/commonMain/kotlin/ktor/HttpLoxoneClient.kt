package cz.smarteon.loxone.ktor

import cz.smarteon.loxone.Codec
import cz.smarteon.loxone.Command
import cz.smarteon.loxone.LoxoneClient
import cz.smarteon.loxone.LoxoneEndpoint
import cz.smarteon.loxone.LoxoneResponse
import cz.smarteon.loxone.LoxoneTokenAuthenticator
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlin.jvm.JvmOverloads

class HttpLoxoneClient @JvmOverloads constructor(
    private val endpoint: LoxoneEndpoint,
    private val authenticator: LoxoneTokenAuthenticator? = null
) : LoxoneClient {

    private val httpClient = HttpClient {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Codec.loxJson)
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

    override suspend fun close() {
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
