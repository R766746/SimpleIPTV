package com.simpleiptv.player.core.model

data class XtreamVodStream(
    val streamId: String,
    val name: String,
    val categoryId: String?,
    val streamIcon: String?,
    val containerExtension: String?,
    val rating: String?
)