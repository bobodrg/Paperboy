package ro.bogdandragomir.paperboy.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

/**
 * The topics the reader follows. Kept in DataStore, not in the Room cache: the cache may be
 * wiped on a schema change, the reader's choices must not be.
 */
interface InterestsRepository {
    /** Null until the reader has finished onboarding. */
    val followedTopicIds: Flow<Set<String>?>

    suspend fun setFollowedTopics(ids: Set<String>)
}

class DataStoreInterestsRepository(
    private val dataStore: DataStore<Preferences>,
) : InterestsRepository {
    private val followedKey = stringSetPreferencesKey("followed_topic_ids")

    override val followedTopicIds: Flow<Set<String>?> = dataStore.data.map { it[followedKey] }

    override suspend fun setFollowedTopics(ids: Set<String>) {
        dataStore.edit { it[followedKey] = ids }
    }
}

/** [path] is a platform file path ending in `.preferences_pb`. */
fun createPreferencesDataStore(path: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { path.toPath() })

const val PREFERENCES_FILE_NAME = "paperboy.preferences_pb"
