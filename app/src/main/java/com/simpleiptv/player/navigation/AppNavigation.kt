package com.simpleiptv.player.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.simpleiptv.player.feature.favorites.FavoritesScreen
import com.simpleiptv.player.feature.home.HomeScreen
import com.simpleiptv.player.feature.live.LiveScreen
import com.simpleiptv.player.feature.movies.MoviesScreen
import com.simpleiptv.player.feature.player.PlayerScreen
import com.simpleiptv.player.feature.playlists.PlaylistsScreen
import com.simpleiptv.player.feature.search.SearchScreen
import com.simpleiptv.player.feature.series.SeriesScreen
import com.simpleiptv.player.feature.settings.SettingsScreen
import com.simpleiptv.player.feature.player.EpgTimelineScreen
import com.simpleiptv.player.core.repository.ChannelSessionStore
import com.simpleiptv.player.core.repository.ThemeMode
import com.simpleiptv.player.core.model.Channel
import com.simpleiptv.player.ui.components.SimpleBottomNavigationBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleIPTVApp(
    onThemeChanged: (ThemeMode) -> Unit = {}
) {
    val navController = rememberNavController()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppDestination.Home.route

    val currentTitle = AllDestinations
        .firstOrNull { it.route == currentRoute }
        ?.title ?: AppDestination.Home.title

    val showAppChrome = currentRoute != AppDestination.Player.route &&
            currentRoute != AppDestination.EpgTimeline.route

    Scaffold(
        topBar = {
            if (showAppChrome) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(text = currentTitle)
                    },
                    actions = {
                        if (currentRoute != AppDestination.Search.route) {
                            IconButton(
                                onClick = {
                                    navController.navigateSingleTopTo(AppDestination.Search.route)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Search"
                                )
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (showAppChrome) {
                SimpleBottomNavigationBar(
                    currentRoute = currentRoute,
                    onDestinationSelected = { destination ->
                        navController.navigateSingleTopTo(destination.route)
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route,
            modifier = if (showAppChrome) {
                Modifier.padding(innerPadding)
            } else {
                Modifier
            }
        ) {
            composable(AppDestination.Home.route) {
                HomeScreen(
                    onOpenDestination = { route ->
                        navController.navigateSingleTopTo(route)
                    }
                )
            }

            composable(AppDestination.Live.route) {
                LiveScreen(
                    onOpenPlaylists = {
                        navController.navigateSingleTopTo(AppDestination.Playlists.route)
                    },
                    onOpenPlayer = {
                        navController.navigate(AppDestination.Player.route)
                    }
                )
            }

            composable(AppDestination.Movies.route) {
                MoviesScreen()
            }

            composable(AppDestination.Series.route) {
                SeriesScreen()
            }

            composable(AppDestination.Favorites.route) {
                FavoritesScreen(
                    onOpenPlayer = {
                        navController.navigate(AppDestination.Player.route)
                    }
                )
            }

            composable(AppDestination.Playlists.route) {
                PlaylistsScreen()
            }

            composable(AppDestination.Settings.route) {
                SettingsScreen(
                    onThemeChanged = onThemeChanged
                )
            }

            composable(AppDestination.Search.route) {
                SearchScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                    onOpenPlayer = {
                        navController.navigate(AppDestination.Player.route)
                    }
                )
            }

            composable(AppDestination.Player.route) {
                PlayerScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                    onOpenEpgTimeline = {
                        navController.navigate(AppDestination.EpgTimeline.route)
                    }
                )
            }
            composable(AppDestination.EpgTimeline.route) {
                val channel = ChannelSessionStore.selectedChannel

                if (channel != null) {
                    EpgTimelineScreen(
                        channel = channel,
                        onBack = {
                            navController.popBackStack()
                        },
                        onPlayCatchup = { catchupInfo ->
                            ChannelSessionStore.selectChannel(
                                Channel(
                                    id = "catchup_${catchupInfo.program.startTimeMillis}",
                                    name = "${catchupInfo.channelName} - ${catchupInfo.program.title}",
                                    streamUrl = catchupInfo.catchupUrl,
                                    tvgId = null,
                                    tvgName = null,
                                    logoUrl = null,
                                    groupTitle = "Catch-up",
                                    playlistSourceId = null,
                                    playlistName = "Catch-up"
                                )
                            )
                            navController.navigate(AppDestination.Player.route)
                        }
                    )
                } else {
                    com.simpleiptv.player.ui.components.PlaceholderScreen(
                        icon = "📺",
                        title = "No Channel Selected",
                        description = "Go back to Live TV and select a channel first.",
                        primaryActionText = "Back",
                        onPrimaryAction = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}

private fun NavHostController.navigateSingleTopTo(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }

        launchSingleTop = true
        restoreState = true
    }
}