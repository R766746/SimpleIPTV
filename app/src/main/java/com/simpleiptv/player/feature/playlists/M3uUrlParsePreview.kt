package com.simpleiptv.player.feature.playlists

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.simpleiptv.player.core.model.Channel
import com.simpleiptv.player.core.network.M3uPlaylistFetcher
import com.simpleiptv.player.core.util.M3uParseResult
import com.simpleiptv.player.core.util.M3uPlaylistParser
import kotlinx.coroutines.launch

@Composable
fun M3uUrlParsePreview(
    m3uUrl: String,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    var isLoading by remember {
        mutableStateOf(false)
    }

    var downloadedInfo by remember {
        mutableStateOf<String?>(null)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var parseResult by remember {
        mutableStateOf<M3uParseResult?>(null)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "M3U URL Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Enter an M3U URL above, then fetch and preview how many channels Simple IPTV can detect.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                enabled = m3uUrl.isNotBlank() && !isLoading,
                onClick = {
                    scope.launch {
                        isLoading = true
                        downloadedInfo = null
                        errorMessage = null
                        parseResult = null

                        val fetchResult = M3uPlaylistFetcher.fetch(m3uUrl)

                        fetchResult
                            .onSuccess { rawPlaylist ->
                                downloadedInfo = "Downloaded ${formatBytes(rawPlaylist.toByteArray().size)}"

                                parseResult = M3uPlaylistParser.parse(rawPlaylist)
                            }
                            .onFailure { throwable ->
                                errorMessage = throwable.message ?: "Failed to fetch playlist."
                            }

                        isLoading = false
                    }
                }
            ) {
                Text(
                    text = if (isLoading) {
                        "Fetching..."
                    } else {
                        "Fetch and Preview"
                    }
                )
            }

            if (isLoading) {
                CircularProgressIndicator()
            }

            downloadedInfo?.let { info ->
                AssistChip(
                    onClick = {},
                    label = {
                        Text(text = info)
                    }
                )
            }

            errorMessage?.let { message ->
                Text(
                    text = "Error: $message",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            parseResult?.let { result ->
                AssistChip(
                    onClick = {},
                    label = {
                        Text(text = "${result.channelCount} channels parsed")
                    }
                )

                if (result.warnings.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Warnings",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        result.warnings.take(3).forEach { warning ->
                            Text(
                                text = "• $warning",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (result.warnings.size > 3) {
                            Text(
                                text = "• ${result.warnings.size - 3} more warnings",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (result.channels.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "First channels",
                            style = MaterialTheme.typography.titleSmall
                        )

                        result.channels.take(5).forEach { channel ->
                            ParsedUrlChannelRow(channel = channel)
                        }

                        if (result.channels.size > 5) {
                            Text(
                                text = "+ ${result.channels.size - 5} more channels",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParsedUrlChannelRow(
    channel: Channel
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = channel.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = buildString {
                append(channel.groupTitle ?: "No group")

                channel.tvgId?.let {
                    append(" • tvg-id: ")
                    append(it)
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = channel.streamUrl,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

private fun formatBytes(bytes: Int): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "%.2f MB".format(bytes / 1024f / 1024f)
    }
}