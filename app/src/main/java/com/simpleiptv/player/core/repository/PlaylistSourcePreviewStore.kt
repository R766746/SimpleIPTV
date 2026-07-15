package com.simpleiptv.player.core.repository

import android.content.Context
import com.simpleiptv.player.core.model.PlaylistSourcePreview
import com.simpleiptv.player.core.model.PlaylistSourceType
import org.json.JSONArray
import org.json.JSONObject

class PlaylistSourcePreviewStore(
    context: Context
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun load(): List<PlaylistSourcePreview> {
        val rawJson = preferences.getString(KEY_PLAYLISTS, null) ?: return emptyList()

        return try {
            val array = JSONArray(rawJson)
            val result = mutableListOf<PlaylistSourcePreview>()

            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue

                val id = item.optString(FIELD_ID)
                val name = item.optString(FIELD_NAME)
                val typeRaw = item.optString(FIELD_TYPE)
                val description = item.optString(FIELD_DESCRIPTION)
                val isEnabled = item.optBoolean(FIELD_ENABLED, true)

                if (id.isBlank() || name.isBlank() || typeRaw.isBlank()) {
                    continue
                }

                val type = try {
                    PlaylistSourceType.valueOf(typeRaw)
                } catch (_: IllegalArgumentException) {
                    continue
                }

                result.add(
                    PlaylistSourcePreview(
                        id = id,
                        name = name,
                        type = type,
                        description = description,
                        isEnabled = isEnabled
                    )
                )
            }

            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(playlists: List<PlaylistSourcePreview>) {
        val array = JSONArray()

        playlists.forEach { playlist ->
            val item = JSONObject()
                .put(FIELD_ID, playlist.id)
                .put(FIELD_NAME, playlist.name)
                .put(FIELD_TYPE, playlist.type.name)
                .put(FIELD_DESCRIPTION, playlist.description)
                .put(FIELD_ENABLED, playlist.isEnabled)

            array.put(item)
        }

        preferences
            .edit()
            .putString(KEY_PLAYLISTS, array.toString())
            .apply()
    }

    fun clear() {
        preferences
            .edit()
            .remove(KEY_PLAYLISTS)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "simple_iptv_playlist_sources"
        const val KEY_PLAYLISTS = "playlist_sources"

        const val FIELD_ID = "id"
        const val FIELD_NAME = "name"
        const val FIELD_TYPE = "type"
        const val FIELD_DESCRIPTION = "description"
        const val FIELD_ENABLED = "enabled"
    }
}