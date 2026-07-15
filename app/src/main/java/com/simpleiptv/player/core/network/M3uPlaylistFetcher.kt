package com.simpleiptv.player.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object M3uPlaylistFetcher {

    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 30_000
    private const val MAX_PLAYLIST_BYTES = 50 * 1024 * 1024 // 50 MB

    suspend fun fetch(url: String): Result<String> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val normalizedUrl = url.trim()

                require(
                    normalizedUrl.startsWith("http://", ignoreCase = true) ||
                            normalizedUrl.startsWith("https://", ignoreCase = true)
                ) {
                    "Playlist URL must start with http:// or https://"
                }

                val connection = (URL(normalizedUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    instanceFollowRedirects = true

                    setRequestProperty(
                        "User-Agent",
                        "SimpleIPTV/1.0 Android"
                    )

                    setRequestProperty(
                        "Accept",
                        "application/x-mpegURL, application/vnd.apple.mpegurl, text/plain, */*"
                    )
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

                    connection.inputStream.use { inputStream ->
                        readLimitedText(inputStream)
                    }
                } finally {
                    connection.disconnect()
                }
            }
        }
    }

    private fun readLimitedText(inputStream: InputStream): String {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        val output = ByteArrayOutputStream()

        var totalBytes = 0

        while (true) {
            val read = inputStream.read(buffer)

            if (read == -1) {
                break
            }

            totalBytes += read

            if (totalBytes > MAX_PLAYLIST_BYTES) {
                throw IllegalStateException(
                    "Playlist is too large. Maximum supported preview size is 50 MB."
                )
            }

            output.write(buffer, 0, read)
        }

        return String(output.toByteArray(), Charsets.UTF_8)
    }
}