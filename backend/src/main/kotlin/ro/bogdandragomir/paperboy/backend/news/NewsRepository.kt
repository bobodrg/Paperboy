package ro.bogdandragomir.paperboy.backend.news

import ro.bogdandragomir.paperboy.backend.catalog.Feed
import ro.bogdandragomir.paperboy.backend.ingestion.ParsedArticle
import ro.bogdandragomir.paperboy.core.model.Source
import ro.bogdandragomir.paperboy.core.model.Story
import ro.bogdandragomir.paperboy.core.model.Topic
import java.time.Instant

interface NewsRepository {
    fun upsertCatalog(
        topics: List<Topic>,
        sources: List<Source>,
        feeds: List<Feed>,
    )

    fun topics(): List<Topic>

    /** Stores new articles and links them to [topicId]. Returns how many articles were new. */
    fun saveArticles(
        sourceId: String,
        topicId: String,
        articles: List<ParsedArticle>,
    ): Int

    /** Stories published since [since] in any of [topicIds], newest first. */
    fun recentStories(
        topicIds: Set<String>,
        since: Instant,
        limit: Int,
    ): List<Story>

    fun deleteArticlesPublishedBefore(cutoff: Instant): Int
}
