package ro.bogdandragomir.paperboy.backend.news

import ro.bogdandragomir.paperboy.core.model.Story
import java.time.Duration
import java.time.Instant
import kotlin.math.exp

/**
 * Picks the stories for a brief. Rules, in order:
 * 1. every followed topic gets its best story first, so the brief covers all of them;
 * 2. the remaining places go to the highest scores;
 * 3. at most [maxPerSource] stories from one outlet.
 *
 * Score = number of followed topics the story matches + a recency bonus that halves roughly every 8 hours.
 */
class BriefRanker(
    private val maxPerSource: Int = 2,
) {
    fun rank(
        candidates: List<Story>,
        followedTopics: List<String>,
        limit: Int,
        now: Instant,
    ): List<Story> {
        val scored = candidates.sortedByDescending { score(it, followedTopics, now) }
        val picked = LinkedHashSet<Story>()
        val perSource = mutableMapOf<String, Int>()

        fun tryPick(story: Story): Boolean {
            if (story in picked || picked.size >= limit) return false
            if ((perSource[story.source.id] ?: 0) >= maxPerSource) return false
            picked += story
            perSource.merge(story.source.id, 1, Int::plus)
            return true
        }

        followedTopics.forEach { topic -> scored.firstOrNull { topic in it.topicIds && tryPick(it) } }
        scored.forEach { tryPick(it) }

        return picked.sortedByDescending { score(it, followedTopics, now) }
    }

    fun score(
        story: Story,
        followedTopics: List<String>,
        now: Instant,
    ): Double {
        val matches = story.topicIds.count { it in followedTopics }
        val ageHours = Duration.between(Instant.ofEpochMilli(story.publishedAtEpochMillis), now).toMinutes() / 60.0
        return matches + exp(-ageHours.coerceAtLeast(0.0) / 12.0)
    }
}
