package ro.bogdandragomir.paperboy.backend

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import ro.bogdandragomir.paperboy.core.model.Brief

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module).start(wait = true)
}

@Serializable
data class Health(
    val status: String,
)

@Serializable
data class ApiError(
    val message: String,
)

fun Application.module() {
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

        route("/v1") {
            // Placeholder until ingestion and ranking exist (roadmap phase 2).
            get("/brief") { call.respond(Brief(date = "2026-10-28", stories = emptyList())) }
        }
    }
}
