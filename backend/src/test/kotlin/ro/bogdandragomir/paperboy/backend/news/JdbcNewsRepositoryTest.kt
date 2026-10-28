package ro.bogdandragomir.paperboy.backend.news

import org.testcontainers.DockerClientFactory
import org.testcontainers.postgresql.PostgreSQLContainer
import ro.bogdandragomir.paperboy.backend.catalog.Feed
import ro.bogdandragomir.paperboy.backend.createDataSource
import ro.bogdandragomir.paperboy.backend.ingestion.ParsedArticle
import ro.bogdandragomir.paperboy.backend.migrate
import ro.bogdandragomir.paperboy.core.model.Source
import ro.bogdandragomir.paperboy.core.model.Topic
import java.time.Instant
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Runs the real SQL against PostgreSQL in Docker. */
class JdbcNewsRepositoryTest {
    private val now = Instant.parse("2026-10-28T08:00:00Z")

    @Test
    fun storesArticlesOnceAndFindsThemByTopic() {
        if (!dockerAvailable()) return

        PostgreSQLContainer("postgres:18-alpine").use { db ->
            db.start()
            createDataSource(db.jdbcUrl, db.username, db.password).use { dataSource ->
                migrate(dataSource)
                val repository = JdbcNewsRepository(dataSource)
                repository.upsertCatalog(
                    topics = listOf(Topic("tech", "Tech"), Topic("ai", "AI", parentId = "tech")),
                    sources = listOf(Source("src", "Source", "https://example.com")),
                    feeds = listOf(Feed("https://example.com/rss", "src", "tech")),
                )
                val article = ParsedArticle("https://example.com/a", "Title", "Summary", null, now)

                assertEquals(1, repository.saveArticles("src", "tech", listOf(article)))
                // The same article from another topic's feed is not stored twice, only tagged.
                assertEquals(0, repository.saveArticles("src", "ai", listOf(article)))

                val stories = repository.recentStories(setOf("ai"), now.minusSeconds(60), limit = 10)
                assertEquals(1, stories.size)
                assertEquals(listOf("ai", "tech"), stories.single().topicIds)
                assertEquals(listOf("ai", "tech"), repository.topics().map { it.id })

                assertEquals(1, repository.deleteArticlesPublishedBefore(now.plusSeconds(1)))
            }
        }
    }

    @Test
    fun twoFeedsSavingTheSameArticlesAtOnceNeitherFailNorDuplicate() {
        if (!dockerAvailable()) return

        PostgreSQLContainer("postgres:18-alpine").use { db ->
            db.start()
            createDataSource(db.jdbcUrl, db.username, db.password).use { dataSource ->
                migrate(dataSource)
                val repository = JdbcNewsRepository(dataSource)
                repository.upsertCatalog(
                    topics = listOf(Topic("world", "World"), Topic("business", "Business")),
                    sources = listOf(Source("src", "Source", "https://example.com")),
                    feeds = emptyList(),
                )
                val articles = (1..50).map { ParsedArticle("https://example.com/$it", "T$it", "", null, now) }

                // Opposite orders: before the fix this produced deadlocks and missing rows.
                val pool = Executors.newFixedThreadPool(2)
                val world = pool.submit<Int> { repository.saveArticles("src", "world", articles) }
                val business = pool.submit<Int> { repository.saveArticles("src", "business", articles.reversed()) }
                val inserted = world.get() + business.get()
                pool.shutdown()

                assertEquals(50, inserted)
                val stories = repository.recentStories(setOf("world"), now.minusSeconds(60), limit = 100)
                assertEquals(50, stories.size)
                assertTrue(stories.all { it.topicIds == listOf("business", "world") })
            }
        }
    }

    @Test
    fun searchMatchesWordsPrefixesSourcesAndTopics() {
        if (!dockerAvailable()) return

        PostgreSQLContainer("postgres:18-alpine").use { db ->
            db.start()
            createDataSource(db.jdbcUrl, db.username, db.password).use { dataSource ->
                migrate(dataSource)
                val repository = JdbcNewsRepository(dataSource)
                repository.upsertCatalog(
                    topics = listOf(Topic("tech", "Tech"), Topic("sport", "Sport")),
                    sources =
                        listOf(
                            Source("verge", "The Verge", "https://theverge.com"),
                            Source("bbc", "BBC", "https://bbc.com"),
                        ),
                    feeds = emptyList(),
                )
                repository.saveArticles(
                    "verge",
                    "tech",
                    listOf(
                        ParsedArticle(
                            "https://v.com/1",
                            "Apple unveils new headset",
                            "Vision Pro successor",
                            null,
                            now,
                        ),
                        ParsedArticle("https://v.com/2", "Chip shortage eases", "Apple and others benefit", null, now),
                    ),
                )
                repository.saveArticles(
                    "bbc",
                    "sport",
                    listOf(ParsedArticle("https://b.com/1", "Football final tonight", "Big match", null, now)),
                )

                // Headline matches rank above summary matches.
                assertEquals(
                    listOf("Apple unveils new headset", "Chip shortage eases"),
                    repository.search("apple", emptySet(), 10).map { it.headline },
                )
                // The last word may be unfinished while typing.
                assertEquals(
                    listOf("Football final tonight"),
                    repository.search("footb", emptySet(), 10).map { it.headline },
                )
                // Source and topic names match too.
                assertEquals(1, repository.search("bbc", emptySet(), 10).size)
                assertEquals(2, repository.search("tech", emptySet(), 10).size)
                // Topic filter.
                assertEquals(emptyList(), repository.search("apple", setOf("sport"), 10))
                // Punctuation can't break the query.
                assertEquals(emptyList(), repository.search("'); DROP TABLE articles; --", emptySet(), 10))
            }
        }
    }

    private fun dockerAvailable(): Boolean {
        val available = runCatching { DockerClientFactory.instance().isDockerAvailable }.getOrDefault(false)
        check(available || System.getProperty("paperboy.requireDocker") != "true") { "Docker is required in CI" }
        if (!available) println("Skipping JdbcNewsRepositoryTest: Docker isn't running")
        return available
    }
}
