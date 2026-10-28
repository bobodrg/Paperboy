package ro.bogdandragomir.paperboy.backend

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import ro.bogdandragomir.paperboy.backend.news.BriefService
import ro.bogdandragomir.paperboy.backend.news.FakeNewsRepository
import ro.bogdandragomir.paperboy.core.model.Brief
import ro.bogdandragomir.paperboy.core.model.Source
import ro.bogdandragomir.paperboy.core.model.Story
import ro.bogdandragomir.paperboy.core.model.Topic
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    private val now = Instant.parse("2026-10-28T08:00:00Z")
    private val repository =
        FakeNewsRepository(
            topics = listOf(Topic("tech", "Tech"), Topic("sport", "Sport")),
            stories =
                listOf(
                    Story(
                        id = "1",
                        headline = "A tech story",
                        summary = "",
                        url = "https://example.com/1",
                        source = Source("src", "Source", "https://example.com"),
                        topicIds = listOf("tech"),
                        publishedAtEpochMillis = now.minusSeconds(3600).toEpochMilli(),
                    ),
                ),
        )

    @Test
    fun healthReturnsOk() =
        testApp {
            val response = client.get("/health")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(Health("ok"), response.body())
        }

    @Test
    fun listsTopics() =
        testApp {
            assertEquals(listOf("tech", "sport"), client.get("/v1/topics").body<List<Topic>>().map { it.id })
        }

    @Test
    fun returnsABriefForTheRequestedTopics() =
        testApp {
            val response = client.get("/v1/brief?topics=tech&limit=5")

            assertEquals(HttpStatusCode.OK, response.status)
            val brief = response.body<Brief>()
            assertEquals("2026-10-28", brief.date)
            assertEquals(listOf("1"), brief.stories.map { it.id })
        }

    @Test
    fun rejectsMissingUnknownOrTooManyTopics() =
        testApp {
            assertEquals(HttpStatusCode.BadRequest, client.get("/v1/brief").status)
            assertEquals(HttpStatusCode.BadRequest, client.get("/v1/brief?topics=knitting").status)
            assertEquals(HttpStatusCode.BadRequest, client.get("/v1/brief?topics=tech&limit=500").status)
        }

    @Test
    fun searchFindsStoriesAndValidatesTheQuery() =
        testApp {
            assertEquals(listOf("1"), client.get("/v1/search?q=tech").body<List<Story>>().map { it.id })
            assertEquals(emptyList(), client.get("/v1/search?q=tech&topics=sport").body<List<Story>>())
            assertEquals(HttpStatusCode.BadRequest, client.get("/v1/search?q=").status)
            assertEquals(HttpStatusCode.BadRequest, client.get("/v1/search?q=" + "x".repeat(101)).status)
            assertEquals(HttpStatusCode.BadRequest, client.get("/v1/search?q=tech&limit=99").status)
        }

    private fun testApp(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application { module(repository, BriefService(repository) { now }) }
            client = createClient { install(ContentNegotiation) { json() } }
            block()
        }
}
