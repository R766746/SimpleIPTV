package com.simpleiptv.player.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.simpleiptv.player.core.repository.ChannelSessionStore
import com.simpleiptv.player.ui.components.PlaceholderScreen

@Composable
fun PlayerScreen(
    onBack: () -> Unit
) {
    val channel = ChannelSessionStore.selectedChannel

    if (channel == null) {
        PlaceholderScreen(
            icon = "▶",
            title = "No Channel Selected",
            description = "Go back to Live TV and choose a channel.",
            primaryActionText = "Back",
            onPrimaryAction = onBack
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Media3 Player Coming Next",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = channel.groupTitle?.takeIf { it.isNotBlank() } ?: "No group",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            channel.playlistName?.let { playlistName ->
                Text(
                    text = "Source: $playlistName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            channel.tvgId?.let { tvgId ->
                Text(
                    text = "TVG ID: $tvgId",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Stream URL",
                style = MaterialTheme.typography.titleSmall
            )

            Text(
                text = channel.streamUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Button(
            onClick = onBack
        ) {
            Text(text = "Back to Live TV")
        }
    }
}