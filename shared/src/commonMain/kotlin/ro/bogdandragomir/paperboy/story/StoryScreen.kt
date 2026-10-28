package ro.bogdandragomir.paperboy.story

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ro.bogdandragomir.paperboy.core.model.Story
import ro.bogdandragomir.paperboy.designsystem.PaperboyIcons
import ro.bogdandragomir.paperboy.designsystem.SaveButton
import ro.bogdandragomir.paperboy.designsystem.storyBounds
import ro.bogdandragomir.paperboy.designsystem.storyByline
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Reads a story inside Paperboy: the publisher's page under our own bar with Back, Share and Save. */
@Composable
fun StoryRoute(
    storyId: String,
    onBack: () -> Unit,
    viewModel: StoryViewModel = koinViewModel(key = storyId) { parametersOf(storyId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StoryScreen(
        modifier = Modifier.storyBounds(storyId),
        uiState = uiState,
        onBack = onBack,
        onToggleSave = viewModel::toggleSave,
        onShare = rememberShareStory(),
        onOpenInBrowser = rememberArticleBrowser(),
    )
}

@Composable
fun StoryScreen(
    uiState: StoryUiState,
    onBack: () -> Unit,
    onToggleSave: () -> Unit,
    onShare: (Story) -> Unit,
    onOpenInBrowser: (url: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var progress by remember { mutableFloatStateOf(0f) }
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        ArticleBar(uiState, onBack, onToggleSave, onShare)
        val shownProgress by animateFloatAsState(progress)
        AnimatedVisibility(visible = progress < 1f, exit = fadeOut()) {
            LinearProgressIndicator(progress = { shownProgress }, modifier = Modifier.fillMaxWidth())
        }
        HorizontalDivider()
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (uiState) {
                StoryUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                StoryUiState.NotFound ->
                    Text(
                        text = "This story is no longer available.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    )
                is StoryUiState.Ready ->
                    ArticleBody(uiState.story, onProgress = { progress = it }, onOpenInBrowser = onOpenInBrowser)
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun ArticleBar(
    uiState: StoryUiState,
    onBack: () -> Unit,
    onToggleSave: () -> Unit,
    onShare: (Story) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(PaperboyIcons.Back, contentDescription = "Back") }
        val story = (uiState as? StoryUiState.Ready)?.story
        Text(
            text = story?.let { storyByline(it, Clock.System.now().toEpochMilliseconds()) }.orEmpty(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        )
        if (uiState is StoryUiState.Ready) {
            IconButton(onClick = { onShare(uiState.story) }) { Icon(PaperboyIcons.Share, contentDescription = "Share") }
            SaveButton(isSaved = uiState.isSaved, onToggle = onToggleSave)
        }
    }
}

@Composable
private fun ArticleBody(
    story: Story,
    onProgress: (Float) -> Unit,
    onOpenInBrowser: (url: String) -> Unit,
) {
    var failed by remember { mutableStateOf(false) }
    // Changing the key recreates the web view, which is how "Try again" reloads the page.
    var attempt by remember { mutableIntStateOf(0) }
    if (failed) {
        OfflineArticle(
            story = story,
            onRetry = {
                failed = false
                attempt++
            },
            onOpenInBrowser = { onOpenInBrowser(story.url) },
        )
    } else {
        key(attempt) {
            ArticleWebView(
                url = story.url,
                onProgress = onProgress,
                onError = {
                    failed = true
                    onProgress(1f)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Without a connection the full article can't load, but the headline and summary are stored on the phone. */
@Composable
private fun OfflineArticle(
    story: Story,
    onRetry: () -> Unit,
    onOpenInBrowser: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(story.headline, style = MaterialTheme.typography.displaySmall)
        if (story.summary.isNotBlank()) Text(story.summary, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "The full article needs a connection. Your summary is saved on this phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onRetry) { Text("Try again") }
            OutlinedButton(onClick = onOpenInBrowser) { Text("Open in browser") }
        }
    }
}
