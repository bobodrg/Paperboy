package ro.bogdandragomir.paperboy.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import ro.bogdandragomir.paperboy.core.model.Brief
import ro.bogdandragomir.paperboy.core.model.Source
import ro.bogdandragomir.paperboy.core.model.Story

/** The brief stored on the device, and when it was downloaded. */
data class CachedBrief(
    val brief: Brief,
    val fetchedAtEpochMillis: Long,
)

interface BriefLocalDataSource {
    /** Emits null until a brief has been downloaded once. */
    fun observe(): Flow<CachedBrief?>

    suspend fun replace(
        brief: Brief,
        fetchedAtEpochMillis: Long,
    )
}

class RoomBriefLocalDataSource(
    private val dao: BriefDao,
) : BriefLocalDataSource {
    override fun observe(): Flow<CachedBrief?> =
        combine(dao.observeStories(), dao.observeMeta()) { stories, meta ->
            meta?.let { CachedBrief(Brief(it.date, stories.map(StoryEntity::toStory)), it.fetchedAtEpochMillis) }
        }

    override suspend fun replace(
        brief: Brief,
        fetchedAtEpochMillis: Long,
    ) = dao.replaceBrief(
        stories = brief.stories.mapIndexed { index, story -> story.toEntity(index) },
        meta = BriefMetaEntity(date = brief.date, fetchedAtEpochMillis = fetchedAtEpochMillis),
    )
}

private fun Story.toEntity(position: Int) =
    StoryEntity(
        id = id,
        position = position,
        headline = headline,
        summary = summary,
        imageUrl = imageUrl,
        url = url,
        sourceId = source.id,
        sourceName = source.name,
        sourceHomepageUrl = source.homepageUrl,
        topicIds = topicIds.joinToString(","),
        publishedAtEpochMillis = publishedAtEpochMillis,
        sourceCount = sourceCount,
    )

private fun StoryEntity.toStory() =
    Story(
        id = id,
        headline = headline,
        summary = summary,
        imageUrl = imageUrl,
        url = url,
        source = Source(sourceId, sourceName, sourceHomepageUrl),
        topicIds = topicIds.split(',').filter { it.isNotEmpty() },
        publishedAtEpochMillis = publishedAtEpochMillis,
        sourceCount = sourceCount,
    )
