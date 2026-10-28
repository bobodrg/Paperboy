package ro.bogdandragomir.paperboy.brief

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.koin.compose.viewmodel.koinViewModel
import ro.bogdandragomir.paperboy.core.model.Story
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun BriefRoute(
    onEditTopics: () -> Unit,
    onOpenStory: (storyId: String) -> Unit,
    viewModel: BriefViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BriefScreen(uiState, onRefresh = viewModel::refresh, onEditTopics = onEditTopics, onOpenStory = onOpenStory)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BriefScreen(
    uiState: BriefUiState,
    onRefresh: () -> Unit,
    onEditTopics: () -> Unit,
    onOpenStory: (storyId: String) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        when (uiState) {
            BriefUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            BriefUiState.Error -> ErrorState(onRetry = onRefresh, modifier = Modifier.align(Alignment.Center))
            is BriefUiState.Ready ->
                PullToRefreshBox(isRefreshing = uiState.isRefreshing, onRefresh = onRefresh) {
                    BriefList(uiState, onEditTopics, onOpenStory)
                }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun BriefList(
    state: BriefUiState.Ready,
    onEditTopics: () -> Unit,
    onOpenStory: (storyId: String) -> Unit,
) {
    val now = Clock.System.now().toEpochMilliseconds()
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Paperboy",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onEditTopics) { Text("Topics") }
            }
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
        items(state.stories, key = { it.id }) { StoryCard(it, now, onClick = { onOpenStory(it.id) }) }
        item { CaughtUp() }
    }
}

@Composable
private fun StoryCard(
    story: Story,
    nowEpochMillis: Long,
    onClick: () -> Unit,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
    ) {
        if (story.imageUrl != null) {
            AsyncImage(
                model = story.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )
        }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text =
                    buildString {
                        append(story.source.name.uppercase())
                        append("  ·  ")
                        append(formatAge(story.publishedAtEpochMillis, nowEpochMillis).uppercase())
                        if (story.sourceCount > 1) append("  ·  ${story.sourceCount} SOURCES")
                    },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(story.headline, style = MaterialTheme.typography.titleLarge)
            if (story.summary.isNotBlank()) {
                Text(
                    text = story.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
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

/** "Just now", "25m ago", "3h ago", "2d ago". */
fun formatAge(
    epochMillis: Long,
    nowEpochMillis: Long,
): String {
    val minutes = ((nowEpochMillis - epochMillis) / 60_000).coerceAtLeast(0)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 24 * 60 -> "${minutes / 60}h ago"
        else -> "${minutes / (24 * 60)}d ago"
    }
}
