package ro.bogdandragomir.paperboy.story

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ro.bogdandragomir.paperboy.brief.BriefRepository
import ro.bogdandragomir.paperboy.core.model.Story

sealed interface StoryUiState {
    data object Loading : StoryUiState

    /** The story left the stored brief, e.g. a refresh replaced it while this screen was in the back stack. */
    data object NotFound : StoryUiState

    data class Ready(
        val story: Story,
    ) : StoryUiState
}

class StoryViewModel(
    storyId: String,
    repository: BriefRepository,
) : ViewModel() {
    val uiState: StateFlow<StoryUiState> =
        repository
            .observeStory(storyId)
            .map { story -> if (story == null) StoryUiState.NotFound else StoryUiState.Ready(story) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StoryUiState.Loading)
}
