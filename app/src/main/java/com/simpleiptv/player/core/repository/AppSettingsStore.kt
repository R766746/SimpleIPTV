package com.simpleiptv.player.core.repository

import android.content.Context

class AppSettingsStore(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun getThemeMode(): ThemeMode {
        val value = preferences.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(value ?: ThemeMode.SYSTEM.name)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        preferences
            .edit()
            .putString(KEY_THEME_MODE, mode.name)
            .apply()
    }

    fun getBufferDurationMs(): Long {
        return preferences.getLong(KEY_BUFFER_DURATION_MS, DEFAULT_BUFFER_MS)
    }

    fun setBufferDurationMs(durationMs: Long) {
        preferences
            .edit()
            .putLong(KEY_BUFFER_DURATION_MS, durationMs)
            .apply()
    }

    fun getBufferLabel(): String {
        val ms = getBufferDurationMs()
        return "${ms / 1000}s"
    }

    private companion object {
        const val PREFS_NAME = "simple_iptv_app_settings"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_BUFFER_DURATION_MS = "buffer_duration_ms"
        const val DEFAULT_BUFFER_MS = 10_000L
    }
}

enum class ThemeMode(
    val label: String
) {
    SYSTEM("System Default"),
    DARK("Dark"),
    LIGHT("Light")
}

val BUFFER_OPTIONS = listOf(
    5_000L to "5s (Low latency)",
    10_000L to "10s (Default)",
    20_000L to "20s (Stable)",
    30_000L to "30s (High buffer)",
    60_000L to "60s (Maximum)"
)