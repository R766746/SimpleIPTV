package com.simpleiptv.player.feature.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.simpleiptv.player.core.model.Channel
import com.simpleiptv.player.core.model.CatchupInfo
import com.simpleiptv.player.core.model.EpgProgram
import com.simpleiptv.player.core.repository.EpgSessionStore
import com.simpleiptv.player.core.util.CatchupResolver
import com.simpleiptv.player.ui.components.ChannelLogo

@Composable
fun EpgTimelineScreen(
    channel: Channel,
    onBack: () -> Unit,
    onPlayCatchup: (CatchupInfo) -> Unit
) {
    val context = LocalContext.current

    val tvgId = channel.tvgId

    val programs = remember(tvgId) {
        if (tvgId != null) {
            EpgSessionStore.getProgramsForChannel(tvgId)
        } else {
            emptyList()
        }
    }

    val catchupAvailable = remember(channel) {
        CatchupResolver.isCatchupAvailable(context, channel)
    }

    val now = remember { System.currentTimeMillis() }

    val pastPrograms = remember(programs) {
        programs.filter { it.hasEnded(now) }.sortedByDescending { it.startTimeMillis }
    }

    val currentProgram = remember(programs) {
        programs.firstOrNull { it.isCurrentlyAiring(now) }
    }

    val upcomingPrograms = remember(programs) {
        programs.filter { it.startTimeMillis > now }.sortedBy { it.startTimeMillis }
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
                    logoUrl = channel.logoUrl,
                    channelName = channel.name,
                    size = 54.dp
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "EPG Timeline",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (catchupAvailable) {
                        Text(
                            text = "Catch-up available • Tap a past program to play",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item {
            Button(onClick = onBack) {
                Text(text = "Back to Player")
            }
        }

        if (programs.isEmpty()) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No EPG data available",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = if (tvgId == null) {
                            "This channel has no tvg-id. EPG data cannot be matched."
                        } else {
                            "No programs found for tvg-id: $tvgId. Make sure EPG is loaded in Settings."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (currentProgram != null) {
            item {
                Text(
                    text = "Now Playing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                EpgProgramCard(
                    program = currentProgram,
                    isNow = true,
                    catchupAvailable = false,
                    onPlayCatchup = null
                )
            }
        }

        if (upcomingPrograms.isNotEmpty()) {
            item {
                Text(
                    text = "Upcoming (${upcomingPrograms.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(
                items = upcomingPrograms.take(10),
                key = { program -> "upcoming_${program.startTimeMillis}_${program.title}" }
            ) { program ->
                EpgProgramCard(
                    program = program,
                    isNow = false,
                    catchupAvailable = false,
                    onPlayCatchup = null
                )
            }

            if (upcomingPrograms.size > 10) {
                item {
                    Text(
                        text = "+ ${upcomingPrograms.size - 10} more upcoming",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (pastPrograms.isNotEmpty()) {
            item {
                Text(
                    text = "Past Programs (${pastPrograms.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(
                items = pastPrograms.take(20),
                key = { program -> "past_${program.startTimeMillis}_${program.title}" }
            ) { program ->
                EpgProgramCard(
                    program = program,
                    isNow = false,
                    catchupAvailable = catchupAvailable,
                    onPlayCatchup = {
                        val catchupInfo = CatchupResolver.resolve(context, channel, program)
                        if (catchupInfo != null) {
                            onPlayCatchup(catchupInfo)
                        }
                    }
                )
            }

            if (pastPrograms.size > 20) {
                item {
                    Text(
                        text = "+ ${pastPrograms.size - 20} more past programs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EpgProgramCard(
    program: EpgProgram,
    isNow: Boolean,
    catchupAvailable: Boolean,
    onPlayCatchup: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isNow) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = BorderStroke(
            width = if (isNow) 2.dp else 1.dp,
            color = if (isNow) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = program.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = program.formattedTimeRange(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (program.description.isNotBlank()) {
                Text(
                    text = program.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }

            if (program.category.isNotBlank()) {
                Text(
                    text = "Category: ${program.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (catchupAvailable && onPlayCatchup != null) {
                TextButton(
                    onClick = onPlayCatchup
                ) {
                    Text(text = "▶ Play Catch-up")
                }
            }
        }
    }
}