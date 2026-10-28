package ro.bogdandragomir.paperboy.brief

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ro.bogdandragomir.paperboy.core.model.Brief
import ro.bogdandragomir.paperboy.core.model.Source
import ro.bogdandragomir.paperboy.core.model.Story
import ro.bogdandragomir.paperboy.core.model.Topic
import ro.bogdandragomir.paperboy.data.local.BriefLocalDataSource
import ro.bogdandragomir.paperboy.data.local.CachedBrief
import ro.bogdandragomir.paperboy.data.local.InterestsRepository
import ro.bogdandragomir.paperboy.data.local.SavedRepository
import ro.bogdandragomir.paperboy.data.remote.PaperboyApi

class FakePaperboyApi(
    var topics: () -> List<Topic> = { emptyList() },
    var response: () -> Brief = { Brief("2026-10-28", emptyList()) },
) : PaperboyApi {
    val briefRequests = mutableListOf<Pair<List<String>, Int>>()

    override suspend fun topics() = topics.invoke()

    override suspend fun brief(
        topicIds: List<String>,
        limit: Int,
    ): Brief {
        briefRequests += topicIds to limit
        return response()
    }
}

class FakeBriefLocalDataSource(
    initial: CachedBrief? = null,
) : BriefLocalDataSource {
    val stored = MutableStateFlow(initial)

    override fun observe() = stored

    override fun observeStory(id: String) = stored.map { cached -> cached?.brief?.stories?.find { it.id == id } }

    override suspend fun replace(
        brief: Brief,
        fetchedAtEpochMillis: Long,
    ) {
        stored.value = CachedBrief(brief, fetchedAtEpochMillis)
    }
}

class FakeInterestsRepository(
    initial: Set<String>? = null,
) : InterestsRepository {
    override val followedTopicIds = MutableStateFlow(initial)

    override suspend fun setFollowedTopics(ids: Set<String>) {
        followedTopicIds.value = ids
    }
}

fun story(id: String) =
    Story(
        id = id,
        headline = "Headline $id",
        summary = "Summary",
        url = "https://example.com/$id",
        source = Source("src", "Example", "https://example.com"),
        topicIds = listOf("tech"),
        publishedAtEpochMillis = 0,
    )

class FakeSavedRepository : SavedRepository {
    val saved = MutableStateFlow<List<Story>>(emptyList())

    override fun observeSaved() = saved

    override fun observeSavedIds() = saved.map { list -> list.mapTo(mutableSetOf()) { it.id } }

    override fun observeStory(id: String) = saved.map { list -> list.find { it.id == id } }

    override suspend fun save(story: Story) {
        saved.value = listOf(story) + saved.value.filterNot { it.id == story.id }
    }

    override suspend fun remove(storyId: String) {
        saved.value = saved.value.filterNot { it.id == storyId }
    }
}
