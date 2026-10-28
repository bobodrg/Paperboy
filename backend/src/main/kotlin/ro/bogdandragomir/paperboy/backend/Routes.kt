package ro.bogdandragomir.paperboy.backend

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ro.bogdandragomir.paperboy.backend.news.BriefService
import ro.bogdandragomir.paperboy.backend.news.NewsRepository

private const val DEFAULT_BRIEF_SIZE = 10
private const val MAX_BRIEF_SIZE = 20
private const val MAX_TOPICS = 20
private const val DEFAULT_SEARCH_SIZE = 20
private const val MAX_SEARCH_SIZE = 30
private const val MAX_QUERY_LENGTH = 100

fun Route.newsRoutes(
    repository: NewsRepository,
    briefService: BriefService,
) {
    route("/v1") {
        get("/topics") {
            call.respond(withContext(Dispatchers.IO) { repository.topics() })
        }

        // GET /v1/brief?topics=tech,ai&limit=10
        get("/brief") {
            val topics =
                call.request.queryParameters["topics"]
                    ?.split(',')
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.distinct()
                    .orEmpty()
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: DEFAULT_BRIEF_SIZE

            if (topics.isEmpty() || topics.size > MAX_TOPICS) {
                return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiError("Pass 1–$MAX_TOPICS topics, e.g. ?topics=tech,ai"),
                )
            }
            if (limit !in 1..MAX_BRIEF_SIZE) {
                return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiError("limit must be between 1 and $MAX_BRIEF_SIZE"),
                )
            }

            val brief =
                withContext(Dispatchers.IO) {
                    val unknown = topics - briefService.knownTopicIds()
                    if (unknown.isNotEmpty()) null else briefService.brief(topics, limit)
                }
            if (brief == null) {
                call.respond(HttpStatusCode.BadRequest, ApiError("Unknown topics"))
            } else {
                call.respond(brief)
            }
        }

        // GET /v1/search?q=apple%20vision&topics=tech&limit=20
        get("/search") {
            val query =
                call.request.queryParameters["q"]
                    ?.trim()
                    .orEmpty()
            val topics =
                call.request.queryParameters["topics"]
                    ?.split(',')
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.toSet()
                    .orEmpty()
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: DEFAULT_SEARCH_SIZE

            if (query.length !in 1..MAX_QUERY_LENGTH) {
                return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiError("q must be 1–$MAX_QUERY_LENGTH characters"),
                )
            }
            if (limit !in 1..MAX_SEARCH_SIZE || topics.size > MAX_TOPICS) {
                return@get call.respond(HttpStatusCode.BadRequest, ApiError("limit must be 1–$MAX_SEARCH_SIZE"))
            }
            call.respond(withContext(Dispatchers.IO) { repository.search(query, topics, limit) })
        }
    }
}
