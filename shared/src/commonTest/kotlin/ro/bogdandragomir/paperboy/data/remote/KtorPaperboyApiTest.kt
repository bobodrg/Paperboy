package ro.bogdandragomir.paperboy.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KtorPaperboyApiTest {
    private fun api(
        status: HttpStatusCode,
        body: String,
        onRequest: (String) -> Unit = {},
    ): KtorPaperboyApi {
        val engine =
            MockEngine { request ->
                onRequest(request.url.toString())
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        val client =
            HttpClient(engine) {
                expectSuccess = true
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            }
        return KtorPaperboyApi(client, ApiConfig("https://api.example.com"))
    }

    @Test
    fun requestsTheBriefForTheTopicsAndParsesIt() =
        runTest {
            var url = ""
            val json =
                """
                {"date":"2026-10-28","stories":[{"id":"1","headline":"H","summary":"S","url":"https://x.com/1",
                "source":{"id":"bbc","name":"BBC","homepageUrl":"https://bbc.com"},"topicIds":["tech"],
                "publishedAtEpochMillis":5,"sourceCount":1,"aFieldFromANewerServer":true}]}
                """.trimIndent()

            val brief = api(HttpStatusCode.OK, json) { url = it }.brief(listOf("tech", "ai"), 10)

            assertEquals("https://api.example.com/v1/brief?topics=tech%2Cai&limit=10", url)
            assertEquals("H", brief.stories.single().headline)
        }

    @Test
    fun requestsTheTopics() =
        runTest {
            var url = ""
            val json = """[{"id":"tech","name":"Tech","parentId":null},{"id":"ai","name":"AI","parentId":"tech"}]"""

            val topics = api(HttpStatusCode.OK, json) { url = it }.topics()

            assertEquals("https://api.example.com/v1/topics", url)
            assertEquals("tech", topics[1].parentId)
        }

    @Test
    fun serverErrorsBecomeExceptions() =
        runTest {
            assertFailsWith<Exception> {
                api(HttpStatusCode.InternalServerError, """{"message":"x"}""").brief(listOf("tech"), 10)
            }
        }
}
