package com.simpleiptv.player.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.simpleiptv.player.core.repository.AppSettingsStore
import com.simpleiptv.player.core.repository.BUFFER_OPTIONS
import com.simpleiptv.player.core.repository.ChannelSessionStore
import com.simpleiptv.player.core.repository.EpgSessionStore
import com.simpleiptv.player.core.repository.EpgSettingsStore
import com.simpleiptv.player.core.repository.FavoriteChannelStore
import com.simpleiptv.player.core.repository.PlaylistSourcePreviewStore
import com.simpleiptv.player.core.repository.ThemeMode
import com.simpleiptv.player.core.repository.XtreamCredentialsStore
import com.simpleiptv.player.core.network.EpgFetcher
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onThemeChanged: (ThemeMode) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val appSettingsStore = remember(context) { AppSettingsStore(context) }
    val epgSettingsStore = remember(context) { EpgSettingsStore(context) }
    val playlistStore = remember(context) { PlaylistSourcePreviewStore(context) }
    val favoriteStore = remember(context) { FavoriteChannelStore(context) }
    val xtreamStore = remember(context) { XtreamCredentialsStore(context) }

    var currentTheme by remember { mutableStateOf(appSettingsStore.getThemeMode()) }
    var currentBuffer by remember { mutableStateOf(appSettingsStore.getBufferDurationMs()) }

    var epgUrl by remember { mutableStateOf(epgSettingsStore.getEpgUrl()) }
    var isLoadingEpg by remember { mutableStateOf(false) }
    var epgStatus by remember { mutableStateOf<String?>(null) }
    var epgError by remember { mutableStateOf<String?>(null) }
    var epgSummary by remember { mutableStateOf<String?>(null) }

    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (EpgSessionStore.hasEpgData()) {
            epgSummary = "${EpgSessionStore.getAllPrograms().size} programs for ${EpgSessionStore.getChannelNames().size} EPG channel(s)"
        }
    }

    val playlistCount = remember { playlistStore.load().size }
    val favoriteCount = remember { favoriteStore.loadFavorites().size }
    val channelCount = remember { ChannelSessionStore.getChannelCount() }
    val xtreamCount = remember { xtreamStore.loadAll().size }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(text = "Clear All Data") },
            text = {
                Text(text = "This will clear all playlist sources, Xtream credentials, favorites, EPG data, and loaded channels. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        playlistStore.clear()
                        favoriteStore.clearFavorites()
                        xtreamStore.loadAll().forEach { entry -> xtreamStore.remove(entry.sourceId) }
                        epgSettingsStore.clearEpgUrl()
                        EpgSessionStore.clear()
                        ChannelSessionStore.clearAll()
                        epgUrl = ""
                        epgSummary = null
                        epgStatus = null
                        epgError = null
                        showClearDialog = false
                    }
                ) {
                    Text(text = "Clear Everything", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Settings", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Configure theme, playback, EPG, and manage app data.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Divider()

        // Theme Section
        SettingsSection(title = "Theme") {
            Text(
                text = "Choose how Simple IPTV looks.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = currentTheme == mode,
                        onClick = {
                            currentTheme = mode
                            appSettingsStore.setThemeMode(mode)
                            onThemeChanged(mode)
                        },
                        label = { Text(text = mode.label) }
                    )
                }
            }
        }

        Divider()

        // Buffer Section
        SettingsSection(title = "Playback Buffer") {
            Text(
                text = "Larger buffer = more stable playback but higher latency. Current: ${appSettingsStore.getBufferLabel()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BUFFER_OPTIONS.forEach { (ms, label) ->
                    FilterChip(
                        selected = currentBuffer == ms,
                        onClick = {
                            currentBuffer = ms
                            appSettingsStore.setBufferDurationMs(ms)
                        },
                        label = { Text(text = label) }
                    )
                }
            }
        }

        Divider()

        // EPG Section
        SettingsSection(title = "EPG Source (XMLTV)") {
            Text(
                text = "Enter a URL pointing to an XMLTV EPG file for Now/Next program data.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = epgUrl,
                onValueChange = { epgUrl = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                label = { Text(text = "XMLTV EPG URL") },
                placeholder = { Text(text = "https://example.com/epg.xml") }
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    enabled = epgUrl.isNotBlank() && !isLoadingEpg,
                    onClick = {
                        epgSettingsStore.setEpgUrl(epgUrl)
                        scope.launch {
                            isLoadingEpg = true
                            epgStatus = null
                            epgError = null
                            epgSummary = null

                            EpgFetcher.fetchAndParse(epgUrl.trim())
                                .onSuccess { result ->
                                    EpgSessionStore.setEpgData(result.programs, result.channelNames)
                                    epgSummary = "${result.programCount} programs for ${result.channelCount} channel(s)"
                                    epgStatus = "EPG loaded successfully."

                                    if (result.warnings.isNotEmpty()) {
                                        epgStatus = "EPG loaded with ${result.warnings.size} warning(s)."
                                    }
                                }
                                .onFailure { throwable ->
                                    epgError = throwable.message ?: "Failed to fetch EPG."
                                }

                            isLoadingEpg = false
                        }
                    }
                ) {
                    Text(text = if (isLoadingEpg) "Loading..." else "Fetch & Load EPG")
                }

                TextButton(
                    onClick = {
                        epgUrl = ""
                        epgSettingsStore.clearEpgUrl()
                        EpgSessionStore.clear()
                        epgStatus = null
                        epgError = null
                        epgSummary = null
                    }
                ) {
                    Text(text = "Clear EPG")
                }
            }

            if (isLoadingEpg) CircularProgressIndicator()

            epgStatus?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }

            epgError?.let {
                Text(text = "Error: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }

            epgSummary?.let {
                AssistChip(onClick = {}, label = { Text(text = it) })
            }
        }

        Divider()

        // Data Management
        SettingsSection(title = "Data Management") {
            Text(
                text = "Manage stored data across the app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            DataInfoCard(
                items = listOf(
                    "Playlist sources" to "$playlistCount",
                    "Xtream accounts" to "$xtreamCount",
                    "Loaded channels" to "$channelCount",
                    "Favorite channels" to "$favoriteCount",
                    "EPG programs" to "${EpgSessionStore.getAllPrograms().size}",
                    "EPG channels" to "${EpgSessionStore.getChannelNames().size}"
                )
            )

            OutlinedButton(
                onClick = { showClearDialog = true }
            ) {
                Text(text = "Clear All Data")
            }
        }

        Divider()

        // About
        SettingsSection(title = "About") {
            Text(
                text = "Simple IPTV v1.0",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Modern IPTV player for Android TV and mobile.\n\n" +
                        "Built with:\n" +
                        "• Kotlin\n" +
                        "• Jetpack Compose\n" +
                        "• Media3 (ExoPlayer)\n" +
                        "• Material 3\n\n" +
                        "Features:\n" +
                        "• M3U playlist support\n" +
                        "• Xtream Codes API\n" +
                        "• EPG (XMLTV)\n" +
                        "• Catch-up / Timeshift\n" +
                        "• Channel favorites\n" +
                        "• Multi-source browsing\n" +
                        "• Android TV remote support\n" +
                        "• Fullscreen player\n" +
                        "• Search across all sources",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        content()
    }
}

@Composable
private fun DataInfoCard(
    items: List<Pair<String, String>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items.forEach { (label, value) ->
                DataInfoRow(label = label, value = value)
            }
        }
    }
}

@Composable
private fun DataInfoRow(
    label: String,
    value: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}