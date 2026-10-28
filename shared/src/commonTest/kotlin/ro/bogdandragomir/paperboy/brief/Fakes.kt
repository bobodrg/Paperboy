package ro.bogdandragomir.paperboy.brief

import kotlinx.coroutines.flow.MutableStateFlow
import ro.bogdandragomir.paperboy.core.model.Brief
import ro.bogdandragomir.paperboy.core.model.Source
import ro.bogdandragomir.paperboy.core.model.Story
import ro.bogdandragomir.paperboy.data.local.BriefLocalDataSource
import ro.bogdandragomir.paperboy.data.local.CachedBrief
import ro.bogdandragomir.paperboy.data.remote.BriefApi

class FakeBriefApi(
    var response: () -> Brief,
) : BriefApi {
    val requests = mutableListOf<Pair<List<String>, Int>>()

    override suspend fun brief(
        topicIds: List<String>,
        limit: Int,
    ): Brief {
        requests += topicIds to limit
        return response()
    }
}

class FakeBriefLocalDataSource(
    initial: CachedBrief? = null,
) : BriefLocalDataSource {
    val stored = MutableStateFlow(initial)

    override fun observe() = stored

    override suspend fun replace(
        brief: Brief,
        fetchedAtEpochMillis: Long,
    ) {
        stored.value = CachedBrief(brief, fetchedAtEpochMillis)
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
