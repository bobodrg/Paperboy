package ro.bogdandragomir.paperboy.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ro.bogdandragomir.paperboy.core.model.Brief
import ro.bogdandragomir.paperboy.core.model.Topic

/** Where the Paperboy API lives. Each platform provides it, see `platformModule`. */
data class ApiConfig(
    val baseUrl: String,
)

interface PaperboyApi {
    suspend fun topics(): List<Topic>

    suspend fun brief(
        topicIds: List<String>,
        limit: Int,
    ): Brief
}

class KtorPaperboyApi(
    private val client: HttpClient,
    private val config: ApiConfig,
) : PaperboyApi {
    override suspend fun topics(): List<Topic> = client.get("${config.baseUrl}/v1/topics").body()

    override suspend fun brief(
        topicIds: List<String>,
        limit: Int,
    ): Brief =
        client
            .get("${config.baseUrl}/v1/brief") {
                parameter("topics", topicIds.joinToString(","))
                parameter("limit", limit)
            }.body()
}

/** The engine comes from the platform: OkHttp on Android, Darwin (NSURLSession) on iOS. */
fun createHttpClient(): HttpClient =
    HttpClient {
        expectSuccess = true
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 20_000
        }
    }
