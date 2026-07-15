package com.simpleiptv.player.feature.live

import androidx.compose.runtime.Composable
import com.simpleiptv.player.ui.components.PlaceholderScreen

@Composable
fun LiveScreen(
    onOpenPlaylists: () -> Unit
) {
    PlaceholderScreen(
        icon = "📺",
        title = "Live TV",
        description = "Live channels will appear here after you add an M3U playlist or Xtream Codes account.",
        primaryActionText = "Open Playlists",
        onPrimaryAction = onOpenPlaylists
    )
}