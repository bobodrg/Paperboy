package ro.bogdandragomir.paperboy.story

import androidx.compose.runtime.Composable

/**
 * Opens the publisher's article inside the app: Chrome Custom Tabs on Android,
 * SFSafariViewController on iOS. The reader stays in Paperboy and comes back with one gesture.
 */
@Composable
expect fun rememberArticleBrowser(): (url: String) -> Unit
