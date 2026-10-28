package ro.bogdandragomir.paperboy.backend.news

import ro.bogdandragomir.paperboy.backend.catalog.Feed
import ro.bogdandragomir.paperboy.backend.ingestion.ParsedArticle
import ro.bogdandragomir.paperboy.core.model.Source
import ro.bogdandragomir.paperboy.core.model.Story
import ro.bogdandragomir.paperboy.core.model.Topic
import java.time.Instant

/** In-memory stand-in for the database. */
class FakeNewsRepository(
    private val topics: List<Topic> = emptyList(),
    stories: List<Story> = emptyList(),
) : NewsRepository {
    val stories = stories.toMutableList()
    val saved = mutableListOf<Pair<String, ParsedArticle>>()

    override fun upsertCatalog(
        topics: List<Topic>,
        sources: List<Source>,
        feeds: List<Feed>,
    ) = Unit

    override fun topics() = topics

    override fun saveArticles(
        sourceId: String,
        topicId: String,
        articles: List<ParsedArticle>,
    ): Int {
        val new = articles.filter { article -> saved.none { it.second.url == article.url } }
        new.forEach { saved += topicId to it }
        return new.size
    }

    override fun recentStories(
        topicIds: Set<String>,
        since: Instant,
        limit: Int,
    ): List<Story> {
        val sinceMillis = since.toEpochMilli()
        return stories
            .filter { story -> story.publishedAtEpochMillis >= sinceMillis && story.topicIds.any { it in topicIds } }
            .sortedByDescending { it.publishedAtEpochMillis }
            .take(limit)
    }

    override fun deleteArticlesPublishedBefore(cutoff: Instant) = 0
}
