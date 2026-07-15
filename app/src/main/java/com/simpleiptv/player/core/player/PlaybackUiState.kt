package com.simpleiptv.player.core.player

sealed interface PlaybackUiState {
    data object Idle : PlaybackUiState
    data object Buffering : PlaybackUiState
    data object Ready : PlaybackUiState

    data class Error(
        val message: String
    ) : PlaybackUiState
}