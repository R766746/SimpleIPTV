package com.simpleiptv.player.navigation

sealed class AppDestination(
    val route: String,
    val title: String,
    val navLabel: String = title
) {
    object Home : AppDestination("home", "Simple IPTV", "Home")
    object Live : AppDestination("live", "Live TV", "Live")
    object Movies : AppDestination("movies", "Movies", "Movies")
    object Series : AppDestination("series", "Series", "Series")
    object Favorites : AppDestination("favorites", "Favorites", "Favs")
    object Playlists : AppDestination("playlists", "Playlists", "Lists")
    object Settings : AppDestination("settings", "Settings", "Settings")
    object Search : AppDestination("search", "Search", "Search")
    object Player : AppDestination("player", "Player", "Player")
    object EpgTimeline : AppDestination("epg_timeline", "EPG Timeline", "EPG")
}

val BottomDestinations = listOf(
    AppDestination.Home,
    AppDestination.Live,
    AppDestination.Playlists,
    AppDestination.Favorites,
    AppDestination.Settings
)

val AllDestinations = listOf(
    AppDestination.Home,
    AppDestination.Live,
    AppDestination.Movies,
    AppDestination.Series,
    AppDestination.Favorites,
    AppDestination.Playlists,
    AppDestination.Settings,
    AppDestination.Search,
    AppDestination.Player,
    AppDestination.EpgTimeline
)