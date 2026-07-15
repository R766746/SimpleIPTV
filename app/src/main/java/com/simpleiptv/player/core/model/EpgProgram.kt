package com.simpleiptv.player.core.model

data class EpgProgram(
    val channelId: String,
    val title: String,
    val description: String = "",
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val category: String = ""
) {
    fun isCurrentlyAiring(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return nowMillis in startTimeMillis until endTimeMillis
    }

    fun hasEnded(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return nowMillis >= endTimeMillis
    }

    fun formattedTimeRange(): String {
        val startFormatted = formatTime(startTimeMillis)
        val endFormatted = formatTime(endTimeMillis)
        return "$startFormatted - $endFormatted"
    }

    private fun formatTime(millis: Long): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = millis
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)
        return "%02d:%02d".format(hour, minute)
    }
}