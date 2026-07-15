package com.simpleiptv.player.core.network

import com.simpleiptv.player.core.model.XtreamCategory
import com.simpleiptv.player.core.model.XtreamCredentials
import com.simpleiptv.player.core.model.XtreamLiveStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

object XtreamApiClient {

    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 30_000

    suspend fun authenticate(credentials: XtreamCredentials): Result<JSONObject> {
        return fetchJson(
            url = "${credentials.baseApiUrl()}?username=${credentials.username}&password=${credentials.password}"
        )
    }

    suspend fun getLiveCategories(credentials: XtreamCredentials): Result<List<XtreamCategory>> {
        val url = "${credentials.baseApiUrl()}?username=${credentials.username}&password=${credentials.password}&action=get_live_categories"

        return fetchJsonArray(url).map { array ->
            val categories = mutableListOf<XtreamCategory>()

            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue

                val id = item.optString("category_id", "").trim()
                val name = item.optString("category_name", "").trim()

                if (id.isNotBlank() && name.isNotBlank()) {
                    categories.add(
                        XtreamCategory(
                            id = id,
                            name = name
                        )
                    )
                }
            }

            categories
        }
    }

    suspend fun getLiveStreams(credentials: XtreamCredentials): Result<List<XtreamLiveStream>> {
        val url = "${credentials.baseApiUrl()}?username=${credentials.username}&password=${credentials.password}&action=get_live_streams"

        return fetchJsonArray(url).map { array ->
            val streams = mutableListOf<XtreamLiveStream>()

            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue

                val streamId = item.optString("stream_id", "").trim()
                val name = item.optString("name", "").trim()

                if (streamId.isBlank() || name.isBlank()) {
                    continue
                }

                streams.add(
                    XtreamLiveStream(
                        streamId = streamId,
                        name = name,
                        categoryId = item.optString("category_id", "").blankToNull(),
                        epgChannelId = item.optString("epg_channel_id", "").blankToNull(),
                        streamIcon = item.optString("stream_icon", "").blankToNull(),
                        containerExtension = item.optString("container_extension", "ts").blankToNull()
                    )
                )
            }

            streams
        }
    }

    private suspend fun fetchJson(url: String): Result<JSONObject> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val rawResponse = httpGet(url)
                JSONObject(rawResponse)
            }
        }
    }

    private suspend fun fetchJsonArray(url: String): Result<JSONArray> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val rawResponse = httpGet(url)
                JSONArray(rawResponse)
            }
        }
    }

    private fun httpGet(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true

            setRequestProperty("User-Agent", "SimpleIPTV/1.0 Android")
            setRequestProperty("Accept", "application/json, */*")
        }

        try {
            val responseCode = connection.responseCode

            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream
                    ?.bufferedReader()
                    ?.use { it.readText().take(300) }
                    .orEmpty()

                throw IllegalStateException(
                    buildString {
                        append("Server returned HTTP ")
                        append(responseCode)
                        if (errorBody.isNotBlank()) {
                            append(": ")
                            append(errorBody)
                        }
                    }
                )
            }

            return connection.inputStream.use { inputStream ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                val output = ByteArrayOutputStream()

                while (true) {
                    val read = inputStream.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                }

                String(output.toByteArray(), Charsets.UTF_8)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun String.blankToNull(): String? {
        return takeIf { it.isNotBlank() }
    }
}