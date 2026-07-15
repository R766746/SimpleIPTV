package com.simpleiptv.player.feature.movies

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.simpleiptv.player.core.model.Channel
import com.simpleiptv.player.core.model.PlaylistSourceType
import com.simpleiptv.player.core.model.XtreamCategory
import com.simpleiptv.player.core.network.XtreamApiClient
import com.simpleiptv.player.core.repository.ChannelSessionStore
import com.simpleiptv.player.core.repository.PlaylistSourcePreviewStore
import com.simpleiptv.player.core.repository.XtreamCredentialsStore
import com.simpleiptv.player.feature.live.LiveChannelCard
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 50

@Composable
fun MoviesScreen(
    onOpenPlayer: (Channel) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var allMovies by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var categories by remember { mutableStateOf<List<XtreamCategory>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var displayLimit by remember { mutableStateOf(PAGE_SIZE) }
    var categoriesLoaded by remember { mutableStateOf(false) }
    var moviesLoadedForCategory by remember { mutableStateOf<String?>("__none__") }

    suspend fun loadCategories() {
        isLoading = true
        statusMessage = null
        errorMessage = null
        categories = emptyList()
        allMovies = emptyList()

        val playlistStore = PlaylistSourcePreviewStore(context)
        val xtreamStore = XtreamCredentialsStore(context)

        val xtreamSources = playlistStore.load().filter {
            it.isEnabled && it.type == PlaylistSourceType.XTREAM_CODES
        }

        if (xtreamSources.isEmpty()) {
            statusMessage = "No enabled Xtream Codes sources found."
            isLoading = false
            return
        }

        val allCategories = mutableListOf<XtreamCategory>()

        xtreamSources.forEach { source ->
            val credentials = xtreamStore.getBySourceId(source.id) ?: return@forEach

            XtreamApiClient.getVodCategories(credentials)
                .onSuccess { cats -> allCategories.addAll(cats) }
                .onFailure { throwable ->
                    errorMessage = "${source.name}: ${throwable.message}"
                }
        }

        categories = allCategories.distinctBy { it.id }.sortedBy { it.name }
        categoriesLoaded = true
        statusMessage = "${categories.size} movie categories loaded. Select a category to browse."
        isLoading = false
    }

    suspend fun loadMoviesForCategory(categoryId: String?, categoryName: String?) {
        isLoading = true
        errorMessage = null
        allMovies = emptyList()
        displayLimit = PAGE_SIZE

        val playlistStore = PlaylistSourcePreviewStore(context)
        val xtreamStore = XtreamCredentialsStore(context)

        val xtreamSources = playlistStore.load().filter {
            it.isEnabled && it.type == PlaylistSourceType.XTREAM_CODES
        }

        val loadedMovies = mutableListOf<Channel>()

        xtreamSources.forEach { source ->
            val credentials = xtreamStore.getBySourceId(source.id) ?: return@forEach

            val url = if (categoryId != null) {
                "${credentials.baseApiUrl()}?username=${credentials.username}&password=${credentials.password}&action=get_vod_streams&category_id=$categoryId"
            } else {
                "${credentials.baseApiUrl()}?username=${credentials.username}&password=${credentials.password}&action=get_vod_streams"
            }

            com.simpleiptv.player.core.network.XtreamApiClient.let {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    runCatching {
                        val connection = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
                            requestMethod = "GET"
                            connectTimeout = 15_000
                            readTimeout = 30_000
                            instanceFollowRedirects = true
                            setRequestProperty("User-Agent", "SimpleIPTV/1.0 Android")
                        }

                        try {
                            val responseCode = connection.responseCode
                            if (responseCode !in 200..299) {
                                throw IllegalStateException("HTTP $responseCode")
                            }

                            val reader = connection.inputStream.bufferedReader()
                            val array = org.json.JSONArray(reader.readText())
                            reader.close()

                            for (index in 0 until array.length()) {
                                val item = array.optJSONObject(index) ?: continue
                                val streamId = item.optString("stream_id", "").trim()
                                val name = item.optString("name", "").trim()
                                if (streamId.isBlank() || name.isBlank()) continue

                                val extension = item.optString("container_extension", "mp4").trim().ifBlank { "mp4" }
                                val streamUrl = credentials.vodStreamUrl(streamId, extension)
                                val icon = item.optString("stream_icon", "").trim().ifBlank { null }

                                loadedMovies.add(
                                    Channel(
                                        id = "vod_${source.id}_$streamId",
                                        name = name,
                                        streamUrl = streamUrl,
                                        tvgId = null,
                                        tvgName = name,
                                        logoUrl = icon,
                                        groupTitle = categoryName ?: "All Movies",
                                        playlistSourceId = source.id,
                                        playlistName = source.name
                                    )
                                )
                            }
                        } finally {
                            connection.disconnect()
                        }
                    }.onFailure { throwable ->
                        errorMessage = "${source.name}: ${throwable.message}"
                    }
                }
            }
        }

        allMovies = loadedMovies.distinctBy { it.streamUrl }.sortedBy { it.name.lowercase() }
        moviesLoadedForCategory = categoryName ?: "All"
        statusMessage = "${allMovies.size} movie(s) in ${categoryName ?: "All"}."
        isLoading = false
    }

    LaunchedEffect(Unit) {
        loadCategories()
    }

    val query = searchQuery.text.trim()

    val visibleMovies = remember(allMovies, query) {
        if (query.isBlank()) {
            allMovies
        } else {
            allMovies.filter { channel ->
                channel.name.contains(query, ignoreCase = true)
            }
        }
    }

    val displayedMovies = remember(visibleMovies, displayLimit) {
        visibleMovies.take(displayLimit)
    }

    LazyColumn(
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Movies", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Select a category to load movies from Xtream Codes sources.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    enabled = !isLoading,
                    onClick = {
                        scope.launch {
                            categoriesLoaded = false
                            loadCategories()
                        }
                    }
                ) {
                    Text(text = if (isLoading) "Loading..." else "Refresh Categories")
                }
            }
        }

        if (isLoading) {
            item { CircularProgressIndicator() }
        }

        statusMessage?.let { msg ->
            item { AssistChip(onClick = {}, label = { Text(text = msg) }) }
        }

        errorMessage?.let { msg ->
            item {
                Text(
                    text = "Error: $msg",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        if (categories.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat.name,
                                onClick = {
                                    selectedCategory = cat.name
                                    displayLimit = PAGE_SIZE
                                    searchQuery = TextFieldValue("")
                                    scope.launch {
                                        loadMoviesForCategory(cat.id, cat.name)
                                    }
                                },
                                label = { Text(text = cat.name) }
                            )
                        }
                    }
                }
            }
        }

        if (allMovies.isNotEmpty()) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(text = "Search movies") }
                )
            }

            item {
                Text(
                    text = "Showing ${displayedMovies.size} of ${visibleMovies.size} movie(s)",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            items(
                items = displayedMovies,
                key = { it.id }
            ) { movie ->
                LiveChannelCard(
                    channel = movie,
                    onClick = {
                        ChannelSessionStore.setChannels(displayedMovies)
                        ChannelSessionStore.selectChannel(movie)
                        onOpenPlayer(movie)
                    }
                )
            }

            if (displayLimit < visibleMovies.size) {
                item {
                    TextButton(
                        onClick = {
                            displayLimit += PAGE_SIZE
                        }
                    ) {
                        Text(text = "Load More (${visibleMovies.size - displayLimit} remaining)")
                    }
                }
            }
        }
    }
}