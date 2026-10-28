package ro.bogdandragomir.paperboy.backend.ingestion

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import ro.bogdandragomir.paperboy.backend.catalog.Feed
import ro.bogdandragomir.paperboy.backend.news.NewsRepository
import java.time.Duration
import java.time.Instant

data class IngestionReport(
    val feedsOk: Int,
    val feedsFailed: Int,
    val newArticles: Int,
    val deletedArticles: Int,
)

/** Reads every feed, stores new articles, and drops old ones. One broken feed never stops the others. */
class IngestionService(
    private val feeds: List<Feed>,
    private val fetcher: FeedFetcher,
    private val parser: FeedParser,
    private val repository: NewsRepository,
    private val now: () -> Instant = Instant::now,
) {
    private val log = LoggerFactory.getLogger(IngestionService::class.java)
    private val parallelFeeds = Semaphore(PARALLEL_FEEDS)

    suspend fun runOnce(): IngestionReport =
        coroutineScope {
            val results = feeds.map { feed -> async { parallelFeeds.withPermit { ingest(feed) } } }.awaitAll()
            val deleted = withContext(Dispatchers.IO) { repository.deleteArticlesPublishedBefore(now() - RETENTION) }
            IngestionReport(
                feedsOk = results.count { it != null },
                feedsFailed = results.count { it == null },
                newArticles = results.sumOf { it ?: 0 },
                deletedArticles = deleted,
            ).also { log.info("Ingestion finished: {}", it) }
        }

    /** Returns the number of new articles, or null when the feed failed. */
    private suspend fun ingest(feed: Feed): Int? =
        runCatching {
            val articles =
                parser
                    .parse(fetcher.fetch(feed.url))
                    .filter { it.publishedAt >= now() - RETENTION }
            withContext(Dispatchers.IO) { repository.saveArticles(feed.sourceId, feed.topicId, articles) }
        }.onFailure { log.warn("Feed failed: {} ({})", feed.url, it.message) }
            .getOrNull()

    private companion object {
        const val PARALLEL_FEEDS = 6
        val RETENTION: Duration = Duration.ofDays(7)
    }
}
