package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.DownloadFormatOption
import com.example.model.MediaTabSection
import com.example.model.MediaTrackItem
import com.example.ui.components.ChamferedHudShape
import com.example.ui.components.CurvedSciFiHudBanner
import com.example.ui.theme.InkCream
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalNekoPalette
import com.example.ui.theme.StageBorder
import com.example.ui.theme.StageCard
import com.example.ui.theme.StageCardElevated
import com.example.ui.theme.StageVoid
import com.example.viewmodel.MediaViewModel

@Composable
fun MediaHubScreen(
    mediaViewModel: MediaViewModel,
    batteryOptimized: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalNekoPalette.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val activeSection by mediaViewModel.activeSection.collectAsStateWithLifecycle()
    val playback by mediaViewModel.playback.collectAsStateWithLifecycle()
    val videoFormat by mediaViewModel.videoFormat.collectAsStateWithLifecycle()
    val videoResizeMode by mediaViewModel.videoResizeMode.collectAsStateWithLifecycle()
    val isVideoFullscreen by mediaViewModel.isVideoFullscreen.collectAsStateWithLifecycle()
    val eqState by mediaViewModel.equalizerState.collectAsStateWithLifecycle()
    val mixerState by mediaViewModel.mixerState.collectAsStateWithLifecycle()
    val lyrics by mediaViewModel.lyrics.collectAsStateWithLifecycle()
    val customLyricsRaw by mediaViewModel.customLyricsRaw.collectAsStateWithLifecycle()
    val isLoadingLyrics by mediaViewModel.isLoadingLyrics.collectAsStateWithLifecycle()
    val searchSource by mediaViewModel.searchSource.collectAsStateWithLifecycle()
    val searchQuery by mediaViewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by mediaViewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by mediaViewModel.isSearching.collectAsStateWithLifecycle()
    val searchError by mediaViewModel.searchError.collectAsStateWithLifecycle()
    val resolvingTrackId by mediaViewModel.resolvingTrackId.collectAsStateWithLifecycle()
    val downloads by mediaViewModel.downloads.collectAsStateWithLifecycle()
    val downloadTargetTrack by mediaViewModel.downloadTargetTrack.collectAsStateWithLifecycle()
    val availableDownloadFormats by mediaViewModel.availableDownloadFormats.collectAsStateWithLifecycle()
    val isResolvingDownload by mediaViewModel.isResolvingDownload.collectAsStateWithLifecycle()

    // Sync screen orientation only when user enables explicit fullscreen mode
    DisposableEffect(isVideoFullscreen, videoFormat.isLandscape) {
        val activity = context as? Activity
        if (isVideoFullscreen) {
            activity?.requestedOrientation = if (videoFormat.isLandscape) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    if (isVideoFullscreen) {
        BackHandler {
            mediaViewModel.setVideoFullscreen(false)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(StageVoid)
        ) {
            MediaVideoSurface(
                player = mediaViewModel.engine.mainPlayer,
                format = videoFormat,
                resizeMode = videoResizeMode,
                isFullscreen = true,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                shape = ChamferedHudShape(10.dp),
                color = StageCardElevated.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, palette.primaryGlow.copy(alpha = 0.6f)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = videoFormat.label,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = palette.primaryGlow
                    )
                    IconButton(onClick = { mediaViewModel.setVideoFullscreen(false) }) {
                        Icon(
                            imageVector = Icons.Filled.FullscreenExit,
                            contentDescription = "Salir de pantalla completa",
                            tint = InkCream
                        )
                    }
                }
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 620.dp)) {
            Text(
                text = "ESTACIÓN MULTIMEDIA & MÚSICA",
                style = MaterialTheme.typography.headlineSmall,
                color = InkCream
            )

            Spacer(modifier = Modifier.height(6.dp))

            CurvedSciFiHudBanner(
                primaryText = playback.currentTrack?.title ?: "REPRODUCTOR, EQ HARDWARE & DESCARGAS",
                secondaryText = "${activeSection.label.uppercase()} · ${videoFormat.label.uppercase()}"
            )

            Spacer(modifier = Modifier.height(8.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                MediaTabSection.entries.forEachIndexed { idx, section ->
                    SegmentedButton(
                        selected = activeSection == section,
                        onClick = { mediaViewModel.selectSection(section) },
                        shape = SegmentedButtonDefaults.itemShape(idx, MediaTabSection.entries.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = palette.primaryGlow,
                            activeContentColor = StageVoid,
                            inactiveContainerColor = StageCard.copy(alpha = 0.85f),
                            inactiveContentColor = InkCream,
                            activeBorderColor = palette.primaryGlow,
                            inactiveBorderColor = StageBorder
                        ),
                        modifier = Modifier.testTag("media_section_${section.name.lowercase()}")
                    ) {
                        Text(
                            text = section.label,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(modifier = Modifier.weight(1f)) {
                when (activeSection) {
                    MediaTabSection.PLAYER -> VideoPlayerSection(
                        mediaViewModel = mediaViewModel,
                        playback = playback,
                        videoFormat = videoFormat,
                        resizeMode = videoResizeMode,
                        mixerState = mixerState,
                        downloads = downloads
                    )

                    MediaTabSection.MUSIC -> MusicStudioSection(
                        mediaViewModel = mediaViewModel,
                        playback = playback,
                        eqState = eqState,
                        mixerState = mixerState,
                        lyrics = lyrics,
                        customLyricsRaw = customLyricsRaw,
                        isLoadingLyrics = isLoadingLyrics,
                        batteryOptimized = batteryOptimized
                    )

                    MediaTabSection.DISCOVER -> MediaDiscoverSection(
                        mediaViewModel = mediaViewModel,
                        searchSource = searchSource,
                        searchQuery = searchQuery,
                        searchResults = searchResults,
                        isSearching = isSearching,
                        searchError = searchError,
                        resolvingTrackId = resolvingTrackId,
                        downloads = downloads
                    )
                }
            }
        }
    }

    val dlTrack = downloadTargetTrack
    if (dlTrack != null) {
        DownloadFormatPickerDialog(
            track = dlTrack,
            formats = availableDownloadFormats,
            isLoading = isResolvingDownload,
            onSelectFormat = { opt -> mediaViewModel.startDownload(dlTrack, opt) },
            onOpenWeb = {
                val url = dlTrack.pageUrl ?: dlTrack.streamUrl
                if (!url.isNullOrBlank()) {
                    runCatching { uriHandler.openUri(url) }
                }
                mediaViewModel.dismissDownloadDialog()
            },
            onDismiss = mediaViewModel::dismissDownloadDialog
        )
    }
}

@Composable
private fun DownloadFormatPickerDialog(
    track: MediaTrackItem,
    formats: List<DownloadFormatOption>,
    isLoading: Boolean,
    onSelectFormat: (DownloadFormatOption) -> Unit,
    onOpenWeb: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalNekoPalette.current
    val shape = remember { ChamferedHudShape(10.dp) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StageCardElevated,
        titleContentColor = InkCream,
        textContentColor = InkCream,
        title = {
            Column {
                Text("DESCARGAR MULTIMEDIA", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${track.title} · ${track.artist}",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            when {
                isLoading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = palette.primaryGlow,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Analizando formatos MP4 / WEBM / M4A / OPUS…", fontSize = 12.sp)
                    }
                }

                formats.isEmpty() -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "No se pudo obtener un flujo de descarga directa para este enlace. Puede abrirlo en el navegador o usar otro enlace compatible.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkMuted
                        )
                        OutlinedButton(
                            onClick = onOpenWeb,
                            shape = shape,
                            border = BorderStroke(1.dp, palette.secondaryGlow)
                        ) {
                            Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Abrir origen en navegador")
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(formats, key = { it.id }) { option ->
                            Surface(
                                onClick = { onSelectFormat(option) },
                                shape = shape,
                                color = StageCard,
                                border = BorderStroke(
                                    1.dp,
                                    if (option.formatKind.isVideo) palette.primaryGlow.copy(alpha = 0.6f)
                                    else palette.secondaryGlow.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("download_option_${option.formatKind.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Download,
                                        contentDescription = null,
                                        tint = if (option.formatKind.isVideo) palette.primaryGlow else palette.secondaryGlow,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = option.qualityLabel,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = InkCream
                                        )
                                        val sizeMb = if (option.contentLengthBytes > 0L) {
                                            " · %.1f MB".format(option.contentLengthBytes / (1024f * 1024f))
                                        } else ""
                                        Text(
                                            text = "Formato .${option.formatKind.ext.uppercase()}$sizeMb",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 10.sp,
                                            color = InkMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = palette.primaryGlow)
            }
        }
    )
}
