package ro.bogdandragomir.paperboy.designsystem

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import ro.bogdandragomir.paperboy.core.model.Story

/** A story as it appears in Today, Search and Saved. */
@Composable
fun StoryCard(
    story: Story,
    nowEpochMillis: Long,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showImage: Boolean = true,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        modifier = modifier.storyBounds(story.id).fillMaxWidth().clickable(onClick = onClick),
    ) {
        if (showImage && story.imageUrl != null) {
            AsyncImage(
                model = story.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )
        }
        Column(
            Modifier.padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = storyByline(story, nowEpochMillis),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                SaveButton(isSaved, onToggleSave)
            }
            Column(Modifier.padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
}

@Composable
fun SaveButton(
    isSaved: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onToggle, modifier = modifier) {
        Crossfade(isSaved) { saved ->
            Icon(
                imageVector = if (saved) PaperboyIcons.Bookmark else PaperboyIcons.BookmarkBorder,
                contentDescription = if (saved) "Remove from saved" else "Save",
                tint = if (saved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** "BBC NEWS · 3H AGO · 4 SOURCES" */
fun storyByline(
    story: Story,
    nowEpochMillis: Long,
): String =
    buildString {
        append(story.source.name.uppercase())
        append("  ·  ")
        append(formatAge(story.publishedAtEpochMillis, nowEpochMillis).uppercase())
        if (story.sourceCount > 1) append("  ·  ${story.sourceCount} SOURCES")
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
