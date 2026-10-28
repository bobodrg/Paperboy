package ro.bogdandragomir.paperboy.backend.news

import ro.bogdandragomir.paperboy.core.model.Brief
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class BriefService(
    private val repository: NewsRepository,
    private val ranker: BriefRanker = BriefRanker(),
    private val now: () -> Instant = Instant::now,
) {
    fun knownTopicIds(): Set<String> = repository.topics().mapTo(mutableSetOf()) { it.id }

    fun brief(
        topicIds: List<String>,
        limit: Int,
    ): Brief {
        val now = now()
        // Prefer the last day and a half; widen the window on quiet days so the brief isn't empty.
        val candidates =
            repository
                .recentStories(topicIds.toSet(), now - FRESH_WINDOW, CANDIDATE_LIMIT)
                .takeIf { it.size >= limit }
                ?: repository.recentStories(topicIds.toSet(), now - WIDE_WINDOW, CANDIDATE_LIMIT)

        return Brief(
            date = now.atOffset(ZoneOffset.UTC).toLocalDate().toString(),
            stories = ranker.rank(candidates, topicIds, limit, now),
        )
    }

    private companion object {
        val FRESH_WINDOW: Duration = Duration.ofHours(36)
        val WIDE_WINDOW: Duration = Duration.ofDays(7)
        const val CANDIDATE_LIMIT = 300
    }
}
