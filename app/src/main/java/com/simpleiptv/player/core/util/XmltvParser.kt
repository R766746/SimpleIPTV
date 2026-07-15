package com.simpleiptv.player.core.util

import com.simpleiptv.player.core.model.EpgProgram
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class XmltvParseResult(
    val programs: List<EpgProgram>,
    val channelNames: Map<String, String>,
    val warnings: List<String>
) {
    val programCount: Int get() = programs.size
    val channelCount: Int get() = channelNames.size
}

object XmltvParser {

    private val XMLTV_DATE_FORMAT = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val XMLTV_DATE_FORMAT_NO_SPACE = SimpleDateFormat("yyyyMMddHHmmssZ", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun parse(xmlContent: String): XmltvParseResult {
        val programs = mutableListOf<EpgProgram>()
        val channelNames = mutableMapOf<String, String>()
        val warnings = mutableListOf<String>()

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false

            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlContent))

            var eventType = parser.eventType

            var currentChannelId: String? = null
            var currentChannelName: String? = null

            var progChannelId: String? = null
            var progStart: String? = null
            var progStop: String? = null
            var progTitle: String? = null
            var progDesc: String? = null
            var progCategory: String? = null

            var insideChannel = false
            var insideProgramme = false
            var currentTag: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val tagName = parser.name.lowercase(Locale.US)

                        when (tagName) {
                            "channel" -> {
                                insideChannel = true
                                currentChannelId = parser.getAttributeValue(null, "id")
                                currentChannelName = null
                            }

                            "display-name" -> {
                                if (insideChannel) {
                                    currentTag = "display-name"
                                }
                            }

                            "programme" -> {
                                insideProgramme = true
                                progChannelId = parser.getAttributeValue(null, "channel")
                                progStart = parser.getAttributeValue(null, "start")
                                progStop = parser.getAttributeValue(null, "stop")
                                progTitle = null
                                progDesc = null
                                progCategory = null
                            }

                            "title" -> {
                                if (insideProgramme) {
                                    currentTag = "title"
                                }
                            }

                            "desc" -> {
                                if (insideProgramme) {
                                    currentTag = "desc"
                                }
                            }

                            "category" -> {
                                if (insideProgramme) {
                                    currentTag = "category"
                                }
                            }

                            else -> {
                                currentTag = null
                            }
                        }
                    }

                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim().orEmpty()

                        if (text.isNotBlank()) {
                            when (currentTag) {
                                "display-name" -> {
                                    if (insideChannel && currentChannelName == null) {
                                        currentChannelName = text
                                    }
                                }

                                "title" -> {
                                    if (insideProgramme) {
                                        progTitle = text
                                    }
                                }

                                "desc" -> {
                                    if (insideProgramme) {
                                        progDesc = text
                                    }
                                }

                                "category" -> {
                                    if (insideProgramme) {
                                        progCategory = text
                                    }
                                }
                            }
                        }

                        currentTag = null
                    }

                    XmlPullParser.END_TAG -> {
                        val tagName = parser.name.lowercase(Locale.US)

                        when (tagName) {
                            "channel" -> {
                                if (insideChannel && currentChannelId != null) {
                                    channelNames[currentChannelId!!] =
                                        currentChannelName ?: currentChannelId!!
                                }

                                insideChannel = false
                                currentChannelId = null
                                currentChannelName = null
                                currentTag = null
                            }

                            "programme" -> {
                                if (insideProgramme) {
                                    val channelId = progChannelId
                                    val startMillis = parseXmltvDate(progStart)
                                    val endMillis = parseXmltvDate(progStop)
                                    val title = progTitle

                                    if (channelId != null && startMillis != null && endMillis != null && title != null) {
                                        programs.add(
                                            EpgProgram(
                                                channelId = channelId,
                                                title = title,
                                                description = progDesc.orEmpty(),
                                                startTimeMillis = startMillis,
                                                endTimeMillis = endMillis,
                                                category = progCategory.orEmpty()
                                            )
                                        )
                                    } else {
                                        warnings.add("Skipped programme: channel=$channelId title=$title")
                                    }
                                }

                                insideProgramme = false
                                progChannelId = null
                                progStart = null
                                progStop = null
                                progTitle = null
                                progDesc = null
                                progCategory = null
                                currentTag = null
                            }

                            else -> {
                                currentTag = null
                            }
                        }
                    }
                }

                eventType = parser.next()
            }
        } catch (e: Exception) {
            warnings.add("XML parse error: ${e.message}")
        }

        return XmltvParseResult(
            programs = programs,
            channelNames = channelNames,
            warnings = warnings
        )
    }

    private fun parseXmltvDate(dateString: String?): Long? {
        if (dateString.isNullOrBlank()) return null

        return try {
            XMLTV_DATE_FORMAT.parse(dateString)?.time
        } catch (_: Exception) {
            try {
                XMLTV_DATE_FORMAT_NO_SPACE.parse(dateString)?.time
            } catch (_: Exception) {
                null
            }
        }
    }
}