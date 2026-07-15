package com.simpleiptv.player.core.model

enum class PlaylistSourceType(
    val title: String,
    val description: String,
    val icon: String
) {
    M3U_URL(
        title = "M3U URL",
        description = "Add a remote M3U playlist link",
        icon = "🔗"
    ),

    M3U_TEXT(
        title = "M3U Text / File",
        description = "Paste playlist text or import a file later",
        icon = "📄"
    ),

    XTREAM_CODES(
        title = "Xtream Codes",
        description = "Add server URL, username, and password",
        icon = "🔐"
    )
}