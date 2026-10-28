package ro.bogdandragomir.paperboy.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ro.bogdandragomir.paperboy.core.model.Topic
import ro.bogdandragomir.paperboy.data.local.InterestsRepository
import ro.bogdandragomir.paperboy.data.local.SavedRepository
import ro.bogdandragomir.paperboy.data.local.Settings
import ro.bogdandragomir.paperboy.data.local.SettingsRepository
import ro.bogdandragomir.paperboy.data.local.ThemeMode
import ro.bogdandragomir.paperboy.data.remote.PaperboyApi

data class ProfileUiState(
    val topicNames: List<String> = emptyList(),
    val savedCount: Int = 0,
    val settings: Settings = Settings(),
)

class ProfileViewModel(
    interests: InterestsRepository,
    saved: SavedRepository,
    private val settingsRepository: SettingsRepository,
    private val api: PaperboyApi,
) : ViewModel() {
    // Topic names come from the API; offline, the ids are shown in a readable form instead.
    private val knownTopics = MutableStateFlow<List<Topic>>(emptyList())

    val uiState: StateFlow<ProfileUiState> =
        combine(
            interests.followedTopicIds,
            knownTopics,
            saved.observeSavedIds(),
            settingsRepository.settings,
        ) { followed, topics, savedIds, settings ->
            val names = topics.associate { it.id to it.name }
            ProfileUiState(
                topicNames =
                    followed
                        .orEmpty()
                        .map { id ->
                            names[id] ?: id.replaceFirstChar(Char::uppercase)
                        }.sorted(),
                savedCount = savedIds.size,
                settings = settings,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    init {
        viewModelScope.launch {
            try {
                knownTopics.value = api.topics()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.i { "Profile shows topic ids: ${e.message}" }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) = launchEdit { settingsRepository.setThemeMode(mode) }

    fun setMorningHour(hour: Int) = launchEdit { settingsRepository.setMorningHour(hour) }

    fun setMorningNotification(enabled: Boolean) = launchEdit { settingsRepository.setMorningNotification(enabled) }

    private fun launchEdit(edit: suspend () -> Unit) {
        viewModelScope.launch { edit() }
    }
}
