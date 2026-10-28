package ro.bogdandragomir.paperboy.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ro.bogdandragomir.paperboy.core.model.Topic

/**
 * First-run topic picker, and the "edit topics" screen later on.
 * [onDone] runs after the choices are saved (or when editing is cancelled).
 */
@Composable
fun OnboardingRoute(
    isEditing: Boolean,
    onDone: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    OnboardingScreen(
        uiState = uiState,
        isEditing = isEditing,
        onToggle = viewModel::toggle,
        onSave = { viewModel.save(onDone) },
        onRetry = viewModel::load,
        onCancel = onDone,
    )
}

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    isEditing: Boolean,
    onToggle: (String) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Paperboy",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            if (isEditing) TextButton(onClick = onCancel) { Text("Cancel") }
        }
        Text(
            text = if (isEditing) "Your topics" else "What's your morning made of?",
            style = MaterialTheme.typography.displaySmall,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Pick at least $MIN_TOPICS. You can change them anytime.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                uiState.loadFailed ->
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Couldn't load the topics.", style = MaterialTheme.typography.bodyLarge)
                        TextButton(onClick = onRetry) { Text("Try again") }
                    }
                else -> TopicBubbles(uiState, onToggle)
            }
        }

        Spacer(Modifier.height(16.dp))
        val missing = MIN_TOPICS - uiState.selected.size
        Button(
            onClick = onSave,
            enabled = uiState.canSave,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text(
                when {
                    missing > 0 -> "Pick $missing more"
                    isEditing -> "Save topics"
                    else -> "Build my brief"
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopicBubbles(
    uiState: OnboardingUiState,
    onToggle: (String) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        uiState.visibleTopics.forEach { topic ->
            key(topic.id) {
                TopicBubble(
                    topic = topic,
                    selected = topic.id in uiState.selected,
                    onToggle = { onToggle(topic.id) },
                )
            }
        }
    }
}

@Composable
private fun TopicBubble(
    topic: Topic,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    val isSubTopic = topic.parentId != null
    // Slightly different sizes make the grid feel organic rather than like a form.
    val diameter = if (isSubTopic) 84.dp else (96 + (topic.name.length % 4) * 8).dp
    val bounce = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
    val selectionScale by animateFloatAsState(if (selected) 1.08f else 1f, bounce)
    val appear = remember { Animatable(if (isSubTopic) 0.4f else 1f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, bounce) }
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
    )
    val content by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(diameter)
                .graphicsLayer {
                    scaleX = selectionScale * appear.value
                    scaleY = selectionScale * appear.value
                }.clip(CircleShape)
                .background(container)
                .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() })
                .padding(8.dp),
    ) {
        Text(
            text = topic.name,
            color = content,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}
