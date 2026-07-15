package com.simpleiptv.player.feature.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.simpleiptv.player.core.player.PlaybackUiState
import com.simpleiptv.player.core.repository.ChannelSessionStore
import com.simpleiptv.player.ui.components.PlaceholderScreen

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit
) {
    val channel = ChannelSessionStore.selectedChannel

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

    var playbackState by remember {
        mutableStateOf<PlaybackUiState>(PlaybackUiState.Idle)
    }

    var isPlaying by remember {
        mutableStateOf(false)
    }

    val exoPlayer = remember(channel.streamUrl) {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                val mediaItem = MediaItem.fromUri(channel.streamUrl)

                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            }
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
            exoPlayer.release()
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
        playbackState = PlaybackUiState.Buffering
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onBack
                ) {
                    Text(text = "Back")
                }

                TextButton(
                    onClick = {
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                        } else {
                            exoPlayer.play()
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
            }

            PlaybackStatusChip(
                playbackState = playbackState,
                isPlaying = isPlaying
            )

            if (playbackState is PlaybackUiState.Error) {
                val error = playbackState as PlaybackUiState.Error

                Text(
                    text = "Playback error: ${error.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )

                Button(
                    onClick = {
                        playbackState = PlaybackUiState.Buffering
                        exoPlayer.stop()
                        exoPlayer.clearMediaItems()
                        exoPlayer.setMediaItem(MediaItem.fromUri(channel.streamUrl))
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = true
                    }
                ) {
                    Text(text = "Retry")
                }
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