package ro.bogdandragomir.paperboy.story

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.SafariServices.SFSafariViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

@Composable
actual fun rememberArticleBrowser(): (url: String) -> Unit =
    remember {
        { url ->
            val nsUrl = NSURL.URLWithString(url)
            val presenter = topViewController()
            if (nsUrl != null && presenter != null) {
                presenter.presentViewController(SFSafariViewController(uRL = nsUrl), animated = true, completion = null)
            }
        }
    }

/** The view controller currently on screen, so sheets open above whatever is showing. */
internal fun topViewController(): UIViewController? {
    val window =
        UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<UIWindowScene>()
            .flatMap { scene -> scene.windows.filterIsInstance<UIWindow>() }
            .firstOrNull { it.isKeyWindow() }
    var top = window?.rootViewController
    while (top?.presentedViewController != null) top = top.presentedViewController
    return top
}
