package com.simpleiptv.player.core.model

data class CatchupInfo(
    val program: EpgProgram,
    val catchupUrl: String,
    val channelName: String
)