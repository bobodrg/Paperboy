package ro.bogdandragomir.paperboy.brief

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ro.bogdandragomir.paperboy.core.model.Story
import ro.bogdandragomir.paperboy.data.local.InterestsRepository

sealed interface BriefUiState {
    /** Nothing stored yet and the first download is running. */
    data object Loading : BriefUiState

    /** Nothing stored and the download failed: the only state with nothing to read. */
    data object Error : BriefUiState

    data class Ready(
        val stories: List<Story>,
        val fetchedAtEpochMillis: Long,
        val isRefreshing: Boolean,
        /** The last refresh failed, so this is an older brief. */
        val isStale: Boolean,
    ) : BriefUiState
}

private data class RefreshStatus(
    val isRefreshing: Boolean = false,
    val failed: Boolean = false,
)

class BriefViewModel(
    private val repository: BriefRepository,
    interests: InterestsRepository,
) : ViewModel() {
    private val refreshStatus = MutableStateFlow(RefreshStatus())
    private var refreshJob: Job? = null

    val uiState: StateFlow<BriefUiState> =
        combine(repository.observeBrief(), refreshStatus) { cached, status ->
            when {
                cached != null ->
                    BriefUiState.Ready(
                        stories = cached.brief.stories,
                        fetchedAtEpochMillis = cached.fetchedAtEpochMillis,
                        isRefreshing = status.isRefreshing,
                        isStale = status.failed,
                    )
                status.failed -> BriefUiState.Error
                else -> BriefUiState.Loading
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BriefUiState.Loading)

    init {
        // Downloads on start, and again whenever the reader changes their topics.
        viewModelScope.launch {
            interests.followedTopicIds
                .filterNotNull()
                .distinctUntilChanged()
                .collect { startRefresh(replaceRunning = true) }
        }
    }

    /** Pull to refresh or "Try again". Ignored while a refresh is already running. */
    fun refresh() = startRefresh(replaceRunning = false)

    private fun startRefresh(replaceRunning: Boolean) {
        if (refreshJob?.isActive == true) {
            if (!replaceRunning) return
            refreshJob?.cancel()
        }
        refreshJob =
            viewModelScope.launch {
                refreshStatus.value = refreshStatus.value.copy(isRefreshing = true)
                val result = repository.refresh()
                refreshStatus.value = RefreshStatus(isRefreshing = false, failed = result.isFailure)
            }
    }
}
