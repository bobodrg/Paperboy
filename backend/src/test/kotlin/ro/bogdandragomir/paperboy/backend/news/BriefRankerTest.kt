package ro.bogdandragomir.paperboy.backend.news

import ro.bogdandragomir.paperboy.core.model.Source
import ro.bogdandragomir.paperboy.core.model.Story
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BriefRankerTest {
    private val now = Instant.parse("2026-10-28T08:00:00Z")
    private val ranker = BriefRanker(maxPerSource = 2)

    @Test
    fun coversEveryFollowedTopicBeforeFillingByScore() {
        val candidates =
            (1..5).map { story("tech-$it", "src-$it", listOf("tech"), hoursAgo = 1) } +
                story("sport-old", "src-x", listOf("sport"), hoursAgo = 30)

        val brief = ranker.rank(candidates, listOf("tech", "sport"), limit = 3, now = now)

        assertTrue(brief.any { it.id == "sport-old" }, "the older sport story still makes the brief")
        assertEquals(3, brief.size)
    }

    @Test
    fun capsStoriesPerSource() {
        val candidates = (1..5).map { story("bbc-$it", "bbc", listOf("world"), hoursAgo = it.toLong()) }

        val brief = ranker.rank(candidates, listOf("world"), limit = 5, now = now)

        assertEquals(listOf("bbc-1", "bbc-2"), brief.map { it.id })
    }

    @Test
    fun prefersStoriesMatchingMoreFollowedTopics() {
        val single = story("single", "a", listOf("tech"), hoursAgo = 0)
        val double = story("double", "b", listOf("tech", "ai"), hoursAgo = 6)

        val brief = ranker.rank(listOf(single, double), listOf("tech", "ai"), limit = 2, now = now)

        assertEquals("double", brief.first().id)
    }

    private fun story(
        id: String,
        sourceId: String,
        topics: List<String>,
        hoursAgo: Long,
    ) = Story(
        id = id,
        headline = id,
        summary = "",
        url = "https://example.com/$id",
        source = Source(sourceId, sourceId, "https://example.com"),
        topicIds = topics,
        publishedAtEpochMillis = (now - Duration.ofHours(hoursAgo)).toEpochMilli(),
    )
}
