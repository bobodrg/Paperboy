package ro.bogdandragomir.paperboy.story

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler

@Composable
actual fun rememberArticleBrowser(): (url: String) -> Unit {
    val context = LocalContext.current
    val fallback = LocalUriHandler.current
    val toolbarColor = MaterialTheme.colorScheme.background.toArgb()
    return remember(context, toolbarColor) {
        { url ->
            val customTab =
                CustomTabsIntent
                    .Builder()
                    .setShowTitle(true)
                    .setDefaultColorSchemeParams(
                        CustomTabColorSchemeParams.Builder().setToolbarColor(toolbarColor).build(),
                    ).build()
            try {
                customTab.launchUrl(context, Uri.parse(url))
            } catch (e: ActivityNotFoundException) {
                // No browser supports Custom Tabs: hand the link to whatever can open it.
                fallback.openUri(url)
            }
        }
    }
}
