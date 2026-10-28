package ro.bogdandragomir.paperboy.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ro.bogdandragomir.paperboy.core.model.Source
import ro.bogdandragomir.paperboy.core.model.Story

/** A story the reader saved. Unlike the brief, this is the reader's own data: it survives refreshes and updates. */
@Entity(tableName = "saved_stories")
data class SavedStoryEntity(
    @PrimaryKey val id: String,
    val headline: String,
    val summary: String,
    val imageUrl: String?,
    val url: String,
    val sourceId: String,
    val sourceName: String,
    val sourceHomepageUrl: String,
    /** Comma-separated topic ids. */
    val topicIds: String,
    val publishedAtEpochMillis: Long,
    val sourceCount: Int,
    val savedAtEpochMillis: Long,
)

@Dao
interface SavedStoryDao {
    @Query("SELECT * FROM saved_stories ORDER BY savedAtEpochMillis DESC")
    fun observeAll(): Flow<List<SavedStoryEntity>>

    @Query("SELECT * FROM saved_stories WHERE id = :id")
    fun observeById(id: String): Flow<SavedStoryEntity?>

    @Upsert
    suspend fun upsert(story: SavedStoryEntity)

    @Query("DELETE FROM saved_stories WHERE id = :id")
    suspend fun delete(id: String)
}

interface SavedRepository {
    /** Newest saved first. */
    fun observeSaved(): Flow<List<Story>>

    fun observeSavedIds(): Flow<Set<String>>

    fun observeStory(id: String): Flow<Story?>

    suspend fun save(story: Story)

    suspend fun remove(storyId: String)
}

class RoomSavedRepository(
    private val dao: SavedStoryDao,
    private val nowEpochMillis: () -> Long,
) : SavedRepository {
    override fun observeSaved(): Flow<List<Story>> = dao.observeAll().map { rows -> rows.map { it.toStory() } }

    override fun observeSavedIds(): Flow<Set<String>> =
        dao.observeAll().map { rows ->
            rows.mapTo(mutableSetOf()) {
                it.id
            }
        }

    override fun observeStory(id: String): Flow<Story?> = dao.observeById(id).map { it?.toStory() }

    override suspend fun save(story: Story) = dao.upsert(story.toSavedEntity(nowEpochMillis()))

    override suspend fun remove(storyId: String) = dao.delete(storyId)
}

private fun Story.toSavedEntity(savedAt: Long) =
    SavedStoryEntity(
        id = id,
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
        savedAtEpochMillis = savedAt,
    )

private fun SavedStoryEntity.toStory() =
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
