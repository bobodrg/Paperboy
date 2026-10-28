package ro.bogdandragomir.paperboy.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ro.bogdandragomir.paperboy.core.model.Story
import ro.bogdandragomir.paperboy.designsystem.PaperboyIcons
import ro.bogdandragomir.paperboy.designsystem.StoryCard
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun SearchRoute(
    onOpenStory: (storyId: String) -> Unit,
    viewModel: SearchViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SearchScreen(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onSubmit = viewModel::rememberQuery,
        onClearRecent = viewModel::clearRecent,
        onToggleSave = viewModel::toggleSave,
        onOpenStory = { id ->
            viewModel.rememberQuery()
            onOpenStory(id)
        },
    )
}

@OptIn(ExperimentalTime::class)
@Composable
fun SearchScreen(
    uiState: SearchUiState,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClearRecent: () -> Unit,
    onToggleSave: (Story) -> Unit,
    onOpenStory: (storyId: String) -> Unit,
) {
    val now = Clock.System.now().toEpochMilliseconds()
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp)) {
            Text("Search", style = MaterialTheme.typography.displaySmall)
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                placeholder = { Text("Topics, headlines, sources…") },
                leadingIcon = { Icon(PaperboyIcons.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                        ) { Icon(PaperboyIcons.Close, contentDescription = "Clear") }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
        }

        when (val results = uiState.results) {
            SearchResults.Idle ->
                RecentSearches(
                    uiState.recentSearches,
                    onPick = onQueryChange,
                    onClear = onClearRecent,
                )
            SearchResults.Searching -> LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 20.dp))
            is SearchResults.Found ->
                if (results.stories.isEmpty()) {
                    Message(
                        if (results.offline) {
                            "You're offline, and none of your saved stories match \"${uiState.query.trim()}\"."
                        } else {
                            "No stories from the last week match \"${uiState.query.trim()}\"."
                        },
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (results.offline) {
                            item {
                                Text(
                                    "You're offline: showing matches from your saved stories.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        items(results.stories, key = { it.id }) { story ->
                            StoryCard(
                                story = story,
                                nowEpochMillis = now,
                                isSaved = story.id in uiState.savedIds,
                                onToggleSave = { onToggleSave(story) },
                                onClick = { onOpenStory(story.id) },
                                showImage = false,
                            )
                        }
                    }
                }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecentSearches(
    recent: List<String>,
    onPick: (String) -> Unit,
    onClear: () -> Unit,
) {
    if (recent.isEmpty()) {
        Message("Search the last week of news by keyword, topic or source.")
        return
    }
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "RECENT",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClear) { Text("Clear") }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            recent.forEach { query -> AssistChip(onClick = { onPick(query) }, label = { Text(query) }) }
        }
    }
}

@Composable
private fun Message(text: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
