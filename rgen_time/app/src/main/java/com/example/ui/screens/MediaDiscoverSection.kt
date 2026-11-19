package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.DownloadStatus
import com.example.model.DownloadTaskState
import com.example.model.MediaSourceType
import com.example.model.MediaTrackItem
import com.example.ui.components.ChamferedHudShape
import com.example.ui.theme.InkCream
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalNekoPalette
import com.example.ui.theme.StageBorder
import com.example.ui.theme.StageCard
import com.example.ui.theme.StageVoid
import com.example.viewmodel.MediaViewModel

@Composable
fun MediaDiscoverSection(
    mediaViewModel: MediaViewModel,
    searchSource: MediaSourceType,
    searchQuery: String,
    searchResults: List<MediaTrackItem>,
    isSearching: Boolean,
    searchError: String?,
    resolvingTrackId: String?,
    downloads: List<DownloadTaskState>
) {
    val palette = LocalNekoPalette.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val hudShape = remember { ChamferedHudShape(14.dp) }
    val chipShape = remember { ChamferedHudShape(8.dp) }

    val onlineSources = remember {
        listOf(MediaSourceType.YOUTUBE, MediaSourceType.YOUTUBE_MUSIC, MediaSourceType.GOOGLE)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 18.dp)
    ) {
        item {
            Card(
                shape = hudShape,
                colors = CardDefaults.cardColors(containerColor = StageCard.copy(alpha = 0.86f)),
                border = BorderStroke(1.dp, palette.primaryGlow.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Búsqueda en YouTube, YouTube Music & Google",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.primaryGlow
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        onlineSources.forEach { src ->
                            FilterChip(
                                selected = searchSource == src,
                                onClick = { mediaViewModel.setSearchSource(src) },
                                shape = chipShape,
                                label = { Text(src.label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.primaryGlow,
                                    selectedLabelColor = StageVoid,
                                    containerColor = StageVoid,
                                    labelColor = InkCream
                                ),
                                modifier = Modifier.testTag("discover_source_${src.id}")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = mediaViewModel::setSearchQuery,
                            placeholder = { Text("Buscar canción, artista o pegar enlace URL…", fontSize = 12.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { mediaViewModel.performSearch() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.primaryGlow,
                                unfocusedBorderColor = StageBorder,
                                focusedTextColor = InkCream,
                                unfocusedTextColor = InkCream
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("discover_search_input")
                        )

                        Button(
                            onClick = { mediaViewModel.performSearch() },
                            enabled = !isSearching && searchQuery.isNotBlank(),
                            shape = chipShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primaryGlow,
                                contentColor = StageVoid
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("discover_search_btn")
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = StageVoid,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Filled.Search, contentDescription = "Buscar", modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    if (!searchError.isNullOrBlank()) {
                        Text(
                            text = searchError,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF6B4A)
                        )
                    }
                }
            }
        }

        if (searchResults.isNotEmpty()) {
            item {
                Text(
                    text = "RESULTADOS (${searchResults.size})",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = palette.secondaryGlow
                )
            }

            items(searchResults, key = { it.id }) { track ->
                val isResolving = resolvingTrackId == track.id
                Card(
                    shape = hudShape,
                    colors = CardDefaults.cardColors(containerColor = StageCard.copy(alpha = 0.84f)),
                    border = BorderStroke(1.dp, StageBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(chipShape)
                                    .background(StageVoid)
                                    .border(1.dp, palette.secondaryGlow.copy(alpha = 0.4f), chipShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!track.thumbnailUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(track.thumbnailUrl).crossfade(true).build(),
                                        contentDescription = track.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (track.isVideo) Icons.Filled.VideoLibrary else Icons.Filled.MusicNote,
                                        contentDescription = null,
                                        tint = palette.secondaryGlow
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = InkCream,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${track.artist} · ${track.sourceType.label}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (track.durationMs > 0L) {
                                    Text(
                                        text = formatMediaTime(track.durationMs),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = palette.secondaryGlow
                                    )
                                }
                            }

                            if (isResolving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = palette.primaryGlow,
                                    strokeWidth = 2.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { mediaViewModel.loadTrack(track, openInMusicStudio = false, forDeckB = false) },
                                enabled = !isResolving,
                                shape = chipShape,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = palette.primaryGlow, contentColor = StageVoid),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Video", fontSize = 11.sp, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = { mediaViewModel.loadTrack(track, openInMusicStudio = true, forDeckB = false) },
                                enabled = !isResolving,
                                shape = chipShape,
                                border = BorderStroke(1.dp, palette.secondaryGlow),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = palette.secondaryGlow, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Música", fontSize = 11.sp, color = palette.secondaryGlow, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = { mediaViewModel.loadTrack(track, openInMusicStudio = true, forDeckB = true) },
                                enabled = !isResolving,
                                shape = chipShape,
                                border = BorderStroke(1.dp, StageBorder),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.Album, contentDescription = "Deck B", tint = InkCream, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("B", fontSize = 11.sp, color = InkCream)
                            }

                            OutlinedButton(
                                onClick = { mediaViewModel.requestDownloadOptions(track) },
                                shape = chipShape,
                                border = BorderStroke(1.dp, palette.primaryGlow.copy(alpha = 0.7f)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("download_btn_${track.id}")
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = "Descargar", tint = palette.primaryGlow, modifier = Modifier.size(15.dp))
                            }

                            val webTarget = track.pageUrl ?: track.streamUrl
                            if (!webTarget.isNullOrBlank()) {
                                IconButton(
                                    onClick = { runCatching { uriHandler.openUri(webTarget) } },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Filled.OpenInBrowser, contentDescription = "Abrir en navegador", tint = InkMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (downloads.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "DESCARGAS & ARCHIVOS MULTIMEDIA (${downloads.size})",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = palette.primaryGlow
                )
            }

            items(downloads, key = { it.id }) { task ->
                Card(
                    shape = hudShape,
                    colors = CardDefaults.cardColors(containerColor = StageCard.copy(alpha = 0.86f)),
                    border = BorderStroke(
                        1.dp,
                        when (task.status) {
                            DownloadStatus.COMPLETED -> palette.secondaryGlow.copy(alpha = 0.55f)
                            DownloadStatus.FAILED -> Color(0xFFFF6B4A)
                            else -> palette.primaryGlow.copy(alpha = 0.6f)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = InkCream,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when (task.status) {
                                        DownloadStatus.QUEUED -> "En cola · ${task.qualityLabel}"
                                        DownloadStatus.DOWNLOADING -> "Descargando ${(task.progress * 100).toInt()}% · ${task.qualityLabel}"
                                        DownloadStatus.COMPLETED -> "Guardado (.${task.formatKind.ext}) · ${task.qualityLabel}"
                                        DownloadStatus.FAILED -> "Error: ${task.errorMessage ?: "Fallo de red"}"
                                    },
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = if (task.status == DownloadStatus.FAILED) Color(0xFFFF6B4A) else InkMuted
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (task.status == DownloadStatus.COMPLETED && !task.localFilePath.isNullOrBlank()) {
                                    Surface(
                                        onClick = { mediaViewModel.playDownloadedItem(task) },
                                        shape = chipShape,
                                        color = palette.primaryGlow.copy(alpha = 0.2f),
                                        border = BorderStroke(0.8.dp, palette.primaryGlow)
                                    ) {
                                        Text(
                                            text = "REPRODUCIR",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = palette.primaryGlow,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                                IconButton(onClick = { mediaViewModel.deleteDownload(task.id) }) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar descarga", tint = InkMuted)
                                }
                            }
                        }

                        if (task.status == DownloadStatus.DOWNLOADING) {
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { task.progress.coerceIn(0f, 1f) },
                                color = palette.primaryGlow,
                                trackColor = StageVoid,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
