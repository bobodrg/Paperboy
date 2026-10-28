package ro.bogdandragomir.paperboy.backend

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import ro.bogdandragomir.paperboy.core.model.Brief
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun healthReturnsOk() =
        testApplication {
            application { module() }

            val response = jsonClient().get("/health")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(Health("ok"), response.body())
        }

    @Test
    fun briefEndpointReturnsABrief() =
        testApplication {
            application { module() }

            val response = jsonClient().get("/v1/brief")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(emptyList(), response.body<Brief>().stories)
        }

    private fun ApplicationTestBuilder.jsonClient() = createClient { install(ContentNegotiation) { json() } }
}
