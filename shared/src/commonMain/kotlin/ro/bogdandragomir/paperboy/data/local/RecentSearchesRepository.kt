package ro.bogdandragomir.paperboy.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface RecentSearchesRepository {
    /** Newest first. */
    val recent: Flow<List<String>>

    suspend fun add(query: String)

    suspend fun clear()
}

class DataStoreRecentSearchesRepository(
    private val dataStore: DataStore<Preferences>,
) : RecentSearchesRepository {
    private val key = stringPreferencesKey("recent_searches")

    override val recent: Flow<List<String>> = dataStore.data.map { decode(it[key]) }

    override suspend fun add(query: String) {
        val cleaned = query.trim()
        if (cleaned.isEmpty()) return
        dataStore.edit { prefs ->
            val updated =
                (
                    listOf(
                        cleaned,
                    ) + decode(prefs[key]).filterNot { it.equals(cleaned, ignoreCase = true) }
                ).take(MAX)
            prefs[key] = updated.joinToString(SEPARATOR)
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(key) }
    }

    private fun decode(stored: String?): List<String> = stored?.split(SEPARATOR)?.filter { it.isNotEmpty() }.orEmpty()

    private companion object {
        const val MAX = 8

        // A control character readers can't type, so it never appears inside a query.
        const val SEPARATOR = "\u001F"
    }
}
