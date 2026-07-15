package com.simpleiptv.player.core.model

data class PlaylistSourcePreview(
    val id: String,
    val name: String,
    val type: PlaylistSourceType,
    val description: String,
    val isEnabled: Boolean = true
)