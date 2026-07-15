package com.simpleiptv.player.core.model

data class XtreamEpisode(
    val id: String,
    val episodeNum: Int,
    val title: String,
    val containerExtension: String,
    val seasonNumber: Int,
    val info: XtreamEpisodeInfo? = null
)

data class XtreamEpisodeInfo(
    val plot: String?,
    val duration: String?,
    val rating: String?,
    val coverBig: String?
)

data class XtreamSeason(
    val seasonNumber: Int,
    val episodes: List<XtreamEpisode>
)