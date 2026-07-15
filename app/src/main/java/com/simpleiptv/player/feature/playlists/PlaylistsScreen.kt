package com.simpleiptv.player.feature.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import java.util.UUID
import com.simpleiptv.player.core.model.PlaylistSourcePreview
import com.simpleiptv.player.core.model.PlaylistSourceType

@Composable
fun PlaylistsScreen() {
    var selectedType by remember {
        mutableStateOf(PlaylistSourceType.M3U_URL)
    }

    var playlistName by remember {
        mutableStateOf("")
    }

    var m3uUrl by remember {
        mutableStateOf("")
    }

    var m3uText by remember {
        mutableStateOf("")
    }

    var xtreamServerUrl by remember {
        mutableStateOf("")
    }

    var xtreamUsername by remember {
        mutableStateOf("")
    }

    var xtreamPassword by remember {
        mutableStateOf("")
    }

    val playlists = remember {
        mutableStateListOf<`PlaylistSourcePreview`>()
    }

    val canAdd = when (selectedType) {
        PlaylistSourceType.M3U_URL -> playlistName.isNotBlank() && m3uUrl.isNotBlank()
        PlaylistSourceType.M3U_TEXT -> playlistName.isNotBlank() && m3uText.isNotBlank()
        PlaylistSourceType.XTREAM_CODES -> {
            playlistName.isNotBlank() &&
                    xtreamServerUrl.isNotBlank() &&
                    xtreamUsername.isNotBlank() &&
                    xtreamPassword.isNotBlank()
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
                text = "Playlist Sources",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Add multiple M3U playlists or Xtream Codes accounts. Later, Simple IPTV will let you browse each playlist separately or merge them into Browse All.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        PlaylistTypeSelector(
            selectedType = selectedType,
            onSelectedTypeChanged = {
                selectedType = it
            }
        )

        Divider()

        PlaylistForm(
            selectedType = selectedType,
            playlistName = playlistName,
            onPlaylistNameChanged = {
                playlistName = it
            },
            m3uUrl = m3uUrl,
            onM3uUrlChanged = {
                m3uUrl = it
            },
            m3uText = m3uText,
            onM3uTextChanged = {
                m3uText = it
            },
            xtreamServerUrl = xtreamServerUrl,
            onXtreamServerUrlChanged = {
                xtreamServerUrl = it
            },
            xtreamUsername = xtreamUsername,
            onXtreamUsernameChanged = {
                xtreamUsername = it
            },
            xtreamPassword = xtreamPassword,
            onXtreamPasswordChanged = {
                xtreamPassword = it
            }
        )

        Row {
            Button(
                enabled = canAdd,
                onClick = {
                    val description = when (selectedType) {
                        PlaylistSourceType.M3U_URL -> m3uUrl
                        PlaylistSourceType.M3U_TEXT -> "Pasted M3U playlist text"
                        PlaylistSourceType.XTREAM_CODES -> xtreamServerUrl
                    }

                    playlists.add(
                        `PlaylistSourcePreview`(
                            id = UUID.randomUUID().toString(),
                            name = playlistName.trim(),
                            type = selectedType,
                            description = description.trim(),
                            isEnabled = true
                        )
                    )

                    playlistName = ""
                    m3uUrl = ""
                    m3uText = ""
                    xtreamServerUrl = ""
                    xtreamUsername = ""
                    xtreamPassword = ""
                }
            ) {
                Text(text = "Add Playlist")
            }

            Spacer(modifier = Modifier.width(12.dp))

            TextButton(
                onClick = {
                    playlistName = ""
                    m3uUrl = ""
                    m3uText = ""
                    xtreamServerUrl = ""
                    xtreamUsername = ""
                    xtreamPassword = ""
                }
            ) {
                Text(text = "Clear")
            }
        }

        Divider()

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Added Sources",
                style = MaterialTheme.typography.titleLarge
            )

            if (playlists.isEmpty()) {
                Text(
                    text = "No playlists added yet. Add an M3U playlist or Xtream Codes account to continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                playlists.forEachIndexed { index, playlist ->
                    PlaylistPreviewCard(
                        playlist = playlist,
                        onToggleEnabled = { enabled ->
                            playlists[index] = playlist.copy(isEnabled = enabled)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistTypeSelector(
    selectedType: PlaylistSourceType,
    onSelectedTypeChanged: (PlaylistSourceType) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        val compact = maxWidth < 700.dp

        if (compact) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlaylistSourceType.entries.forEach { type ->
                    PlaylistTypeCard(
                        type = type,
                        selected = selectedType == type,
                        onClick = {
                            onSelectedTypeChanged(type)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlaylistSourceType.entries.forEach { type ->
                    PlaylistTypeCard(
                        type = type,
                        selected = selectedType == type,
                        onClick = {
                            onSelectedTypeChanged(type)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistForm(
    selectedType: PlaylistSourceType,
    playlistName: String,
    onPlaylistNameChanged: (String) -> Unit,
    m3uUrl: String,
    onM3uUrlChanged: (String) -> Unit,
    m3uText: String,
    onM3uTextChanged: (String) -> Unit,
    xtreamServerUrl: String,
    onXtreamServerUrlChanged: (String) -> Unit,
    xtreamUsername: String,
    onXtreamUsernameChanged: (String) -> Unit,
    xtreamPassword: String,
    onXtreamPasswordChanged: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Add ${selectedType.title}",
            style = MaterialTheme.typography.titleLarge
        )

        OutlinedTextField(
            value = playlistName,
            onValueChange = onPlaylistNameChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = {
                Text(text = "Playlist name")
            },
            placeholder = {
                Text(text = "Example: My IPTV Provider")
            }
        )

        when (selectedType) {
            PlaylistSourceType.M3U_URL -> {
                OutlinedTextField(
                    value = m3uUrl,
                    onValueChange = onM3uUrlChanged,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri
                    ),
                    label = {
                        Text(text = "M3U playlist URL")
                    },
                    placeholder = {
                        Text(text = "https://example.com/playlist.m3u")
                    }
                )
            }

            PlaylistSourceType.M3U_TEXT -> {
                OutlinedTextField(
                    value = m3uText,
                    onValueChange = onM3uTextChanged,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6,
                    label = {
                        Text(text = "M3U playlist text")
                    },
                    placeholder = {
                        Text(text = "#EXTM3U\n#EXTINF:-1, Example Channel\nhttps://example.com/live.m3u8")
                    },
                    colors = OutlinedTextFieldDefaults.colors()
                )
            }

            PlaylistSourceType.XTREAM_CODES -> {
                OutlinedTextField(
                    value = xtreamServerUrl,
                    onValueChange = onXtreamServerUrlChanged,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri
                    ),
                    label = {
                        Text(text = "Server URL")
                    },
                    placeholder = {
                        Text(text = "https://example.com:8080")
                    }
                )

                OutlinedTextField(
                    value = xtreamUsername,
                    onValueChange = onXtreamUsernameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = {
                        Text(text = "Username")
                    }
                )

                OutlinedTextField(
                    value = xtreamPassword,
                    onValueChange = onXtreamPasswordChanged,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    label = {
                        Text(text = "Password")
                    }
                )
            }
        }

        Text(
            text = "Note: This screen currently stores playlists only in memory. Persistent storage will be added with Room in the next database milestone.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}