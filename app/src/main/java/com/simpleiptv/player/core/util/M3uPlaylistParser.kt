package com.simpleiptv.player.core.util

import com.simpleiptv.player.core.model.Channel
import java.security.MessageDigest
import java.util.Locale

data class M3uParseResult(
    val channels: List<Channel>,
    val warnings: List<String>
) {
    val channelCount: Int
        get() = channels.size
}

object M3uPlaylistParser {

    private val AttributeRegex = Regex("""([\w-]+)\s*=\s*(?:"([^"]*)"|'([^']*)')""")

    fun parse(rawContent: String): M3uParseResult {
        val warnings = mutableListOf<String>()
        val channels = mutableListOf<Channel>()

        val lines = rawContent
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        if (lines.isEmpty()) {
            return M3uParseResult(
                channels = emptyList(),
                warnings = listOf("Playlist is empty.")
            )
        }

        if (!lines.first().startsWith("#EXTM3U", ignoreCase = true)) {
            warnings.add("Playlist does not start with #EXTM3U. Parsing will continue anyway.")
        }

        var pendingExtInf: ExtInfData? = null

        lines.forEachIndexed { index, line ->
            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    pendingExtInf = parseExtInf(line)
                }

                line.startsWith("#") -> {
                    // Ignore other metadata/comments for now.
                }

                else -> {
                    val streamUrl = line

                    val extInf = pendingExtInf
                    if (extInf == null) {
                        warnings.add("Line ${index + 1}: stream URL without #EXTINF was skipped.")
                        return@forEachIndexed
                    }

                    pendingExtInf = null

                    if (!looksLikeStreamUrl(streamUrl)) {
                        warnings.add("Line ${index + 1}: stream URL looks unusual: $streamUrl")
                    }

                    val channelName = extInf.name
                        .ifBlank { extInf.tvgName.orEmpty() }
                        .ifBlank { "Unnamed Channel" }

                    val channel = Channel(
                        id = generateStableId(
                            name = channelName,
                            streamUrl = streamUrl,
                            tvgId = extInf.tvgId
                        ),
                        name = channelName,
                        streamUrl = streamUrl,
                        tvgId = extInf.tvgId,
                        tvgName = extInf.tvgName,
                        logoUrl = extInf.logoUrl,
                        groupTitle = extInf.groupTitle
                    )

                    channels.add(channel)
                }
            }
        }

        if (pendingExtInf != null) {
            warnings.add("Playlist ended with #EXTINF but no stream URL after it.")
        }

        return M3uParseResult(
            channels = channels,
            warnings = warnings
        )
    }

    private fun parseExtInf(line: String): ExtInfData {
        val payload = line.substringAfter(":", missingDelimiterValue = "")
        val separatorIndex = findUnquotedComma(payload)

        val metadataPart = if (separatorIndex >= 0) {
            payload.substring(0, separatorIndex)
        } else {
            payload
        }

        val namePart = if (separatorIndex >= 0) {
            payload.substring(separatorIndex + 1).trim()
        } else {
            ""
        }

        val attributes = parseAttributes(metadataPart)

        return ExtInfData(
            name = namePart,
            tvgId = attributes["tvg-id"],
            tvgName = attributes["tvg-name"],
            logoUrl = attributes["tvg-logo"]
                ?: attributes["logo"]
                ?: attributes["tvg-logo-url"],
            groupTitle = attributes["group-title"]
        )
    }

    private fun parseAttributes(metadata: String): Map<String, String> {
        val result = mutableMapOf<String, String>()

        AttributeRegex.findAll(metadata).forEach { match ->
            val key = match.groupValues[1].lowercase(Locale.US)
            val doubleQuotedValue = match.groupValues.getOrNull(2).orEmpty()
            val singleQuotedValue = match.groupValues.getOrNull(3).orEmpty()

            val value = doubleQuotedValue.ifBlank {
                singleQuotedValue
            }.trim()

            if (key.isNotBlank() && value.isNotBlank()) {
                result[key] = value
            }
        }

        return result
    }

    private fun findUnquotedComma(value: String): Int {
        var inDoubleQuote = false
        var inSingleQuote = false

        value.forEachIndexed { index, char ->
            when (char) {
                '"' -> {
                    if (!inSingleQuote) {
                        inDoubleQuote = !inDoubleQuote
                    }
                }

                '\'' -> {
                    if (!inDoubleQuote) {
                        inSingleQuote = !inSingleQuote
                    }
                }

                ',' -> {
                    if (!inDoubleQuote && !inSingleQuote) {
                        return index
                    }
                }
            }
        }

        return -1
    }

    private fun looksLikeStreamUrl(url: String): Boolean {
        val normalized = url.lowercase(Locale.US)

        return normalized.startsWith("http://") ||
                normalized.startsWith("https://") ||
                normalized.startsWith("rtmp://") ||
                normalized.startsWith("rtsp://") ||
                normalized.startsWith("udp://") ||
                normalized.startsWith("rtp://")
    }

    private fun generateStableId(
        name: String,
        streamUrl: String,
        tvgId: String?
    ): String {
        val input = "${tvgId.orEmpty()}|$name|$streamUrl"
        val digest = MessageDigest
            .getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))

        return digest
            .take(16)
            .joinToString(separator = "") { byte ->
                "%02x".format(byte.toInt() and 0xff)
            }
    }

    private data class ExtInfData(
        val name: String,
        val tvgId: String?,
        val tvgName: String?,
        val logoUrl: String?,
        val groupTitle: String?
    )
}