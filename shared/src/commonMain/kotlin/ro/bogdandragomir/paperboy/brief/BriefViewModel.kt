package ro.bogdandragomir.paperboy.brief

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ro.bogdandragomir.paperboy.core.model.Story

sealed interface BriefUiState {
    data object Loading : BriefUiState

    data class Ready(
        val stories: List<Story>,
    ) : BriefUiState

    data object Error : BriefUiState
}

class BriefViewModel(
    repository: BriefRepository,
) : ViewModel() {
    val uiState: StateFlow<BriefUiState> =
        repository
            .observeTodayBrief()
            .map<_, BriefUiState> { BriefUiState.Ready(it.stories) }
            .catch { emit(BriefUiState.Error) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BriefUiState.Loading)
}
