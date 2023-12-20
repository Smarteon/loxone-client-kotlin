package cz.smarteon.loxone.ktor

import cz.smarteon.loxone.Codec
import cz.smarteon.loxone.Command
import cz.smarteon.loxone.LoxoneClient
import cz.smarteon.loxone.LoxoneProfile
import cz.smarteon.loxone.LoxoneResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*

class HttpLoxoneClient(
    override val profile: LoxoneProfile
) : LoxoneClient {


    private val httpClient = HttpClient(clientEngineFactory) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Codec.loxJson)
        }

    }

    override suspend fun <RESPONSE : LoxoneResponse> call(command: Command<RESPONSE>): RESPONSE = httpClient.get {
        commandReuqest {
            pathSegments = command.pathSegments
        }
    }.body(command.responseType.typeInfo)


    override suspend fun callRaw(command: String): String = httpClient.get {
        commandReuqest {
            encodedPath = command
        }
    }.body()

    override fun close() {
        httpClient.close()
    }

    private fun HttpRequestBuilder.commandReuqest(pathBuilder: URLBuilder.() -> Unit) {
        url {
            protocol = if (profile.endpoint.useSsl) URLProtocol.HTTPS else URLProtocol.HTTP
            host = profile.endpoint.host
            pathBuilder()
        }
        profile.credentials?.let {
            basicAuth(it.username, it.password)
        }
    }
}

internal expect val clientEngineFactory: HttpClientEngineFactory<*>
