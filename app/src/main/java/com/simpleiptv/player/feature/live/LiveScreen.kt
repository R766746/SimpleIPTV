package com.simpleiptv.player.feature.live

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.simpleiptv.player.core.model.Channel
import com.simpleiptv.player.core.model.PlaylistSourceType
import com.simpleiptv.player.core.network.M3uPlaylistFetcher
import com.simpleiptv.player.core.repository.ChannelSessionStore
import com.simpleiptv.player.core.repository.PlaylistSourcePreviewStore
import com.simpleiptv.player.core.util.M3uPlaylistParser
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveScreen(
    onOpenPlaylists: () -> Unit,
    onOpenPlayer: (Channel) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember {
        mutableStateOf(false)
    }

    var channels by remember {
        mutableStateOf<List<Channel>>(emptyList())
    }

    var selectedGroup by remember {
        mutableStateOf<String?>(null)
    }

    var selectedSourceName by remember {
        mutableStateOf<String?>(null)
    }

    var searchQuery by remember {
        mutableStateOf(TextFieldValue(""))
    }

    var statusMessage by remember {
        mutableStateOf<String?>(null)
    }

    var warnings by remember {
        mutableStateOf<List<String>>(emptyList())
    }

    suspend fun refreshChannels() {
        isLoading = true
        statusMessage = null
        warnings = emptyList()

        val result = loadChannelsFromSavedM3uUrls(context = context)

        channels = result.channels
        warnings = result.warnings

        ChannelSessionStore.setChannels(result.channels)

        statusMessage = when {
            result.sourceCount == 0 -> "No enabled M3U URL playlist sources found."
            result.channels.isEmpty() -> "No channels were parsed from enabled M3U URL sources."
            else -> "Loaded ${result.channels.size} channels from ${result.sourceCount} source(s)."
        }

        selectedGroup = null
        selectedSourceName = null
        isLoading = false
    }

    LaunchedEffect(Unit) {
        refreshChannels()
    }

    val sourceNames = remember(channels) {
        channels
            .mapNotNull { channel ->
                channel.playlistName?.takeIf { it.isNotBlank() }
            }
            .distinct()
            .sorted()
    }

    val groups = remember(channels, selectedSourceName) {
        channels
            .asSequence()
            .filter { channel ->
                selectedSourceName == null || channel.playlistName == selectedSourceName
            }
            .map { groupName(it) }
            .distinct()
            .sorted()
            .toList()
    }

    val visibleChannels = remember(
        channels,
        selectedGroup,
        selectedSourceName,
        searchQuery
    ) {
        val query = searchQuery.text.trim()

        channels
            .asSequence()
            .filter { channel ->
                selectedSourceName == null || channel.playlistName == selectedSourceName
            }
            .filter { channel ->
                selectedGroup == null || groupName(channel) == selectedGroup
            }
            .filter { channel ->
                query.isBlank() || channel.matchesQuery(query)
            }
            .toList()
    }

    val hasActiveFilters =
        selectedGroup != null ||
                selectedSourceName != null ||
                searchQuery.text.isNotBlank()

    LazyColumn(
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Live TV",
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = "Channels are loaded from enabled M3U URL playlist sources. You can search, filter by playlist source, and filter by group.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    enabled = !isLoading,
                    onClick = {
                        scope.launch {
                            refreshChannels()
                        }
                    }
                ) {
                    Text(
                        text = if (isLoading) {
                            "Loading..."
                        } else {
                            "Refresh Channels"
                        }
                    )
                }

                TextButton(
                    onClick = onOpenPlaylists
                ) {
                    Text(text = "Open Playlists")
                }

                if (hasActiveFilters) {
                    TextButton(
                        onClick = {
                            selectedGroup = null
                            selectedSourceName = null
                            searchQuery = TextFieldValue("")
                        }
                    ) {
                        Text(text = "Clear Filters")
                    }
                }
            }
        }

        if (isLoading) {
            item {
                CircularProgressIndicator()
            }
        }

        statusMessage?.let { message ->
            item {
                AssistChip(
                    onClick = {},
                    label = {
                        Text(text = message)
                    }
                )
            }
        }

        if (warnings.isNotEmpty()) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Warnings",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    warnings.take(4).forEach { warning ->
                        Text(
                            text = "• $warning",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (warnings.size > 4) {
                        Text(
                            text = "• ${warnings.size - 4} more warning(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (channels.isNotEmpty()) {
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
                        Text(text = "Search by channel name, group, tvg-id, or playlist")
                    }
                )
            }

            if (sourceNames.isNotEmpty()) {
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Playlist Source",
                            style = MaterialTheme.typography.titleSmall
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedSourceName == null,
                                onClick = {
                                    selectedSourceName = null
                                    selectedGroup = null
                                },
                                label = {
                                    Text(text = "All Sources")
                                }
                            )

                            sourceNames.forEach { sourceName ->
                                FilterChip(
                                    selected = selectedSourceName == sourceName,
                                    onClick = {
                                        selectedSourceName = sourceName
                                        selectedGroup = null
                                    },
                                    label = {
                                        Text(text = sourceName)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (groups.isNotEmpty()) {
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Group",
                            style = MaterialTheme.typography.titleSmall
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedGroup == null,
                                onClick = {
                                    selectedGroup = null
                                },
                                label = {
                                    Text(text = "All Groups")
                                }
                            )

                            groups.forEach { group ->
                                FilterChip(
                                    selected = selectedGroup == group,
                                    onClick = {
                                        selectedGroup = group
                                    },
                                    label = {
                                        Text(text = group)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                ChannelCountSummary(
                    totalChannels = channels.size,
                    visibleChannels = visibleChannels.size,
                    selectedSourceName = selectedSourceName,
                    selectedGroup = selectedGroup,
                    searchQuery = searchQuery.text
                )
            }

            if (visibleChannels.isEmpty()) {
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No channels match your filters.",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "Try clearing search text, selecting All Sources, or selecting All Groups.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(
                    items = visibleChannels,
                    key = { channel -> channel.id }
                ) { channel ->
                    LiveChannelCard(
                        channel = channel,
                        onClick = {
                            ChannelSessionStore.setChannels(visibleChannels)
                            ChannelSessionStore.selectChannel(channel)
                            onOpenPlayer(channel)
                        }
                    )
                }
            }
        } else if (!isLoading) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No live channels yet.",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Go to Playlists, add an enabled M3U URL source, then return here and refresh.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelCountSummary(
    totalChannels: Int,
    visibleChannels: Int,
    selectedSourceName: String?,
    selectedGroup: String?,
    searchQuery: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "$visibleChannels of $totalChannels channel(s)",
            style = MaterialTheme.typography.titleMedium
        )

        val filters = buildList {
            selectedSourceName?.let {
                add("Source: $it")
            }

            selectedGroup?.let {
                add("Group: $it")
            }

            if (searchQuery.isNotBlank()) {
                add("Search: ${searchQuery.trim()}")
            }
        }

        if (filters.isNotEmpty()) {
            Text(
                text = filters.joinToString(separator = " • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private suspend fun loadChannelsFromSavedM3uUrls(
    context: android.content.Context
): LiveChannelLoadResult {
    val playlistStore = PlaylistSourcePreviewStore(context)

    val sources = playlistStore
        .load()
        .filter { playlist ->
            playlist.isEnabled && playlist.type == PlaylistSourceType.M3U_URL
        }

    if (sources.isEmpty()) {
        return LiveChannelLoadResult(
            channels = emptyList(),
            warnings = emptyList(),
            sourceCount = 0
        )
    }

    val allChannels = mutableListOf<Channel>()
    val warnings = mutableListOf<String>()

    sources.forEach { source ->
        val url = source.description.trim()

        if (url.isBlank()) {
            warnings.add("${source.name}: empty M3U URL.")
            return@forEach
        }

        val fetchResult = M3uPlaylistFetcher.fetch(url)

        fetchResult
            .onSuccess { rawPlaylist ->
                val parseResult = M3uPlaylistParser.parse(rawPlaylist)

                warnings.addAll(
                    parseResult.warnings.map { warning ->
                        "${source.name}: $warning"
                    }
                )

                val sourceChannels = parseResult.channels.map { channel ->
                    channel.copy(
                        playlistSourceId = source.id,
                        playlistName = source.name
                    )
                }

                allChannels.addAll(sourceChannels)
            }
            .onFailure { throwable ->
                warnings.add(
                    "${source.name}: ${throwable.message ?: "Failed to load playlist."}"
                )
            }
    }

    val dedupedChannels = allChannels
        .distinctBy { channel ->
            channel.streamUrl
        }
        .sortedWith(
            compareBy<Channel> { groupName(it) }
                .thenBy { it.name.lowercase() }
        )

    return LiveChannelLoadResult(
        channels = dedupedChannels,
        warnings = warnings,
        sourceCount = sources.size
    )
}

private fun Channel.matchesQuery(query: String): Boolean {
    return name.contains(query, ignoreCase = true) ||
            streamUrl.contains(query, ignoreCase = true) ||
            groupTitle.orEmpty().contains(query, ignoreCase = true) ||
            tvgId.orEmpty().contains(query, ignoreCase = true) ||
            tvgName.orEmpty().contains(query, ignoreCase = true) ||
            playlistName.orEmpty().contains(query, ignoreCase = true)
}

private fun groupName(channel: Channel): String {
    return channel.groupTitle
        ?.takeIf { it.isNotBlank() }
        ?: "Other"
}

private data class LiveChannelLoadResult(
    val channels: List<Channel>,
    val warnings: List<String>,
    val sourceCount: Int
)