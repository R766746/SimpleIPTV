package com.simpleiptv.player.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.simpleiptv.player.navigation.AppDestination
import com.simpleiptv.player.navigation.BottomDestinations

@Composable
fun SimpleBottomNavigationBar(
    currentRoute: String,
    onDestinationSelected: (AppDestination) -> Unit
) {
    NavigationBar {
        BottomDestinations.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = {
                    onDestinationSelected(destination)
                },
                icon = {
                    Icon(
                        imageVector = destination.icon(),
                        contentDescription = destination.title
                    )
                },
                label = {
                    Text(
                        text = destination.navLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

private fun AppDestination.icon(): ImageVector {
    return when (this) {
        AppDestination.Home -> Icons.Filled.Home
        AppDestination.Live -> Icons.Filled.PlayArrow
        AppDestination.Playlists -> Icons.Filled.Menu
        AppDestination.Favorites -> Icons.Filled.Favorite
        AppDestination.Settings -> Icons.Filled.Settings
        else -> Icons.Filled.Home
    }
}