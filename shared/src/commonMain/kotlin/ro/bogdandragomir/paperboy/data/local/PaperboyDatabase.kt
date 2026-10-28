package ro.bogdandragomir.paperboy.data.local

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow

/** One story of the cached brief. [position] keeps the server's ranking. */
@Entity(tableName = "brief_stories")
data class StoryEntity(
    @PrimaryKey val id: String,
    val position: Int,
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
)

/** A single row (id = 0) describing the cached brief. */
@Entity(tableName = "brief_meta")
data class BriefMetaEntity(
    @PrimaryKey val id: Int = 0,
    val date: String,
    val fetchedAtEpochMillis: Long,
)

@Dao
abstract class BriefDao {
    @Query("SELECT * FROM brief_stories ORDER BY position")
    abstract fun observeStories(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM brief_meta WHERE id = 0")
    abstract fun observeMeta(): Flow<BriefMetaEntity?>

    @Query("DELETE FROM brief_stories")
    protected abstract suspend fun clearStories()

    @Insert
    protected abstract suspend fun insertStories(stories: List<StoryEntity>)

    @Upsert
    protected abstract suspend fun upsertMeta(meta: BriefMetaEntity)

    /** Swaps the whole brief at once, so the UI never sees half of an old and half of a new one. */
    @Transaction
    open suspend fun replaceBrief(
        stories: List<StoryEntity>,
        meta: BriefMetaEntity,
    ) {
        clearStories()
        insertStories(stories)
        upsertMeta(meta)
    }
}

@Database(entities = [StoryEntity::class, BriefMetaEntity::class], version = 1)
@ConstructedBy(PaperboyDatabaseConstructor::class)
abstract class PaperboyDatabase : RoomDatabase() {
    abstract fun briefDao(): BriefDao
}

// Room's compiler generates the actual implementations.
@Suppress("KotlinNoActualForExpect")
expect object PaperboyDatabaseConstructor : RoomDatabaseConstructor<PaperboyDatabase> {
    override fun initialize(): PaperboyDatabase
}

/**
 * Finishes a platform's database builder. The database is only a cache of server data,
 * so a schema change may simply drop it and download the brief again.
 */
fun RoomDatabase.Builder<PaperboyDatabase>.buildPaperboyDatabase(): PaperboyDatabase =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
