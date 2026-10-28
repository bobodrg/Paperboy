package ro.bogdandragomir.paperboy.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Paperboy's mascot for now: a rolled-up newspaper with a red band, drawn in code so it follows
 * light and dark themes. A designed character (Lottie) can replace it later.
 */
@Composable
fun RolledNewspaper(
    modifier: Modifier = Modifier,
    width: Dp = 72.dp,
) {
    val paper = MaterialTheme.colorScheme.surface
    val ink = MaterialTheme.colorScheme.onSurface
    val band = MaterialTheme.colorScheme.primary
    Canvas(modifier.size(width = width, height = width * 0.42f)) {
        val stroke = 1.5.dp.toPx()
        val h = size.height
        val radius = CornerRadius(h / 2, h / 2)
        // The roll.
        drawRoundRect(color = paper, cornerRadius = radius)
        // Printed lines peeking out.
        val lineColor = ink.copy(alpha = 0.28f)
        listOf(0.30f, 0.50f, 0.70f).forEach { y ->
            drawLine(lineColor, Offset(h * 0.55f, h * y), Offset(size.width * 0.38f, h * y), strokeWidth = stroke)
            drawLine(
                lineColor,
                Offset(size.width * 0.62f, h * y),
                Offset(size.width - h * 0.7f, h * y),
                strokeWidth = stroke,
            )
        }
        // The open end of the roll.
        drawOval(
            color = ink.copy(alpha = 0.12f),
            topLeft = Offset(size.width - h * 0.55f, stroke),
            size = Size(h * 0.45f, h - 2 * stroke),
        )
        // Rubber band.
        drawRoundRect(
            color = band,
            topLeft = Offset(size.width * 0.44f, -stroke),
            size = Size(size.width * 0.12f, h + 2 * stroke),
            cornerRadius = CornerRadius(stroke * 2, stroke * 2),
        )
        // Outline.
        drawRoundRect(color = ink, cornerRadius = radius, style = Stroke(width = stroke))
    }
}

/** Pull-to-refresh indicator: the newspaper spins as you pull, and keeps spinning while the brief downloads. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewspaperRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    val travelPx = with(LocalDensity.current) { 72.dp.toPx() }
    val spin = rememberInfiniteTransition()
    val spinning by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing)),
    )
    val pull = if (isRefreshing) 1f else state.distanceFraction.coerceIn(0f, 1.2f)
    Box(
        modifier =
            modifier
                .padding(top = 8.dp)
                .graphicsLayer {
                    translationY = pull * travelPx - 40.dp.toPx()
                    alpha = (pull * 1.5f).coerceIn(0f, 1f)
                    rotationZ = if (isRefreshing) spinning else pull * 300f
                },
    ) {
        RolledNewspaper(width = 56.dp)
    }
}

/** The end of the brief: the paperboy's throw lands on the doorstep with a little bounce. */
@Composable
fun TossedNewspaper(modifier: Modifier = Modifier) {
    if (LocalReduceMotion.current) {
        RolledNewspaper(modifier.graphicsLayer { rotationZ = -8f })
        return
    }
    val dropPx = with(LocalDensity.current) { 90.dp.toPx() }
    val fall = remember { Animatable(0f) }
    val spin = remember { Animatable(-160f) }
    LaunchedEffect(Unit) {
        launch {
            fall.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            )
        }
        spin.animateTo(-8f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessVeryLow))
    }
    RolledNewspaper(
        modifier.graphicsLayer {
            translationY = (1f - fall.value) * -dropPx
            rotationZ = spin.value
            alpha = fall.value.coerceIn(0f, 1f)
        },
    )
}

/** A placeholder card with a soft shimmer, shown while the first brief downloads. */
@Composable
fun SkeletonStoryCard(modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val shimmer = rememberInfiniteTransition()
    val x by shimmer.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1300, easing = LinearEasing), RepeatMode.Restart),
    )
    val brush =
        if (LocalReduceMotion.current) {
            Brush.linearGradient(listOf(base, base))
        } else {
            Brush.linearGradient(
                colors = listOf(base, highlight, base),
                start = Offset(x * 800f, 0f),
                end = Offset(x * 800f + 400f, 400f),
            )
        }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(brush))
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Bar(brush, widthFraction = 0.35f, height = 12.dp)
            Bar(brush, widthFraction = 0.9f, height = 22.dp)
            Bar(brush, widthFraction = 0.7f, height = 22.dp)
            Bar(brush, widthFraction = 0.95f, height = 12.dp)
        }
    }
}

@Composable
private fun Bar(
    brush: Brush,
    widthFraction: Float,
    height: Dp,
) {
    Box(
        Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(brush),
    )
}
