package ro.bogdandragomir.paperboy.backend.ingestion

import kotlinx.coroutines.test.runTest
import ro.bogdandragomir.paperboy.backend.catalog.Feed
import ro.bogdandragomir.paperboy.backend.news.FakeNewsRepository
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class IngestionServiceTest {
    private val now = Instant.parse("2026-10-28T10:00:00Z")
    private val fixture = requireNotNull(javaClass.getResourceAsStream("/feeds/sample-rss.xml")).readBytes()

    @Test
    fun oneBrokenFeedDoesNotStopTheOthers() =
        runTest {
            val repository = FakeNewsRepository()
            val fetcher =
                FeedFetcher { url ->
                    if (url.contains("broken")) error("HTTP 500") else fixture
                }
            val service =
                IngestionService(
                    feeds =
                        listOf(
                            Feed("https://good.example/rss", "good", "tech"),
                            Feed("https://broken.example/rss", "broken", "tech"),
                        ),
                    fetcher = fetcher,
                    parser = FeedParser { now },
                    repository = repository,
                    now = { now },
                )

            val report = service.runOnce()

            assertEquals(1, report.feedsOk)
            assertEquals(1, report.feedsFailed)
            assertEquals(3, report.newArticles)
        }
}
