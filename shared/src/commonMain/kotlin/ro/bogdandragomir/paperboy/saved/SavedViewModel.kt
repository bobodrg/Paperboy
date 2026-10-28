package ro.bogdandragomir.paperboy.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ro.bogdandragomir.paperboy.core.model.Story
import ro.bogdandragomir.paperboy.data.local.SavedRepository

sealed interface SavedUiState {
    data object Loading : SavedUiState

    data class Ready(
        val stories: List<Story>,
    ) : SavedUiState
}

class SavedViewModel(
    private val saved: SavedRepository,
) : ViewModel() {
    val uiState: StateFlow<SavedUiState> =
        saved
            .observeSaved()
            .map<_, SavedUiState> { SavedUiState.Ready(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavedUiState.Loading)

    private val lastRemovedState = MutableStateFlow<Story?>(null)

    /** The story just removed, so the screen can offer Undo. */
    val lastRemoved: StateFlow<Story?> = lastRemovedState.asStateFlow()

    fun remove(story: Story) {
        viewModelScope.launch {
            saved.remove(story.id)
            lastRemovedState.value = story
        }
    }

    fun undoRemove() {
        val story = lastRemovedState.value ?: return
        lastRemovedState.value = null
        viewModelScope.launch { saved.save(story) }
    }

    fun dismissUndo() {
        lastRemovedState.value = null
    }
}
