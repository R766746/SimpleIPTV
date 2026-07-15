package com.simpleiptv.player.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.simpleiptv.player.navigation.AppDestination
import com.simpleiptv.player.ui.components.SimpleFeatureCard

private data class HomeFeature(
    val icon: String,
    val title: String,
    val description: String,
    val route: String
)

@Composable
fun HomeScreen(
    onOpenDestination: (String) -> Unit
) {
    val features = listOf(
        HomeFeature(
            icon = "📺",
            title = "Live TV",
            description = "Browse and watch live channels",
            route = AppDestination.Live.route
        ),
        HomeFeature(
            icon = "🎬",
            title = "Movies",
            description = "Browse movie content",
            route = AppDestination.Movies.route
        ),
        HomeFeature(
            icon = "📺",
            title = "Series",
            description = "Browse series content",
            route = AppDestination.Series.route
        ),
        HomeFeature(
            icon = "⭐",
            title = "Favorites",
            description = "Quick access to saved channels",
            route = AppDestination.Favorites.route
        ),
        HomeFeature(
            icon = "📁",
            title = "Playlists",
            description = "Manage M3U and Xtream playlists",
            route = AppDestination.Playlists.route
        ),
        HomeFeature(
            icon = "⚙",
            title = "Settings",
            description = "Configure app and playback options",
            route = AppDestination.Settings.route
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "Welcome to Simple IPTV",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Your modern IPTV player for Android TV and mobile.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val columns = if (maxWidth < 700.dp) 2 else 3
            val rows = features.chunked(columns)

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rows.forEach { rowFeatures ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        rowFeatures.forEach { feature ->
                            SimpleFeatureCard(
                                icon = feature.icon,
                                title = feature.title,
                                description = feature.description,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onOpenDestination(feature.route)
                                }
                            )
                        }

                        repeat(columns - rowFeatures.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}