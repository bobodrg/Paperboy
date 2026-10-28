package ro.bogdandragomir.paperboy.brief

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ro.bogdandragomir.paperboy.core.model.Story
import ro.bogdandragomir.paperboy.designsystem.StoryCard
import ro.bogdandragomir.paperboy.designsystem.formatAge
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun BriefRoute(
    onOpenStory: (storyId: String) -> Unit,
    viewModel: BriefViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedIds by viewModel.savedIds.collectAsStateWithLifecycle()
    BriefScreen(
        uiState = uiState,
        savedIds = savedIds,
        onRefresh = viewModel::refresh,
        onOpenStory = onOpenStory,
        onToggleSave = viewModel::toggleSave,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BriefScreen(
    uiState: BriefUiState,
    savedIds: Set<String>,
    onRefresh: () -> Unit,
    onOpenStory: (storyId: String) -> Unit,
    onToggleSave: (Story) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                ),
    ) {
        when (uiState) {
            BriefUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            BriefUiState.Error -> ErrorState(onRetry = onRefresh, modifier = Modifier.align(Alignment.Center))
            is BriefUiState.Ready ->
                PullToRefreshBox(isRefreshing = uiState.isRefreshing, onRefresh = onRefresh) {
                    BriefList(uiState, savedIds, onOpenStory, onToggleSave)
                }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun BriefList(
    state: BriefUiState.Ready,
    savedIds: Set<String>,
    onOpenStory: (storyId: String) -> Unit,
    onToggleSave: (Story) -> Unit,
) {
    val now = Clock.System.now().toEpochMilliseconds()
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text("Paperboy", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text("Your morning edition", style = MaterialTheme.typography.displaySmall)
            if (state.isStale) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "You're offline. Last updated ${formatAge(state.fetchedAtEpochMillis, now).lowercase()}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        items(state.stories, key = { it.id }) { story ->
            StoryCard(
                story = story,
                nowEpochMillis = now,
                isSaved = story.id in savedIds,
                onToggleSave = { onToggleSave(story) },
                onClick = { onOpenStory(story.id) },
            )
        }
        item { CaughtUp() }
    }
}

@Composable
private fun CaughtUp() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("You're all caught up", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "See you tomorrow morning.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "The paperboy couldn't get through",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Check your connection and try again.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onRetry) { Text("Try again") }
    }
}
