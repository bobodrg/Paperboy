package ro.bogdandragomir.paperboy.designsystem

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The system "Reduce motion" setting. When true, Paperboy skips decorative animations. */
val LocalReduceMotion = compositionLocalOf { false }

/** Reads the platform's reduce-motion accessibility setting. */
@Composable
expect fun rememberReduceMotion(): Boolean

/** Set by the nav host so a story card and its article screen can morph into each other. */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/** The enter/exit animation of the current navigation destination. */
val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Marks the bounds of story [storyId]: the card in a list and the article screen share it, so opening
 * a story grows the card into the page (Material's "container transform") and Back shrinks it again.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.storyBounds(storyId: String): Modifier {
    val sharedScope = LocalSharedTransitionScope.current
    val visibilityScope = LocalNavAnimatedVisibilityScope.current
    if (sharedScope == null || visibilityScope == null || LocalReduceMotion.current) return this
    return with(sharedScope) {
        this@storyBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(key = "story-$storyId"),
            animatedVisibilityScope = visibilityScope,
        )
    }
}

/**
 * Cards "dealt" onto the table: each slides up and fades in a moment after the one before it.
 * Only the first [MAX_STAGGERED] items are staggered, so a long list doesn't make you wait.
 */
@Composable
fun Modifier.dealIn(index: Int): Modifier {
    if (LocalReduceMotion.current) return this
    val offsetPx = with(LocalDensity.current) { 24.dp.toPx() }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index.coerceAtMost(MAX_STAGGERED) * STAGGER_MILLIS)
        launch {
            progress.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
        }
    }
    return graphicsLayer {
        alpha = progress.value.coerceIn(0f, 1f)
        translationY = (1f - progress.value) * offsetPx
    }
}

private const val MAX_STAGGERED = 8
private const val STAGGER_MILLIS = 60L
