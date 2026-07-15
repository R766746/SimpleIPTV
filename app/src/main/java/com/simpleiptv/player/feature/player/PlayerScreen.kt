package com.simpleiptv.player.feature.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import com.simpleiptv.player.core.util.TvDeviceDetector
import com.simpleiptv.player.ui.components.ChannelLogo
import com.simpleiptv.player.ui.components.PlaceholderScreen

import android.view.KeyEvent as AndroidKeyEvent

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    onOpenEpgTimeline: () -> Unit = {}
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

    val isTv = remember(context) { TvDeviceDetector.isAndroidTv(context) }

    var isFullscreen by remember(isTv) {
        mutableStateOf(isTv)
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

    ApplyFullscreenMode(enabled = isFullscreen)

    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    val playerResult = remember(context) {
        runCatching { ExoPlayer.Builder(context).build() }
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
            playbackState = PlaybackUiState.Error(message = "Stream URL is empty.")
            return
        }

        runCatching {
            playbackState = PlaybackUiState.Buffering
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.setMediaItem(MediaItem.Builder().setUri(streamUrl).build())
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }.onFailure { throwable ->
            playbackState = PlaybackUiState.Error(
                message = throwable.message ?: throwable::class.java.simpleName
            )
        }
    }

    fun switchToChannel(nextChannel: Channel?) {
        nextChannel?.let { currentChannel = it }
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
                playbackState = PlaybackUiState.Error(message = error.message ?: "Playback failed.")
            }
        }

        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            runCatching { exoPlayer.stop(); exoPlayer.release() }
        }
    }

    DisposableEffect(Unit) {
        val previousKeepScreenOn = view.keepScreenOn
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = previousKeepScreenOn }
    }

    LaunchedEffect(channel.streamUrl) { prepareAndPlay(channel) }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // Hardware key handling (TV remote + D-pad)
    val handleKeyEvent: (androidx.compose.ui.input.key.KeyEvent) -> Boolean = remember {
        { keyEvent ->
            if (keyEvent.nativeKeyEvent.action == AndroidKeyEvent.ACTION_DOWN) {
                when (keyEvent.nativeKeyEvent.keyCode) {
                    AndroidKeyEvent.KEYCODE_CHANNEL_UP,
                    AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                        switchToChannel(ChannelSessionStore.selectNextChannel())
                        true
                    }
                    AndroidKeyEvent.KEYCODE_CHANNEL_DOWN,
                    AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                        switchToChannel(ChannelSessionStore.selectPreviousChannel())
                        true
                    }
                    AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                    AndroidKeyEvent.KEYCODE_SPACE -> {
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        true
                    }
                    AndroidKeyEvent.KEYCODE_MEDIA_PLAY -> {
                        exoPlayer.play()
                        true
                    }
                    AndroidKeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        exoPlayer.pause()
                        true
                    }
                    else -> false
                }
            } else false
        }
    }

    if (isFullscreen) {
        FullscreenPlayerView(
            exoPlayer = exoPlayer,
            playbackState = playbackState,
            channelName = channel.name,
            onBack = onBack,
            onExitFullscreen = { isFullscreen = false },
            handleKeyEvent = handleKeyEvent,
            focusRequester = focusRequester
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusable()
            .focusRequester(focusRequester)
            .onKeyEvent(handleKeyEvent)
    ) {
        PlayerVideoSurface(
            exoPlayer = exoPlayer,
            playbackState = playbackState,
            modifier = Modifier.fillMaxWidth().height(340.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (isTv) {
                AssistChip(
                    onClick = {},
                    interactionSource = remember { MutableInteractionSource() },
                    label = { Text(text = "📺 TV Mode Active • Remote keys enabled") }
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = onBack) { Text(text = "Back") }
                TextButton(onClick = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() }) {
                    Text(text = if (isPlaying) "Pause" else "Play")
                }
                TextButton(onClick = { prepareAndPlay(channel) }) { Text(text = "Retry") }
                OutlinedButton(onClick = { isFullscreen = true }) { Text(text = "Full Screen") }
                OutlinedButton(enabled = ChannelSessionStore.hasMultipleChannels(), onClick = { switchToChannel(ChannelSessionStore.selectPreviousChannel()) }) { Text(text = "Previous") }
                OutlinedButton(enabled = ChannelSessionStore.hasMultipleChannels(), onClick = { switchToChannel(ChannelSessionStore.selectNextChannel()) }) { Text(text = "Next") }
                OutlinedButton(onClick = { isFavorite = favoriteStore.toggleFavorite(channel) })
                {
                    Text(text = if (isFavorite) "★ Remove Favorite" else "☆ Add Favorite")
                }
                OutlinedButton(
                    onClick = onOpenEpgTimeline
                ) {
                    Text(text = "📺 EPG / Catch-up")
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    interactionSource = remember { MutableInteractionSource() },
                    label = {
                        val stateText = when (playbackState) {
                            PlaybackUiState.Idle -> "Idle"
                            PlaybackUiState.Buffering -> "Buffering"
                            PlaybackUiState.Ready -> if (isPlaying) "Playing" else "Ready"
                            is PlaybackUiState.Error -> "Error"
                        }
                        Text(text = stateText)
                    }
                )
                ChannelSessionStore.selectedChannelPositionText()?.let { position ->
                    AssistChip(
                        onClick = {},
                        interactionSource = remember { MutableInteractionSource() },
                        label = { Text(text = "Channel $position") }
                    )
                }
                if (isFavorite) {
                    AssistChip(
                        onClick = {},
                        interactionSource = remember { MutableInteractionSource() },
                        label = { Text(text = "Favorite") }
                    )
                }
            }

            if (playbackState is PlaybackUiState.Error) {
                val error = playbackState as PlaybackUiState.Error
                Text(text = "Playback error: ${error.message}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                ChannelLogo(logoUrl = channel.logoUrl, channelName = channel.name, size = 64.dp)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    Text(text = channel.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(text = channel.groupTitle?.takeIf { it.isNotBlank() } ?: "No group", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    channel.playlistName?.let { Text(text = "Source: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    channel.tvgId?.let { Text(text = "TVG ID: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Stream URL", style = MaterialTheme.typography.titleSmall)
                Text(text = channel.streamUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FullscreenPlayerView(
    exoPlayer: ExoPlayer,
    playbackState: PlaybackUiState,
    channelName: String,
    onBack: () -> Unit,
    onExitFullscreen: () -> Unit,
    handleKeyEvent: (androidx.compose.ui.input.key.KeyEvent) -> Boolean,
    focusRequester: FocusRequester
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusable()
            .focusRequester(focusRequester)
            .onKeyEvent(handleKeyEvent)
    ) {
        PlayerVideoSurface(exoPlayer = exoPlayer, playbackState = playbackState, modifier = Modifier.fillMaxSize())

        FlowRow(
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(onClick = onBack) { Text(text = "Back") }
            OutlinedButton(onClick = onExitFullscreen) { Text(text = "Exit Full Screen") }
            AssistChip(
                onClick = {},
                interactionSource = remember { MutableInteractionSource() },
                label = { Text(text = channelName) }
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PlayerVideoSurface(
    exoPlayer: ExoPlayer,
    playbackState: PlaybackUiState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { androidContext ->
                PlayerView(androidContext).apply {
                    player = exoPlayer
                    useController = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                }
            },
            update = { playerView -> playerView.player = exoPlayer }
        )
        if (playbackState is PlaybackUiState.Buffering) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun ApplyFullscreenMode(enabled: Boolean) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    DisposableEffect(enabled, activity) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            if (enabled) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
                WindowCompat.setDecorFitsSystemWindows(window, true)
            }
        }
        onDispose {
            activity?.window?.let { w ->
                WindowInsetsControllerCompat(w, w.decorView).show(WindowInsetsCompat.Type.systemBars())
                WindowCompat.setDecorFitsSystemWindows(w, true)
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}