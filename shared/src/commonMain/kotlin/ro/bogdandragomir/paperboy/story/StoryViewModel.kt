package ro.bogdandragomir.paperboy.story

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ro.bogdandragomir.paperboy.core.model.Story
import ro.bogdandragomir.paperboy.data.local.SavedRepository

sealed interface StoryUiState {
    data object Loading : StoryUiState

    /** The story isn't in the brief, saved, or recent results any more (e.g. a refresh replaced it). */
    data object NotFound : StoryUiState

    data class Ready(
        val story: Story,
        val isSaved: Boolean,
    ) : StoryUiState
}

class StoryViewModel(
    storyId: String,
    lookup: StoryLookup,
    private val saved: SavedRepository,
) : ViewModel() {
    val uiState: StateFlow<StoryUiState> =
        combine(lookup.observe(storyId), saved.observeSavedIds()) { story, savedIds ->
            if (story == null) StoryUiState.NotFound else StoryUiState.Ready(story, isSaved = story.id in savedIds)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StoryUiState.Loading)

    fun toggleSave() {
        val state = uiState.value as? StoryUiState.Ready ?: return
        viewModelScope.launch {
            if (state.isSaved) saved.remove(state.story.id) else saved.save(state.story)
        }
    }
}
