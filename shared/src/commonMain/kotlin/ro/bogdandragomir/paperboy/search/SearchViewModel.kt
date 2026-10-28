package ro.bogdandragomir.paperboy.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ro.bogdandragomir.paperboy.core.model.Story
import ro.bogdandragomir.paperboy.data.local.RecentSearchesRepository
import ro.bogdandragomir.paperboy.data.local.SavedRepository
import ro.bogdandragomir.paperboy.data.remote.PaperboyApi
import ro.bogdandragomir.paperboy.story.RecentStories

sealed interface SearchResults {
    /** Nothing typed yet: the screen shows recent searches instead. */
    data object Idle : SearchResults

    data object Searching : SearchResults

    data class Found(
        val stories: List<Story>,
        /** True when the server couldn't be reached and only saved stories were searched. */
        val offline: Boolean = false,
    ) : SearchResults
}

data class SearchUiState(
    val results: SearchResults = SearchResults.Idle,
    val recentSearches: List<String> = emptyList(),
    val savedIds: Set<String> = emptySet(),
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val api: PaperboyApi,
    private val saved: SavedRepository,
    private val recentSearches: RecentSearchesRepository,
    private val recentStories: RecentStories,
) : ViewModel() {
    /**
     * What the reader typed. Compose state, not a Flow: the text field must read back every keystroke
     * immediately, or fast typing drops letters.
     */
    var query by mutableStateOf("")
        private set

    private val results: Flow<SearchResults> =
        snapshotFlow { query }
            .map { it.trim() }
            .distinctUntilChanged()
            // Wait for a short pause in typing instead of searching on every key press.
            .debounce(DEBOUNCE_MILLIS)
            .flatMapLatest { text -> if (text.isEmpty()) flowOf(SearchResults.Idle) else search(text) }

    val uiState: StateFlow<SearchUiState> =
        combine(results, recentSearches.recent, saved.observeSavedIds()) { found, recent, savedIds ->
            SearchUiState(results = found, recentSearches = recent, savedIds = savedIds)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onQueryChange(text: String) {
        query = text
    }

    /** The reader opened a result or pressed search: remember what they looked for. */
    fun rememberQuery() {
        val text = query.trim()
        if (text.isNotEmpty()) viewModelScope.launch { recentSearches.add(text) }
    }

    fun clearRecent() {
        viewModelScope.launch { recentSearches.clear() }
    }

    fun toggleSave(story: Story) {
        viewModelScope.launch {
            if (story.id in uiState.value.savedIds) saved.remove(story.id) else saved.save(story)
        }
    }

    private fun search(text: String): Flow<SearchResults> =
        flow {
            emit(SearchResults.Searching)
            val found =
                try {
                    SearchResults.Found(api.search(text, LIMIT))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.w(e) { "Search failed, searching saved stories instead" }
                    SearchResults.Found(searchSaved(text), offline = true)
                }
            recentStories.remember(found.stories)
            emit(found)
        }

    private suspend fun searchSaved(text: String): List<Story> {
        val words = text.lowercase().split(' ').filter { it.isNotBlank() }
        return saved.observeSaved().first().filter { story ->
            val haystack = "${story.headline} ${story.summary} ${story.source.name}".lowercase()
            words.all { it in haystack }
        }
    }

    private companion object {
        const val DEBOUNCE_MILLIS = 300L
        const val LIMIT = 20
    }
}
