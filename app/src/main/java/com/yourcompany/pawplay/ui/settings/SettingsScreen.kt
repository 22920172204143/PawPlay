package com.yourcompany.pawplay.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourcompany.pawplay.theme.PawPrimary

data class SettingsState(
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val speedMultiplier: Float = 1.0f,
    val preyCount: Int = 1,
    val keepScreenOn: Boolean = true,
    val hapticEnabled: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onSoundToggle: (Boolean) -> Unit,
    onMusicToggle: (Boolean) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPreyCountChange: (Int) -> Unit,
    onKeepScreenToggle: (Boolean) -> Unit,
    onHapticToggle: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader("Audio")
            SettingSwitch("Sound Effects", state.soundEnabled, onSoundToggle)
            SettingSwitch("Background Music", state.musicEnabled, onMusicToggle)

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            SectionHeader("Gameplay")

            Text(
                text = "Movement Speed",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Slow", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = state.speedMultiplier,
                    onValueChange = onSpeedChange,
                    valueRange = 0.5f..2.0f,
                    steps = 2,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(thumbColor = PawPrimary, activeTrackColor = PawPrimary)
                )
                Text("Fast", style = MaterialTheme.typography.bodySmall)
            }

            Text(
                text = "Prey Count: ${state.preyCount}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                (1..3).forEach { count ->
                    val selected = state.preyCount == count
                    androidx.compose.material3.FilterChip(
                        selected = selected,
                        onClick = { onPreyCountChange(count) },
                        label = { Text("$count") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            SectionHeader("Device")
            SettingSwitch("Keep Screen On", state.keepScreenOn, onKeepScreenToggle)
            SettingSwitch("Haptic Feedback", state.hapticEnabled, onHapticToggle)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = PawPrimary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
