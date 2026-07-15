package com.simpleiptv.player.feature.series

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.simpleiptv.player.core.model.Channel
import com.simpleiptv.player.core.model.PlaylistSourceType
import com.simpleiptv.player.core.model.XtreamCredentials
import com.simpleiptv.player.core.model.XtreamEpisode
import com.simpleiptv.player.core.model.XtreamEpisodeInfo
import com.simpleiptv.player.core.model.XtreamSeason
import com.simpleiptv.player.core.model.XtreamSeriesInfo
import com.simpleiptv.player.core.network.XtreamApiClient
import com.simpleiptv.player.core.repository.ChannelSessionStore
import com.simpleiptv.player.core.repository.PlaylistSourcePreviewStore
import com.simpleiptv.player.core.repository.XtreamCredentialsStore
import com.simpleiptv.player.ui.components.ChannelLogo

@Composable
fun SeriesDetailScreen(
    seriesInfo: XtreamSeriesInfo,
    onBack: () -> Unit,
    onPlayEpisode: (Channel) -> Unit
) {
    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(true) }
    var seasons by remember { mutableStateOf<List<XtreamSeason>>(emptyList()) }
    var selectedSeason by remember { mutableStateOf<Int?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var seriesCover by remember { mutableStateOf(seriesInfo.cover) }
    var seriesPlot by remember { mutableStateOf(seriesInfo.plot) }
    var credentials by remember { mutableStateOf<XtreamCredentials?>(null) }
    var sourceId by remember { mutableStateOf<String?>(null) }
    var sourceName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(seriesInfo.seriesId) {
        isLoading = true
        errorMessage = null

        val playlistStore = PlaylistSourcePreviewStore(context)
        val xtreamStore = XtreamCredentialsStore(context)

        val xtreamSources = playlistStore.load().filter {
            it.isEnabled && it.type == PlaylistSourceType.XTREAM_CODES
        }

        var loaded = false

        for (source in xtreamSources) {
            val creds = xtreamStore.getBySourceId(source.id) ?: continue

            XtreamApiClient.getSeriesInfo(creds, seriesInfo.seriesId)
                .onSuccess { json ->
                    credentials = creds
                    sourceId = source.id
                    sourceName = source.name

                    val info = json.optJSONObject("info")
                    if (info != null) {
                        seriesCover = info.optString("cover", "").ifBlank { seriesInfo.cover }
                        seriesPlot = info.optString("plot", "").ifBlank { seriesInfo.plot }
                    }

                    val episodesObj = json.optJSONObject("episodes")
                    val parsedSeasons = mutableListOf<XtreamSeason>()

                    if (episodesObj != null) {
                        val seasonKeys = episodesObj.keys()

                        while (seasonKeys.hasNext()) {
                            val seasonKey = seasonKeys.next()
                            val seasonNum = seasonKey.toIntOrNull() ?: continue
                            val episodesArray = episodesObj.optJSONArray(seasonKey) ?: continue

                            val episodeList = mutableListOf<XtreamEpisode>()

                            for (index in 0 until episodesArray.length()) {
                                val epItem = episodesArray.optJSONObject(index) ?: continue

                                val epId = epItem.optString("id", "").trim()
                                val epNum = epItem.optInt("episode_num", index + 1)
                                val epTitle = epItem.optString("title", "Episode $epNum").trim()
                                val epExtension = epItem.optString("container_extension", "mp4").trim().ifBlank { "mp4" }

                                val epInfoObj = epItem.optJSONObject("info")
                                val epInfo = if (epInfoObj != null) {
                                    XtreamEpisodeInfo(
                                        plot = epInfoObj.optString("plot", "").ifBlank { null },
                                        duration = epInfoObj.optString("duration", "").ifBlank { null },
                                        rating = epInfoObj.optString("rating", "").ifBlank { null },
                                        coverBig = epInfoObj.optString("cover_big", "").ifBlank { null }
                                    )
                                } else null

                                episodeList.add(
                                    XtreamEpisode(
                                        id = epId,
                                        episodeNum = epNum,
                                        title = epTitle,
                                        containerExtension = epExtension,
                                        seasonNumber = seasonNum,
                                        info = epInfo
                                    )
                                )
                            }

                            if (episodeList.isNotEmpty()) {
                                parsedSeasons.add(
                                    XtreamSeason(
                                        seasonNumber = seasonNum,
                                        episodes = episodeList.sortedBy { it.episodeNum }
                                    )
                                )
                            }
                        }
                    }

                    seasons = parsedSeasons.sortedBy { it.seasonNumber }

                    if (seasons.isNotEmpty()) {
                        selectedSeason = seasons.first().seasonNumber
                    }

                    loaded = true
                }
                .onFailure { throwable ->
                    errorMessage = "${source.name}: ${throwable.message}"
                }

            if (loaded) break
        }

        if (!loaded && errorMessage == null) {
            errorMessage = "Could not load series details from any Xtream source."
        }

        isLoading = false
    }

    val currentEpisodes = remember(seasons, selectedSeason) {
        seasons.firstOrNull { it.seasonNumber == selectedSeason }?.episodes.orEmpty()
    }

    LazyColumn(
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ChannelLogo(
                    logoUrl = seriesCover,
                    channelName = seriesInfo.name,
                    size = 80.dp
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = seriesInfo.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    seriesInfo.rating?.let {
                        Text(
                            text = "Rating: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "${seasons.size} season(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        seriesPlot?.takeIf { it.isNotBlank() }?.let { plot ->
            item {
                Text(
                    text = plot,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Button(onClick = onBack) {
                Text(text = "Back to Series")
            }
        }

        if (isLoading) {
            item { CircularProgressIndicator() }
        }

        errorMessage?.let { msg ->
            item {
                Text(
                    text = "Error: $msg",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        if (seasons.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Seasons",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        seasons.forEach { season ->
                            FilterChip(
                                selected = selectedSeason == season.seasonNumber,
                                onClick = { selectedSeason = season.seasonNumber },
                                label = { Text(text = "Season ${season.seasonNumber}") }
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "${currentEpisodes.size} episode(s) in Season $selectedSeason",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            items(
                items = currentEpisodes,
                key = { ep -> "s${ep.seasonNumber}_e${ep.episodeNum}_${ep.id}" }
            ) { episode ->
                EpisodeCard(
                    episode = episode,
                    seriesName = seriesInfo.name,
                    onClick = {
                        val creds = credentials ?: return@EpisodeCard
                        val normalized = creds.serverUrl.trimEnd('/')
                        val streamUrl = "$normalized/series/${creds.username}/${creds.password}/${episode.id}.${episode.containerExtension}"

                        val channel = Channel(
                            id = "series_ep_${episode.id}",
                            name = "${seriesInfo.name} S${episode.seasonNumber}E${episode.episodeNum} - ${episode.title}",
                            streamUrl = streamUrl,
                            tvgId = null,
                            tvgName = episode.title,
                            logoUrl = episode.info?.coverBig ?: seriesCover,
                            groupTitle = "Series",
                            playlistSourceId = sourceId,
                            playlistName = sourceName
                        )

                        ChannelSessionStore.setChannels(
                            currentEpisodes.map { ep ->
                                val epUrl = "$normalized/series/${creds.username}/${creds.password}/${ep.id}.${ep.containerExtension}"
                                Channel(
                                    id = "series_ep_${ep.id}",
                                    name = "${seriesInfo.name} S${ep.seasonNumber}E${ep.episodeNum} - ${ep.title}",
                                    streamUrl = epUrl,
                                    tvgId = null,
                                    tvgName = ep.title,
                                    logoUrl = ep.info?.coverBig ?: seriesCover,
                                    groupTitle = "Series",
                                    playlistSourceId = sourceId,
                                    playlistName = sourceName
                                )
                            }
                        )
                        ChannelSessionStore.selectChannel(channel)
                        onPlayEpisode(channel)
                    }
                )
            }
        } else if (!isLoading && errorMessage == null) {
            item {
                Text(
                    text = "No episodes found for this series.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EpisodeCard(
    episode: XtreamEpisode,
    seriesName: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChannelLogo(
                logoUrl = episode.info?.coverBig,
                channelName = episode.title,
                size = 54.dp
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "E${episode.episodeNum} - ${episode.title}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                episode.info?.duration?.let {
                    Text(
                        text = "Duration: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                episode.info?.rating?.let {
                    Text(
                        text = "Rating: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                episode.info?.plot?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }

            Text(
                text = "▶ Play",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}