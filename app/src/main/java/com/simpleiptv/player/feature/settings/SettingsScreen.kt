package com.simpleiptv.player.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.simpleiptv.player.core.network.M3uPlaylistFetcher
import com.simpleiptv.player.core.repository.EpgSessionStore
import com.simpleiptv.player.core.repository.EpgSettingsStore
import com.simpleiptv.player.core.util.XmltvParser
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val epgSettingsStore = remember(context) {
        EpgSettingsStore(context)
    }

    var epgUrl by remember {
        mutableStateOf(epgSettingsStore.getEpgUrl())
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var statusMessage by remember {
        mutableStateOf<String?>(null)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var epgSummary by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(Unit) {
        if (EpgSessionStore.hasEpgData()) {
            epgSummary = "${EpgSessionStore.getAllPrograms().size} programs loaded for ${EpgSessionStore.getChannelNames().size} EPG channel(s)"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Configure EPG (Electronic Program Guide) and app options.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Divider()

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "EPG Source (XMLTV)",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Enter a URL pointing to an XMLTV EPG file. This file provides Now/Next program data for your channels.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = epgUrl,
                onValueChange = {
                    epgUrl = it
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri
                ),
                label = {
                    Text(text = "XMLTV EPG URL")
                },
                placeholder = {
                    Text(text = "https://example.com/epg.xml")
                }
            )

            Button(
                enabled = epgUrl.isNotBlank() && !isLoading,
                onClick = {
                    epgSettingsStore.setEpgUrl(epgUrl)

                    scope.launch {
                        isLoading = true
                        statusMessage = null
                        errorMessage = null
                        epgSummary = null

                        val fetchResult = M3uPlaylistFetcher.fetch(epgUrl.trim())

                        fetchResult
                            .onSuccess { rawXml ->
                                statusMessage = "Downloaded EPG (${formatBytes(rawXml.toByteArray().size)}). Parsing..."

                                val parseResult = XmltvParser.parse(rawXml)

                                EpgSessionStore.setEpgData(
                                    programs = parseResult.programs,
                                    channelNames = parseResult.channelNames
                                )

                                epgSummary = buildString {
                                    append("${parseResult.programCount} programs parsed")
                                    append(" for ${parseResult.channelCount} EPG channel(s)")

                                    if (parseResult.warnings.isNotEmpty()) {
                                        append(" (${parseResult.warnings.size} warning(s))")
                                    }
                                }

                                statusMessage = "EPG loaded successfully."
                            }
                            .onFailure { throwable ->
                                errorMessage = throwable.message ?: "Failed to fetch EPG."
                            }

                        isLoading = false
                    }
                }
            ) {
                Text(
                    text = if (isLoading) "Loading EPG..." else "Fetch & Load EPG"
                )
            }

            TextButton(
                onClick = {
                    epgUrl = ""
                    epgSettingsStore.clearEpgUrl()
                    EpgSessionStore.clear()
                    statusMessage = null
                    errorMessage = null
                    epgSummary = null
                }
            ) {
                Text(text = "Clear EPG Data")
            }
        }

        if (isLoading) {
            CircularProgressIndicator()
        }

        statusMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        errorMessage?.let { message ->
            Text(
                text = "Error: $message",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        epgSummary?.let { summary ->
            AssistChip(
                onClick = {},
                label = {
                    Text(text = summary)
                }
            )
        }

        Divider()

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "About",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Simple IPTV v1.0\nModern IPTV player for Android TV and mobile.\nBuilt with Kotlin, Jetpack Compose, and Media3.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatBytes(bytes: Int): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "%.2f MB".format(bytes / 1024f / 1024f)
    }
}