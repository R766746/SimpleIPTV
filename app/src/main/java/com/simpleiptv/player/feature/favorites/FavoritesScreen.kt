package com.simpleiptv.player.feature.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.simpleiptv.player.core.model.Channel
import com.simpleiptv.player.core.repository.ChannelSessionStore
import com.simpleiptv.player.core.repository.FavoriteChannelStore
import com.simpleiptv.player.feature.live.LiveChannelCard

@Composable
fun FavoritesScreen(
    onOpenPlayer: (Channel) -> Unit
) {
    val context = LocalContext.current
    val favoriteStore = remember(context) {
        FavoriteChannelStore(context)
    }

    val favorites = remember {
        mutableStateListOf<Channel>()
    }

    fun reloadFavorites() {
        favorites.clear()
        favorites.addAll(favoriteStore.loadFavorites())
    }

    LaunchedEffect(Unit) {
        reloadFavorites()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Favorites",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Favorite channels saved from the player will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AssistChip(
            onClick = {},
            label = {
                Text(text = "${favorites.size} favorite channel(s)")
            }
        )

        if (favorites.isEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "No favorites yet.",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Open a channel from Live TV, then tap Add Favorite on the player screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        reloadFavorites()
                    }
                ) {
                    Text(text = "Refresh")
                }
            }
        } else {
            TextButton(
                onClick = {
                    favoriteStore.clearFavorites()
                    reloadFavorites()
                }
            ) {
                Text(text = "Clear All Favorites")
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = favorites,
                    key = { channel -> channel.id }
                ) { channel ->
                    LiveChannelCard(
                        channel = channel,
                        onClick = {
                            ChannelSessionStore.setChannels(favorites.toList())
                            ChannelSessionStore.selectChannel(channel)
                            onOpenPlayer(channel)
                        }
                    )
                }
            }
        }
    }
}