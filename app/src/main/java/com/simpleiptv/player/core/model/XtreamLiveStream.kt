package com.simpleiptv.player.core.model

data class XtreamLiveStream(
    val streamId: String,
    val name: String,
    val categoryId: String?,
    val epgChannelId: String?,
    val streamIcon: String?,
    val containerExtension: String?
)