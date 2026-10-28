package ro.bogdandragomir.paperboy.story

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ro.bogdandragomir.paperboy.brief.formatAge
import ro.bogdandragomir.paperboy.core.model.Story
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun StoryRoute(
    storyId: String,
    onBack: () -> Unit,
    viewModel: StoryViewModel = koinViewModel(key = storyId) { parametersOf(storyId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StoryScreen(uiState, onBack = onBack, onReadArticle = rememberArticleBrowser())
}

@Composable
fun StoryScreen(
    uiState: StoryUiState,
    onBack: () -> Unit,
    onReadArticle: (url: String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        TextButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) { Text("← Your brief") }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (uiState) {
                StoryUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                StoryUiState.NotFound ->
                    Text(
                        text = "This story is no longer in your brief.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    )
                is StoryUiState.Ready -> StoryContent(uiState.story, onReadArticle)
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun StoryContent(
    story: Story,
    onReadArticle: (url: String) -> Unit,
) {
    val now = Clock.System.now().toEpochMilliseconds()
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text =
                buildString {
                    append(story.source.name.uppercase())
                    append("  ·  ")
                    append(formatAge(story.publishedAtEpochMillis, now).uppercase())
                    if (story.sourceCount > 1) append("  ·  COVERED BY ${story.sourceCount} SOURCES")
                },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(story.headline, style = MaterialTheme.typography.displaySmall)
        if (story.imageUrl != null) {
            AsyncImage(
                model = story.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp)),
            )
        }
        if (story.summary.isNotBlank()) {
            Text(story.summary, style = MaterialTheme.typography.bodyLarge)
        }
        Button(
            onClick = { onReadArticle(story.url) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text("Read the full story at ${story.source.name}")
        }
        Text(
            text = "Paperboy shows the publisher's own summary. The full article opens on their site.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
