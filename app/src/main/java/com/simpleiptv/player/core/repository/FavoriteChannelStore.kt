package com.simpleiptv.player.core.repository

import android.content.Context
import com.simpleiptv.player.core.model.Channel
import org.json.JSONArray
import org.json.JSONObject

class FavoriteChannelStore(
    context: Context
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun loadFavorites(): List<Channel> {
        val rawJson = preferences.getString(KEY_FAVORITES, null) ?: return emptyList()

        return try {
            val array = JSONArray(rawJson)
            val favorites = mutableListOf<Channel>()

            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue

                val id = item.optString(FIELD_ID)
                val name = item.optString(FIELD_NAME)
                val streamUrl = item.optString(FIELD_STREAM_URL)

                if (id.isBlank() || name.isBlank() || streamUrl.isBlank()) {
                    continue
                }

                favorites.add(
                    Channel(
                        id = id,
                        name = name,
                        streamUrl = streamUrl,
                        tvgId = item.optString(FIELD_TVG_ID).blankToNull(),
                        tvgName = item.optString(FIELD_TVG_NAME).blankToNull(),
                        logoUrl = item.optString(FIELD_LOGO_URL).blankToNull(),
                        groupTitle = item.optString(FIELD_GROUP_TITLE).blankToNull(),
                        playlistSourceId = item.optString(FIELD_PLAYLIST_SOURCE_ID).blankToNull(),
                        playlistName = item.optString(FIELD_PLAYLIST_NAME).blankToNull()
                    )
                )
            }

            favorites
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun isFavorite(channelId: String): Boolean {
        return loadFavorites().any { channel ->
            channel.id == channelId
        }
    }

    fun addFavorite(channel: Channel) {
        val current = loadFavorites()
            .filterNot { favorite ->
                favorite.id == channel.id
            }

        saveFavorites(current + channel)
    }

    fun removeFavorite(channelId: String) {
        val updated = loadFavorites()
            .filterNot { favorite ->
                favorite.id == channelId
            }

        saveFavorites(updated)
    }

    fun toggleFavorite(channel: Channel): Boolean {
        val currentlyFavorite = isFavorite(channel.id)

        return if (currentlyFavorite) {
            removeFavorite(channel.id)
            false
        } else {
            addFavorite(channel)
            true
        }
    }

    fun clearFavorites() {
        preferences
            .edit()
            .remove(KEY_FAVORITES)
            .apply()
    }

    private fun saveFavorites(favorites: List<Channel>) {
        val array = JSONArray()

        favorites.forEach { channel ->
            val item = JSONObject()
                .put(FIELD_ID, channel.id)
                .put(FIELD_NAME, channel.name)
                .put(FIELD_STREAM_URL, channel.streamUrl)
                .put(FIELD_TVG_ID, channel.tvgId.orEmpty())
                .put(FIELD_TVG_NAME, channel.tvgName.orEmpty())
                .put(FIELD_LOGO_URL, channel.logoUrl.orEmpty())
                .put(FIELD_GROUP_TITLE, channel.groupTitle.orEmpty())
                .put(FIELD_PLAYLIST_SOURCE_ID, channel.playlistSourceId.orEmpty())
                .put(FIELD_PLAYLIST_NAME, channel.playlistName.orEmpty())

            array.put(item)
        }

        preferences
            .edit()
            .putString(KEY_FAVORITES, array.toString())
            .apply()
    }

    private fun String.blankToNull(): String? {
        return takeIf { it.isNotBlank() }
    }

    private companion object {
        const val PREFS_NAME = "simple_iptv_favorites"
        const val KEY_FAVORITES = "favorite_channels"

        const val FIELD_ID = "id"
        const val FIELD_NAME = "name"
        const val FIELD_STREAM_URL = "stream_url"
        const val FIELD_TVG_ID = "tvg_id"
        const val FIELD_TVG_NAME = "tvg_name"
        const val FIELD_LOGO_URL = "logo_url"
        const val FIELD_GROUP_TITLE = "group_title"
        const val FIELD_PLAYLIST_SOURCE_ID = "playlist_source_id"
        const val FIELD_PLAYLIST_NAME = "playlist_name"
    }
}