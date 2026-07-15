package com.simpleiptv.player.core.repository

import android.content.Context
import com.simpleiptv.player.core.model.XtreamCredentials
import org.json.JSONArray
import org.json.JSONObject

class XtreamCredentialsStore(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun loadAll(): List<XtreamCredentialEntry> {
        val rawJson = preferences.getString(KEY_CREDENTIALS, null) ?: return emptyList()

        return try {
            val array = JSONArray(rawJson)
            val result = mutableListOf<XtreamCredentialEntry>()

            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue

                val sourceId = item.optString(FIELD_SOURCE_ID).trim()
                val serverUrl = item.optString(FIELD_SERVER_URL).trim()
                val username = item.optString(FIELD_USERNAME).trim()
                val password = item.optString(FIELD_PASSWORD).trim()

                if (sourceId.isBlank() || serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
                    continue
                }

                result.add(
                    XtreamCredentialEntry(
                        sourceId = sourceId,
                        credentials = XtreamCredentials(
                            serverUrl = serverUrl,
                            username = username,
                            password = password
                        )
                    )
                )
            }

            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(sourceId: String, credentials: XtreamCredentials) {
        val current = loadAll()
            .filterNot { it.sourceId == sourceId }

        val updated = current + XtreamCredentialEntry(
            sourceId = sourceId,
            credentials = credentials
        )

        saveAll(updated)
    }

    fun getBySourceId(sourceId: String): XtreamCredentials? {
        return loadAll()
            .firstOrNull { it.sourceId == sourceId }
            ?.credentials
    }

    fun remove(sourceId: String) {
        val updated = loadAll().filterNot { it.sourceId == sourceId }
        saveAll(updated)
    }

    private fun saveAll(entries: List<XtreamCredentialEntry>) {
        val array = JSONArray()

        entries.forEach { entry ->
            val item = JSONObject()
                .put(FIELD_SOURCE_ID, entry.sourceId)
                .put(FIELD_SERVER_URL, entry.credentials.serverUrl)
                .put(FIELD_USERNAME, entry.credentials.username)
                .put(FIELD_PASSWORD, entry.credentials.password)

            array.put(item)
        }

        preferences
            .edit()
            .putString(KEY_CREDENTIALS, array.toString())
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "simple_iptv_xtream_creds"
        const val KEY_CREDENTIALS = "xtream_credentials"

        const val FIELD_SOURCE_ID = "source_id"
        const val FIELD_SERVER_URL = "server_url"
        const val FIELD_USERNAME = "username"
        const val FIELD_PASSWORD = "password"
    }
}

data class XtreamCredentialEntry(
    val sourceId: String,
    val credentials: XtreamCredentials
)