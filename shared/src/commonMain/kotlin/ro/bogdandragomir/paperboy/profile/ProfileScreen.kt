package ro.bogdandragomir.paperboy.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ro.bogdandragomir.paperboy.data.local.ThemeMode

private val morningHours = listOf(6, 7, 8, 9)

@Composable
fun ProfileRoute(
    onEditTopics: () -> Unit,
    onOpenSaved: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(
        uiState = uiState,
        onEditTopics = onEditTopics,
        onOpenSaved = onOpenSaved,
        onThemeMode = viewModel::setThemeMode,
        onMorningHour = viewModel::setMorningHour,
        onMorningNotification = viewModel::setMorningNotification,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onEditTopics: () -> Unit,
    onOpenSaved: () -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onMorningHour: (Int) -> Unit,
    onMorningNotification: (Boolean) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                ).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Profile", style = MaterialTheme.typography.displaySmall)

        Section("Account") {
            Text("Not signed in", style = MaterialTheme.typography.titleMedium)
            Hint(
                "Accounts arrive with the online version of Paperboy. Until then, your topics and saved stories " +
                    "stay on this phone.",
            )
        }

        Section("Your topics") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                uiState.topicNames.forEach { name -> AssistChip(onClick = onEditTopics, label = { Text(name) }) }
            }
            OutlinedButton(onClick = onEditTopics) { Text("Edit topics") }
        }

        Section("Saved") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text =
                        when (uiState.savedCount) {
                            0 -> "No saved stories yet"
                            1 -> "1 saved story"
                            else -> "${uiState.savedCount} saved stories"
                        },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = onOpenSaved) { Text("Open") }
            }
        }

        Section("Your morning") {
            Text("Brief ready at", style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                morningHours.forEach { hour ->
                    FilterChip(
                        selected = uiState.settings.morningHour == hour,
                        onClick = { onMorningHour(hour) },
                        label = { Text("$hour:00") },
                    )
                }
            }
            SettingSwitch(
                title = "Morning notification",
                hint = "A gentle nudge when your brief is ready. Notifications start when Paperboy goes online.",
                checked = uiState.settings.morningNotification,
                onCheckedChange = onMorningNotification,
            )
            SettingSwitch(
                title = "Email me my brief",
                hint = "Needs an account, coming soon.",
                checked = uiState.settings.emailDigest,
                onCheckedChange = {},
                enabled = false,
            )
        }

        Section("Appearance") {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = uiState.settings.themeMode == mode,
                        onClick = { onThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                        label = { Text(mode.name) },
                    )
                }
            }
        }

        Section("About") {
            Text("Paperboy 0.1.0", style = MaterialTheme.typography.bodyLarge)
            Hint(
                "Headlines and summaries come from each publisher's RSS feed; full articles open on their site. " +
                    "No tracking: your topics and saved stories never leave your phone.",
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    hint: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Hint(hint)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
