package com.simpleiptv.player.core.repository

import android.content.Context

class EpgSettingsStore(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun getEpgUrl(): String {
        return preferences.getString(KEY_EPG_URL, "").orEmpty()
    }

    fun setEpgUrl(url: String) {
        preferences
            .edit()
            .putString(KEY_EPG_URL, url.trim())
            .apply()
    }

    fun clearEpgUrl() {
        preferences
            .edit()
            .remove(KEY_EPG_URL)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "simple_iptv_epg_settings"
        const val KEY_EPG_URL = "epg_url"
    }
}