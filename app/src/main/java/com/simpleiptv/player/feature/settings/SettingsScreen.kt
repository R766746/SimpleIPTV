package com.simpleiptv.player.feature.settings

import androidx.compose.runtime.Composable
import com.simpleiptv.player.ui.components.PlaceholderScreen

@Composable
fun SettingsScreen() {
    PlaceholderScreen(
        icon = "⚙",
        title = "Settings",
        description = "Playback, cache, EPG, theme, and remote-control options will be configured here."
    )
}