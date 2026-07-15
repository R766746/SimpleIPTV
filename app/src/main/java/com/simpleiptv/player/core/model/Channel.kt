package com.simpleiptv.player.core.model

data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val logoUrl: String? = null,
    val groupTitle: String? = null
)