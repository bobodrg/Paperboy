package ro.bogdandragomir.paperboy.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { System, Light, Dark }

data class Settings(
    val themeMode: ThemeMode = ThemeMode.System,
    /** Hour of the day (0–23) the morning brief should be ready. */
    val morningHour: Int = 7,
    val morningNotification: Boolean = true,
    /** Needs an account; stored now so the choice is kept when accounts arrive. */
    val emailDigest: Boolean = false,
)

interface SettingsRepository {
    val settings: Flow<Settings>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setMorningHour(hour: Int)

    suspend fun setMorningNotification(enabled: Boolean)

    suspend fun setEmailDigest(enabled: Boolean)
}

/** Stored in the same DataStore file as the followed topics: preferences, not cached data. */
class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val morningHourKey = intPreferencesKey("morning_hour")
    private val morningNotificationKey = booleanPreferencesKey("morning_notification")
    private val emailDigestKey = booleanPreferencesKey("email_digest")

    override val settings: Flow<Settings> =
        dataStore.data.map { prefs ->
            val defaults = Settings()
            Settings(
                themeMode =
                    prefs[themeKey]?.let { name -> ThemeMode.entries.find { it.name == name } } ?: defaults.themeMode,
                morningHour = prefs[morningHourKey] ?: defaults.morningHour,
                morningNotification = prefs[morningNotificationKey] ?: defaults.morningNotification,
                emailDigest = prefs[emailDigestKey] ?: defaults.emailDigest,
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[themeKey] = mode.name }
    }

    override suspend fun setMorningHour(hour: Int) {
        require(hour in 0..23) { "hour must be 0–23" }
        dataStore.edit { it[morningHourKey] = hour }
    }

    override suspend fun setMorningNotification(enabled: Boolean) {
        dataStore.edit { it[morningNotificationKey] = enabled }
    }

    override suspend fun setEmailDigest(enabled: Boolean) {
        dataStore.edit { it[emailDigestKey] = enabled }
    }
}
