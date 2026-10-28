package ro.bogdandragomir.paperboy.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ro.bogdandragomir.paperboy.core.model.Story
import ro.bogdandragomir.paperboy.designsystem.StoryCard
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun SavedRoute(
    onOpenStory: (storyId: String) -> Unit,
    viewModel: SavedViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lastRemoved by viewModel.lastRemoved.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(lastRemoved) {
        if (lastRemoved == null) return@LaunchedEffect
        val result =
            snackbar.showSnackbar(
                "Removed from saved",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short,
            )
        if (result == SnackbarResult.ActionPerformed) viewModel.undoRemove() else viewModel.dismissUndo()
    }

    Box(Modifier.fillMaxSize()) {
        SavedScreen(uiState, onOpenStory = onOpenStory, onRemove = viewModel::remove)
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun SavedScreen(
    uiState: SavedUiState,
    onOpenStory: (storyId: String) -> Unit,
    onRemove: (Story) -> Unit,
) {
    val now = Clock.System.now().toEpochMilliseconds()
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        Text(
            text = "Saved",
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp),
        )
        when (uiState) {
            SavedUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            is SavedUiState.Ready ->
                if (uiState.stories.isEmpty()) {
                    EmptySaved()
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(uiState.stories, key = { it.id }) { story ->
                            StoryCard(
                                story = story,
                                nowEpochMillis = now,
                                isSaved = true,
                                onToggleSave = { onRemove(story) },
                                onClick = { onOpenStory(story.id) },
                                showImage = false,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
        }
    }
}

@Composable
private fun EmptySaved() {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Nothing saved yet", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Tap the bookmark on any story to keep it here. Saved stories stay on your phone, even offline.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
