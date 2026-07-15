package com.simpleiptv.player.feature.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.simpleiptv.player.core.model.Channel
import com.simpleiptv.player.core.player.PlaybackUiState
import com.simpleiptv.player.core.repository.ChannelSessionStore
import com.simpleiptv.player.core.repository.FavoriteChannelStore
import com.simpleiptv.player.ui.components.PlaceholderScreen

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit
) {
    var currentChannel by remember {
        mutableStateOf(ChannelSessionStore.selectedChannel)
    }

    val channel = currentChannel

    if (channel == null) {
        PlaceholderScreen(
            icon = "▶",
            title = "No Channel Selected",
            description = "Go back to Live TV and choose a channel.",
            primaryActionText = "Back",
            onPrimaryAction = onBack
        )
        return
    }

    val context = LocalContext.current
    val view = LocalView.current

    val favoriteStore = remember(context) {
        FavoriteChannelStore(context)
    }

    var isFavorite by remember(channel.id) {
        mutableStateOf(favoriteStore.isFavorite(channel.id))
    }

    var playbackState by remember {
        mutableStateOf<PlaybackUiState>(PlaybackUiState.Idle)
    }

    var isPlaying by remember {
        mutableStateOf(false)
    }

    val playerResult = remember(context) {
        runCatching {
            ExoPlayer.Builder(context).build()
        }
    }

    val exoPlayer = playerResult.getOrNull()

    if (exoPlayer == null) {
        val errorMessage = playerResult.exceptionOrNull()?.message
            ?: "Unable to create video player."

        PlaceholderScreen(
            icon = "⚠",
            title = "Player Error",
            description = errorMessage,
            primaryActionText = "Back",
            onPrimaryAction = onBack
        )
        return
    }

    fun prepareAndPlay(targetChannel: Channel) {
        val streamUrl = targetChannel.streamUrl.trim()

        if (streamUrl.isBlank()) {
            playbackState = PlaybackUiState.Error(
                message = "Stream URL is empty."
            )
            return
        }

        runCatching {
            playbackState = PlaybackUiState.Buffering

            exoPlayer.stop()
            exoPlayer.clearMediaItems()

            val mediaItem = MediaItem.Builder()
                .setUri(streamUrl)
                .build()

            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }.onFailure { throwable ->
            playbackState = PlaybackUiState.Error(
                message = throwable.message ?: throwable::class.java.simpleName
            )
        }
    }

    fun switchToChannel(nextChannel: Channel?) {
        if (nextChannel == null) {
            return
        }

        currentChannel = nextChannel
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = when (state) {
                    Player.STATE_IDLE -> PlaybackUiState.Idle
                    Player.STATE_BUFFERING -> PlaybackUiState.Buffering
                    Player.STATE_READY -> PlaybackUiState.Ready
                    Player.STATE_ENDED -> PlaybackUiState.Idle
                    else -> PlaybackUiState.Idle
                }
            }

            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                isPlaying = isPlayingNow
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackState = PlaybackUiState.Error(
                    message = error.message ?: "Playback failed."
                )
            }
        }

        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)

            runCatching {
                exoPlayer.stop()
                exoPlayer.release()
            }
        }
    }

    DisposableEffect(Unit) {
        val previousKeepScreenOn = view.keepScreenOn
        view.keepScreenOn = true

        onDispose {
            view.keepScreenOn = previousKeepScreenOn
        }
    }

    LaunchedEffect(channel.streamUrl) {
        prepareAndPlay(channel)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { androidContext ->
                    PlayerView(androidContext).apply {
                        player = exoPlayer
                        useController = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)

                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { playerView ->
                    playerView.player = exoPlayer
                }
            )

            if (playbackState is PlaybackUiState.Buffering) {
                CircularProgressIndicator()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onBack
                ) {
                    Text(text = "Back")
                }

                TextButton(
                    onClick = {
                        runCatching {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                        }.onFailure { throwable ->
                            playbackState = PlaybackUiState.Error(
                                message = throwable.message ?: "Unable to control playback."
                            )
                        }
                    }
                ) {
                    Text(
                        text = if (isPlaying) {
                            "Pause"
                        } else {
                            "Play"
                        }
                    )
                }

                TextButton(
                    onClick = {
                        prepareAndPlay(channel)
                    }
                ) {
                    Text(text = "Retry")
                }

                OutlinedButton(
                    enabled = ChannelSessionStore.hasMultipleChannels(),
                    onClick = {
                        switchToChannel(ChannelSessionStore.selectPreviousChannel())
                    }
                ) {
                    Text(text = "Previous")
                }

                OutlinedButton(
                    enabled = ChannelSessionStore.hasMultipleChannels(),
                    onClick = {
                        switchToChannel(ChannelSessionStore.selectNextChannel())
                    }
                ) {
                    Text(text = "Next")
                }

                OutlinedButton(
                    onClick = {
                        isFavorite = favoriteStore.toggleFavorite(channel)
                    }
                ) {
                    Text(
                        text = if (isFavorite) {
                            "★ Remove Favorite"
                        } else {
                            "☆ Add Favorite"
                        }
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlaybackStatusChip(
                    playbackState = playbackState,
                    isPlaying = isPlaying
                )

                ChannelSessionStore.selectedChannelPositionText()?.let { position ->
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(text = "Channel $position")
                        }
                    )
                }

                if (isFavorite) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(text = "Favorite")
                        }
                    )
                }
            }

            if (playbackState is PlaybackUiState.Error) {
                val error = playbackState as PlaybackUiState.Error

                Text(
                    text = "Playback error: ${error.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = channel.groupTitle?.takeIf { it.isNotBlank() } ?: "No group",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                channel.playlistName?.let { playlistName ->
                    Text(
                        text = "Source: $playlistName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                channel.tvgId?.let { tvgId ->
                    Text(
                        text = "TVG ID: $tvgId",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Stream URL",
                    style = MaterialTheme.typography.titleSmall
                )

                Text(
                    text = channel.streamUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlaybackStatusChip(
    playbackState: PlaybackUiState,
    isPlaying: Boolean
) {
    val text = when (playbackState) {
        PlaybackUiState.Idle -> "Idle"
        PlaybackUiState.Buffering -> "Buffering"
        PlaybackUiState.Ready -> {
            if (isPlaying) {
                "Playing"
            } else {
                "Ready"
            }
        }

        is PlaybackUiState.Error -> "Error"
    }

    AssistChip(
        onClick = {},
        label = {
            Text(text = text)
        }
    )
}