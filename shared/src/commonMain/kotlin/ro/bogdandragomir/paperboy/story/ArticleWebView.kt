package ro.bogdandragomir.paperboy.story

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ro.bogdandragomir.paperboy.core.model.Story

/**
 * The publisher's page, embedded in Paperboy's article screen: Android WebView, iOS WKWebView.
 * [onProgress] reports page loading from 0 to 1; [onError] fires when the page can't load (e.g. offline).
 */
@Composable
expect fun ArticleWebView(
    url: String,
    onProgress: (Float) -> Unit,
    onError: () -> Unit,
    modifier: Modifier = Modifier,
)

/** Opens the system share sheet with the story's headline and link. */
@Composable
expect fun rememberShareStory(): (Story) -> Unit

internal fun shareText(story: Story) = "${story.headline}\n${story.url}\n\nvia Paperboy"
