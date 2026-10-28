package ro.bogdandragomir.paperboy.story

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import ro.bogdandragomir.paperboy.core.model.Story

@SuppressLint("SetJavaScriptEnabled") // News sites need JavaScript; no JavaScript interface is exposed to them.
@Composable
actual fun ArticleWebView(
    url: String,
    onProgress: (Float) -> Unit,
    onError: () -> Unit,
    modifier: Modifier,
) {
    val currentOnProgress = rememberUpdatedState(onProgress)
    val currentOnError = rememberUpdatedState(onError)
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                webChromeClient =
                    object : WebChromeClient() {
                        override fun onProgressChanged(
                            view: WebView,
                            newProgress: Int,
                        ) = currentOnProgress.value(newProgress / 100f)
                    }
                webViewClient =
                    object : WebViewClient() {
                        // Stay on the web: links to other apps (intent://, market://…) are ignored.
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ) = request.url.scheme !in setOf("http", "https")

                        override fun onReceivedError(
                            view: WebView,
                            request: WebResourceRequest,
                            error: WebResourceError,
                        ) {
                            if (request.isForMainFrame) currentOnError.value()
                        }
                    }
                loadUrl(url)
            }
        },
        onRelease = { it.destroy() },
    )
}

@Composable
actual fun rememberShareStory(): (Story) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { story ->
            val send =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, story.headline)
                    putExtra(Intent.EXTRA_TEXT, shareText(story))
                }
            context.startActivity(Intent.createChooser(send, null))
        }
    }
}
