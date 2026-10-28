package ro.bogdandragomir.paperboy.story

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ro.bogdandragomir.paperboy.brief.BriefRepository
import ro.bogdandragomir.paperboy.core.model.Story
import ro.bogdandragomir.paperboy.data.local.SavedRepository

/** Stories shown outside the brief (e.g. search results), kept in memory so they can be opened. */
class RecentStories {
    private val stories = MutableStateFlow<Map<String, Story>>(emptyMap())

    fun remember(shown: List<Story>) = stories.update { it + shown.associateBy(Story::id) }

    fun observe(id: String): Flow<Story?> = stories.map { it[id] }
}

/** Finds a story wherever the reader saw it: today's brief, their saved stories, or recent search results. */
class StoryLookup(
    private val brief: BriefRepository,
    private val saved: SavedRepository,
    private val recent: RecentStories,
) {
    fun observe(id: String): Flow<Story?> =
        combine(
            brief.observeStory(id),
            saved.observeStory(id),
            recent.observe(id),
        ) { fromBrief, fromSaved, fromRecent ->
            fromBrief ?: fromSaved ?: fromRecent
        }
}
