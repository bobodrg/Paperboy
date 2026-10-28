package ro.bogdandragomir.paperboy.backend.news

import ro.bogdandragomir.paperboy.backend.catalog.Feed
import ro.bogdandragomir.paperboy.backend.ingestion.ParsedArticle
import ro.bogdandragomir.paperboy.core.model.Source
import ro.bogdandragomir.paperboy.core.model.Story
import ro.bogdandragomir.paperboy.core.model.Topic
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import javax.sql.DataSource

/** PostgreSQL implementation with plain SQL. JDBC calls block: call these from Dispatchers.IO. */
class JdbcNewsRepository(
    private val dataSource: DataSource,
) : NewsRepository {
    override fun upsertCatalog(
        topics: List<Topic>,
        sources: List<Source>,
        feeds: List<Feed>,
    ) = transaction { connection ->
        connection
            .prepareStatement(
                """
                INSERT INTO topics (id, name, parent_id) VALUES (?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, parent_id = EXCLUDED.parent_id
                """.trimIndent(),
            ).use { statement ->
                // Parents first, so the foreign key on parent_id is satisfied.
                topics.sortedBy { it.parentId != null }.forEach { topic ->
                    statement.setString(1, topic.id)
                    statement.setString(2, topic.name)
                    statement.setString(3, topic.parentId)
                    statement.executeUpdate()
                }
            }
        connection
            .prepareStatement(
                """
                INSERT INTO sources (id, name, homepage_url) VALUES (?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, homepage_url = EXCLUDED.homepage_url
                """.trimIndent(),
            ).use { statement ->
                sources.forEach { source ->
                    statement.setString(1, source.id)
                    statement.setString(2, source.name)
                    statement.setString(3, source.homepageUrl)
                    statement.executeUpdate()
                }
            }
        connection
            .prepareStatement(
                """
                INSERT INTO feeds (url, source_id, topic_id) VALUES (?, ?, ?)
                ON CONFLICT (url) DO UPDATE SET source_id = EXCLUDED.source_id, topic_id = EXCLUDED.topic_id
                """.trimIndent(),
            ).use { statement ->
                feeds.forEach { feed ->
                    statement.setString(1, feed.url)
                    statement.setString(2, feed.sourceId)
                    statement.setString(3, feed.topicId)
                    statement.executeUpdate()
                }
            }
    }

    override fun topics(): List<Topic> =
        transaction { connection ->
            connection.prepareStatement("SELECT id, name, parent_id FROM topics ORDER BY name").use { statement ->
                statement.executeQuery().use { rows ->
                    buildList {
                        while (rows.next()) add(Topic(rows.getString(1), rows.getString(2), rows.getString(3)))
                    }
                }
            }
        }

    override fun saveArticles(
        sourceId: String,
        topicId: String,
        articles: List<ParsedArticle>,
    ): Int =
        transaction { connection ->
            // DO UPDATE (instead of DO NOTHING) makes RETURNING give back the row even when another feed
            // stored the same URL first, including one committed by a parallel transaction.
            // xmax = 0 is PostgreSQL's way of saying the row was inserted, not updated, by this statement.
            val upsertArticle =
                connection.prepareStatement(
                    """
                    INSERT INTO articles (url, title, summary, image_url, source_id, published_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT (url) DO UPDATE SET url = EXCLUDED.url
                    RETURNING id, (xmax = 0)
                    """.trimIndent(),
                )
            val linkTopic =
                connection.prepareStatement(
                    "INSERT INTO article_topics (article_id, topic_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                )
            var newArticles = 0
            upsertArticle.use {
                linkTopic.use {
                    // Same order in every transaction, so two feeds sharing articles can't deadlock.
                    articles.distinctBy { it.url }.sortedBy { it.url }.forEach { article ->
                        upsertArticle.setString(1, article.url)
                        upsertArticle.setString(2, article.title)
                        upsertArticle.setString(3, article.summary)
                        upsertArticle.setString(4, article.imageUrl)
                        upsertArticle.setString(5, sourceId)
                        upsertArticle.setTimestamp(6, Timestamp.from(article.publishedAt))
                        upsertArticle.executeQuery().use { rows ->
                            rows.next()
                            if (rows.getBoolean(2)) newArticles++
                            linkTopic.setLong(1, rows.getLong(1))
                            linkTopic.setString(2, topicId)
                            linkTopic.executeUpdate()
                        }
                    }
                }
            }
            newArticles
        }

    override fun recentStories(
        topicIds: Set<String>,
        since: Instant,
        limit: Int,
    ): List<Story> =
        transaction { connection ->
            connection
                .prepareStatement(
                    """
                    SELECT a.id, a.url, a.title, a.summary, a.image_url, a.published_at,
                           s.id, s.name, s.homepage_url,
                           array_agg(t.topic_id ORDER BY t.topic_id)
                    FROM articles a
                    JOIN sources s ON s.id = a.source_id
                    JOIN article_topics t ON t.article_id = a.id
                    WHERE a.published_at >= ?
                      AND a.id IN (SELECT article_id FROM article_topics WHERE topic_id = ANY (?))
                    GROUP BY a.id, s.id
                    ORDER BY a.published_at DESC
                    LIMIT ?
                    """.trimIndent(),
                ).use { statement ->
                    statement.setTimestamp(1, Timestamp.from(since))
                    statement.setArray(2, connection.createArrayOf("text", topicIds.toTypedArray()))
                    statement.setInt(3, limit)
                    statement.executeQuery().use { rows -> buildList { while (rows.next()) add(rows.toStory()) } }
                }
        }

    override fun search(
        query: String,
        topicIds: Set<String>,
        limit: Int,
    ): List<Story> {
        val tsQuery = SearchQuery.toTsQuery(query) ?: return emptyList()
        val like = "%${SearchQuery.escapeLike(query.trim())}%"
        return transaction { connection ->
            connection
                .prepareStatement(
                    """
                    WITH q AS (SELECT to_tsquery('english', ?) AS query)
                    SELECT a.id, a.url, a.title, a.summary, a.image_url, a.published_at,
                           s.id, s.name, s.homepage_url,
                           array_agg(t.topic_id ORDER BY t.topic_id),
                           ts_rank(a.search, q.query) AS rank
                    FROM articles a
                    CROSS JOIN q
                    JOIN sources s ON s.id = a.source_id
                    JOIN article_topics t ON t.article_id = a.id
                    WHERE (
                        a.search @@ q.query
                        OR s.name ILIKE ?
                        OR a.id IN (
                            SELECT at.article_id FROM article_topics at JOIN topics tp ON tp.id = at.topic_id
                            WHERE tp.name ILIKE ?
                        )
                    )
                    AND (cardinality(?::text[]) = 0
                         OR a.id IN (SELECT article_id FROM article_topics WHERE topic_id = ANY (?)))
                    GROUP BY a.id, s.id, q.query
                    ORDER BY rank DESC, a.published_at DESC
                    LIMIT ?
                    """.trimIndent(),
                ).use { statement ->
                    val topics = connection.createArrayOf("text", topicIds.toTypedArray())
                    statement.setString(1, tsQuery)
                    statement.setString(2, like)
                    statement.setString(3, like)
                    statement.setArray(4, topics)
                    statement.setArray(5, topics)
                    statement.setInt(6, limit)
                    statement.executeQuery().use { rows -> buildList { while (rows.next()) add(rows.toStory()) } }
                }
        }
    }

    override fun deleteArticlesPublishedBefore(cutoff: Instant): Int =
        transaction { connection ->
            connection.prepareStatement("DELETE FROM articles WHERE published_at < ?").use { statement ->
                statement.setTimestamp(1, Timestamp.from(cutoff))
                statement.executeUpdate()
            }
        }

    /** Maps the first ten columns shared by the brief and search queries. */
    private fun ResultSet.toStory(): Story {
        @Suppress("UNCHECKED_CAST")
        val topics = (getArray(10).array as Array<String>).toList()
        return Story(
            id = getLong(1).toString(),
            url = getString(2),
            headline = getString(3),
            summary = getString(4),
            imageUrl = getString(5),
            publishedAtEpochMillis = getTimestamp(6).time,
            source = Source(getString(7), getString(8), getString(9)),
            topicIds = topics,
        )
    }

    private fun <T> transaction(block: (Connection) -> T): T =
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                block(connection).also { connection.commit() }
            } catch (e: Exception) {
                connection.rollback()
                throw e
            }
        }
}
