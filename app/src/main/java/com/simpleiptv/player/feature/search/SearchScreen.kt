package com.simpleiptv.player.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.simpleiptv.player.core.model.Channel
import com.simpleiptv.player.core.repository.ChannelSessionStore
import com.simpleiptv.player.feature.live.LiveChannelCard

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenPlayer: (Channel) -> Unit
) {
    var searchQuery by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }

    val allChannels = remember {
        ChannelSessionStore.getChannels()
    }

    val hasChannels = allChannels.isNotEmpty()

    val searchResults = remember(searchQuery.text, allChannels) {
        val query = searchQuery.text.trim()

        if (query.isBlank() || !hasChannels) {
            emptyList()
        } else {
            allChannels.filter { channel ->
                channel.matchesSearchQuery(query)
            }
        }
    }

    val queryText = searchQuery.text.trim()

    LazyColumn(
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Search",
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = if (hasChannels) {
                        "Search across ${allChannels.size} loaded channel(s) from all sources."
                    } else {
                        "No channels loaded yet. Go to Live TV and refresh channels first."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = {
                    Text(text = "Search channels")
                },
                placeholder = {
                    Text(text = "Channel name, group, tvg-id, source...")
                }
            )
        }

        if (queryText.isNotBlank()) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${searchResults.size} result(s) for \"$queryText\"",
                        style = MaterialTheme.typography.titleMedium
                    )

                    TextButton(
                        onClick = {
                            searchQuery = TextFieldValue("")
                        }
                    ) {
                        Text(text = "Clear Search")
                    }
                }
            }

            if (searchResults.isEmpty()) {
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No channels match your search.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = "Try a different search term or shorter keyword.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(
                    items = searchResults,
                    key = { channel -> channel.id }
                ) { channel ->
                    LiveChannelCard(
                        channel = channel,
                        onClick = {
                            ChannelSessionStore.setChannels(searchResults)
                            ChannelSessionStore.selectChannel(channel)
                            onOpenPlayer(channel)
                        }
                    )
                }
            }
        } else if (hasChannels) {
            item {
                Text(
                    text = "Start typing to search across all loaded channels.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "No channels available to search.",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Open Live TV and refresh channels, then come back to search.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onBack
                    ) {
                        Text(text = "Back")
                    }
                }
            }
        }
    }
}

private fun Channel.matchesSearchQuery(query: String): Boolean {
    return name.contains(query, ignoreCase = true) ||
            groupTitle.orEmpty().contains(query, ignoreCase = true) ||
            tvgId.orEmpty().contains(query, ignoreCase = true) ||
            tvgName.orEmpty().contains(query, ignoreCase = true) ||
            playlistName.orEmpty().contains(query, ignoreCase = true) ||
            streamUrl.contains(query, ignoreCase = true)
}