package ro.bogdandragomir.paperboy.backend

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import ro.bogdandragomir.paperboy.backend.catalog.Catalog
import ro.bogdandragomir.paperboy.backend.ingestion.FeedParser
import ro.bogdandragomir.paperboy.backend.ingestion.HttpFeedFetcher
import ro.bogdandragomir.paperboy.backend.ingestion.IngestionService
import ro.bogdandragomir.paperboy.backend.news.BriefService
import ro.bogdandragomir.paperboy.backend.news.JdbcNewsRepository
import ro.bogdandragomir.paperboy.backend.news.NewsRepository

fun main() {
    val config = AppConfig.fromEnv()
    embeddedServer(Netty, port = config.port, host = "0.0.0.0") { production(config) }.start(wait = true)
}

@Serializable
data class Health(
    val status: String,
)

@Serializable
data class ApiError(
    val message: String,
)

/** Wires the real database, feeds and background ingestion. */
fun Application.production(config: AppConfig) {
    val dataSource = createDataSource(config.databaseUrl, config.databaseUser, config.databasePassword)
    migrate(dataSource)
    val repository = JdbcNewsRepository(dataSource)
    repository.upsertCatalog(Catalog.topics, Catalog.sources, Catalog.feeds)

    val httpClient =
        HttpClient(CIO) {
            install(HttpTimeout) { requestTimeoutMillis = 15_000 }
            followRedirects = true
        }
    val ingestion = IngestionService(Catalog.feeds, HttpFeedFetcher(httpClient), FeedParser(), repository)

    if (config.ingestionEnabled) {
        monitor.subscribe(ApplicationStarted) {
            launch(Dispatchers.Default) {
                while (isActive) {
                    runCatching { ingestion.runOnce() }.onFailure { log.error("Ingestion run failed", it) }
                    delay(config.ingestionInterval.toMillis())
                }
            }
        }
    }
    monitor.subscribe(ApplicationStopped) {
        httpClient.close()
        dataSource.close()
    }

    module(repository, BriefService(repository))
}

/** HTTP layer only, so tests can pass in fakes. */
fun Application.module(
    repository: NewsRepository,
    briefService: BriefService,
) {
    install(ContentNegotiation) { json() }
    install(CallLogging)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled error", cause)
            call.respond(HttpStatusCode.InternalServerError, ApiError("Something went wrong"))
        }
    }

    routing {
        get("/health") { call.respond(Health("ok")) }
        newsRoutes(repository, briefService)
    }
}
