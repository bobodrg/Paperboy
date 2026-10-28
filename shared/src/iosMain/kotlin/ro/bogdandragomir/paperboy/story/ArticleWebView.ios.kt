package ro.bogdandragomir.paperboy.story

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.readValue
import kotlinx.coroutines.delay
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.UIKit.UIActivityViewController
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject
import ro.bogdandragomir.paperboy.core.model.Story

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun ArticleWebView(
    url: String,
    onProgress: (Float) -> Unit,
    onError: () -> Unit,
    modifier: Modifier,
) {
    val currentOnError = rememberUpdatedState(onError)
    val webView = remember { WKWebView(frame = CGRectZero.readValue(), configuration = WKWebViewConfiguration()) }
    // WKWebView only keeps a weak reference to its delegate, so we hold it here.
    val delegate = remember { NavigationDelegate { currentOnError.value() } }

    LaunchedEffect(url) {
        webView.navigationDelegate = delegate
        NSURL.URLWithString(url)?.let { webView.loadRequest(NSURLRequest(uRL = it)) }
        // WKWebView reports progress through key-value observing; polling it is simpler and cheap.
        // Some pages keep loading trackers and never report 100%, so "stopped loading" also means done.
        delay(PROGRESS_POLL_MILLIS)
        while (webView.loading) {
            onProgress(webView.estimatedProgress.toFloat())
            delay(PROGRESS_POLL_MILLIS)
        }
        onProgress(1f)
    }

    UIKitView(factory = { webView }, modifier = modifier)
}

private const val PROGRESS_POLL_MILLIS = 100L

private class NavigationDelegate(
    private val onError: () -> Unit,
) : NSObject(),
    WKNavigationDelegateProtocol {
    @ObjCSignatureOverride
    override fun webView(
        webView: WKWebView,
        didFailProvisionalNavigation: WKNavigation?,
        withError: NSError,
    ) = onError()

    @ObjCSignatureOverride
    override fun webView(
        webView: WKWebView,
        didFailNavigation: WKNavigation?,
        withError: NSError,
    ) = onError()
}

@Composable
actual fun rememberShareStory(): (Story) -> Unit =
    remember {
        { story ->
            val items = listOf(shareText(story))
            val sheet = UIActivityViewController(activityItems = items, applicationActivities = null)
            topViewController()?.presentViewController(sheet, animated = true, completion = null)
        }
    }
