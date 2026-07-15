package com.simpleiptv.player.feature.series

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.simpleiptv.player.core.model.PlaylistSourceType
import com.simpleiptv.player.core.model.XtreamCategory
import com.simpleiptv.player.core.model.XtreamSeriesInfo
import com.simpleiptv.player.core.network.XtreamApiClient
import com.simpleiptv.player.core.repository.PlaylistSourcePreviewStore
import com.simpleiptv.player.core.repository.XtreamCredentialsStore
import com.simpleiptv.player.core.repository.SeriesSessionStore
import com.simpleiptv.player.ui.components.ChannelLogo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PAGE_SIZE = 50

@Composable
fun SeriesScreen(
    onOpenSeriesDetail: (XtreamSeriesInfo) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var allSeries by remember { mutableStateOf<List<XtreamSeriesInfo>>(emptyList()) }
    var categories by remember { mutableStateOf<List<XtreamCategory>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var displayLimit by remember { mutableStateOf(PAGE_SIZE) }

    suspend fun loadCategories() {
        isLoading = true
        statusMessage = null
        errorMessage = null
        categories = emptyList()
        allSeries = emptyList()

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

            XtreamApiClient.getSeriesCategories(credentials)
                .onSuccess { cats -> allCategories.addAll(cats) }
                .onFailure { throwable ->
                    errorMessage = "${source.name}: ${throwable.message}"
                }
        }

        categories = allCategories.distinctBy { it.id }.sortedBy { it.name }
        statusMessage = "${categories.size} series categories loaded. Select a category to browse."
        isLoading = false
    }

    suspend fun loadSeriesForCategory(categoryId: String?, categoryName: String?) {
        isLoading = true
        errorMessage = null
        allSeries = emptyList()
        displayLimit = PAGE_SIZE

        val playlistStore = PlaylistSourcePreviewStore(context)
        val xtreamStore = XtreamCredentialsStore(context)

        val xtreamSources = playlistStore.load().filter {
            it.isEnabled && it.type == PlaylistSourceType.XTREAM_CODES
        }

        val loadedSeries = mutableListOf<XtreamSeriesInfo>()

        xtreamSources.forEach { source ->
            val credentials = xtreamStore.getBySourceId(source.id) ?: return@forEach

            val url = if (categoryId != null) {
                "${credentials.baseApiUrl()}?username=${credentials.username}&password=${credentials.password}&action=get_series&category_id=$categoryId"
            } else {
                "${credentials.baseApiUrl()}?username=${credentials.username}&password=${credentials.password}&action=get_series"
            }

            withContext(Dispatchers.IO) {
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
                            val seriesId = item.optString("series_id", "").trim()
                            val name = item.optString("name", "").trim()
                            if (seriesId.isBlank() || name.isBlank()) continue

                            loadedSeries.add(
                                XtreamSeriesInfo(
                                    seriesId = seriesId,
                                    name = name,
                                    categoryId = item.optString("category_id", "").trim().ifBlank { null },
                                    cover = item.optString("cover", "").trim().ifBlank { null },
                                    rating = item.optString("rating", "").trim().ifBlank { null },
                                    plot = item.optString("plot", "").trim().ifBlank { null }
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

        allSeries = loadedSeries.distinctBy { it.seriesId }.sortedBy { it.name.lowercase() }
        statusMessage = "${allSeries.size} series in ${categoryName ?: "All"}."
        isLoading = false
    }

    LaunchedEffect(Unit) {
        loadCategories()
    }

    val query = searchQuery.text.trim()

    val visibleSeries = remember(allSeries, query) {
        if (query.isBlank()) {
            allSeries
        } else {
            allSeries.filter { series ->
                series.name.contains(query, ignoreCase = true) ||
                        series.plot.orEmpty().contains(query, ignoreCase = true)
            }
        }
    }

    val displayedSeries = remember(visibleSeries, displayLimit) {
        visibleSeries.take(displayLimit)
    }

    LazyColumn(
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Series", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Select a category to load series from Xtream Codes sources.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Button(
                enabled = !isLoading,
                onClick = {
                    scope.launch {
                        loadCategories()
                    }
                }
            ) {
                Text(text = if (isLoading) "Loading..." else "Refresh Categories")
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
                                        loadSeriesForCategory(cat.id, cat.name)
                                    }
                                },
                                label = { Text(text = cat.name) }
                            )
                        }
                    }
                }
            }
        }

        if (allSeries.isNotEmpty()) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(text = "Search series") }
                )
            }

            item {
                Text(
                    text = "Showing ${displayedSeries.size} of ${visibleSeries.size} series",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            items(
                items = displayedSeries,
                key = { it.seriesId }
            ) { series ->
                SeriesCard(
                    series = series,
                    onClick = {
                        SeriesSessionStore.selectSeries(series)
                        onOpenSeriesDetail(series)
                    }
                )
            }

            if (displayLimit < visibleSeries.size) {
                item {
                    TextButton(
                        onClick = {
                            displayLimit += PAGE_SIZE
                        }
                    ) {
                        Text(text = "Load More (${visibleSeries.size - displayLimit} remaining)")
                    }
                }
            }
        }
    }
}

@Composable
private fun SeriesCard(
    series: XtreamSeriesInfo,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ChannelLogo(
                logoUrl = series.cover,
                channelName = series.name
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = series.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                series.rating?.let {
                    Text(
                        text = "Rating: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                series.plot?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3
                    )
                }
            }
        }
    }
}