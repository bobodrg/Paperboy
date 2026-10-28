package ro.bogdandragomir.paperboy.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ro.bogdandragomir.paperboy.core.model.Topic
import ro.bogdandragomir.paperboy.data.local.InterestsRepository
import ro.bogdandragomir.paperboy.data.remote.PaperboyApi

const val MIN_TOPICS = 3

data class OnboardingUiState(
    val topics: List<Topic> = emptyList(),
    val selected: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
) {
    val canSave: Boolean get() = selected.size >= MIN_TOPICS

    /** Top-level topics, each followed by its sub-topics when it's selected (like Spotify's genre bubbles). */
    val visibleTopics: List<Topic>
        get() =
            topics.filter { it.parentId == null }.flatMap { parent ->
                listOf(parent) + if (parent.id in selected) topics.filter { it.parentId == parent.id } else emptyList()
            }
}

class OnboardingViewModel(
    private val api: PaperboyApi,
    private val interests: InterestsRepository,
) : ViewModel() {
    private val state = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            state.update { it.copy(isLoading = true, loadFailed = false) }
            try {
                val topics = api.topics()
                val alreadyFollowed = interests.followedTopicIds.first().orEmpty()
                state.update { it.copy(topics = topics, selected = alreadyFollowed, isLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(e) { "Couldn't load topics" }
                state.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    /** Unselecting a topic also unselects its sub-topics, which disappear with it. */
    fun toggle(topicId: String) {
        state.update { current ->
            val selected =
                if (topicId in current.selected) {
                    val children =
                        current.topics
                            .filter { it.parentId == topicId }
                            .map { it.id }
                            .toSet()
                    current.selected - topicId - children
                } else {
                    current.selected + topicId
                }
            current.copy(selected = selected)
        }
    }

    fun save(onSaved: () -> Unit) {
        val selected = state.value.selected
        if (selected.size < MIN_TOPICS) return
        viewModelScope.launch {
            interests.setFollowedTopics(selected)
            onSaved()
        }
    }
}
