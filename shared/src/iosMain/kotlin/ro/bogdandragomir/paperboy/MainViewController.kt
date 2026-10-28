package ro.bogdandragomir.paperboy

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** Called once from the Swift app's init. */
fun startPaperboy() {
    initKoin()
}

@Suppress("ktlint:standard:function-naming")
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
