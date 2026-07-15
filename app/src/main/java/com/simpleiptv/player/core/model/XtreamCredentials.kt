package com.simpleiptv.player.core.model

data class XtreamCredentials(
    val serverUrl: String,
    val username: String,
    val password: String
) {
    fun baseApiUrl(): String {
        val normalized = serverUrl.trimEnd('/')
        return "$normalized/player_api.php"
    }

    fun liveStreamUrl(streamId: String, extension: String = "m3u8"): String {
        val normalized = serverUrl.trimEnd('/')
        return "$normalized/live/$username/$password/$streamId.$extension"
    }
}