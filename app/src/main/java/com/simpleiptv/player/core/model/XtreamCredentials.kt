package com.simpleiptv.player.core.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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

    fun vodStreamUrl(streamId: String, extension: String = "mp4"): String {
        val normalized = serverUrl.trimEnd('/')
        return "$normalized/movie/$username/$password/$streamId.$extension"
    }
    fun catchupUrl(streamId: String, startTimeMillis: Long, durationSeconds: Long): String {
        val normalized = serverUrl.trimEnd('/')
        val startFormatted = formatCatchupTime(startTimeMillis)
        return "$normalized/streaming/timeshift.php?username=$username&password=$password&stream=$streamId&start=$startFormatted&duration=$durationSeconds"
    }

    fun catchupUrlAlt(streamId: String, startTimeMillis: Long, endTimeMillis: Long): String {
        val normalized = serverUrl.trimEnd('/')
        val startFormatted = formatCatchupTime(startTimeMillis)
        val endFormatted = formatCatchupTime(endTimeMillis)
        return "$normalized/timeshift/$username/$password/$streamId/$startFormatted/$endFormatted.ts"
    }

    private fun formatCatchupTime(millis: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd:HH-mm", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date(millis))
    }
}