package com.pawhunt.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.pawhunt.app.R
import com.pawhunt.app.service.AdManager
import com.pawhunt.app.theme.PawPrimary
import com.pawhunt.app.theme.PawProGold
import kotlinx.coroutines.flow.StateFlow

@Composable
fun HomeScreen(
    isPro: StateFlow<Boolean>,
    adManager: AdManager?,
    onPlay: () -> Unit,
    onLibrary: () -> Unit,
    onSettings: () -> Unit,
    onPaywall: () -> Unit
) {
    val isProValue by isPro.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            PawPrimary.copy(alpha = 0.08f)
                        )
                    )
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = onSettings,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "🐾",
                fontSize = 72.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = PawPrimary
            )
            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onPlay,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PawPrimary)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("Play Now", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            FilledTonalButton(
                onClick = onLibrary,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Outlined.Palette, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Content Library")
            }

            if (!isProValue) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onPaywall,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = PawProGold)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Unlock Pro", color = PawProGold, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (!isProValue && adManager != null) {
                AndroidView(
                    factory = { adManager.createBannerAdView() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                )
            }
        }
    }
}
