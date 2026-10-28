package ro.bogdandragomir.paperboy.backend.ingestion

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess

fun interface FeedFetcher {
    /** Downloads a feed. Throws when the server can't be reached or answers with an error. */
    suspend fun fetch(url: String): ByteArray
}

class HttpFeedFetcher(
    private val client: HttpClient,
) : FeedFetcher {
    override suspend fun fetch(url: String): ByteArray {
        val response =
            client.get(url) {
                header(HttpHeaders.UserAgent, USER_AGENT)
                header(HttpHeaders.Accept, "application/rss+xml, application/atom+xml, application/xml, text/xml")
            }
        check(response.status.isSuccess()) { "HTTP ${response.status.value} for $url" }
        return response.readRawBytes()
    }

    private companion object {
        const val USER_AGENT = "PaperboyBot/0.1 (+https://github.com/bobodrg/Paperboy)"
    }
}
