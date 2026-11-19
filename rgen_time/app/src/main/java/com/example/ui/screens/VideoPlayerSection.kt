package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOn
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.media.PlayerPlaybackSnapshot
import com.example.model.DownloadStatus
import com.example.model.DownloadTaskState
import com.example.model.MixerState
import com.example.model.VideoOrientationFormat
import com.example.model.VideoResizeMode
import com.example.ui.components.ChamferedHudShape
import com.example.ui.theme.InkCream
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalNekoPalette
import com.example.ui.theme.StageBorder
import com.example.ui.theme.StageCard
import com.example.ui.theme.StageVoid
import com.example.viewmodel.MediaViewModel
import java.util.Locale

@Composable
fun VideoPlayerSection(
    mediaViewModel: MediaViewModel,
    playback: PlayerPlaybackSnapshot,
    videoFormat: VideoOrientationFormat,
    resizeMode: VideoResizeMode,
    mixerState: MixerState,
    downloads: List<DownloadTaskState>
) {
    val palette = LocalNekoPalette.current
    val hudShape = remember { ChamferedHudShape(14.dp) }
    val chipShape = remember { ChamferedHudShape(8.dp) }

    val localMediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            mediaViewModel.loadLocalMediaUri(uri, targetDeckB = false, openMusicTab = false)
        }
    }

    val completedDownloads = remember(downloads) {
        downloads.filter { it.status == DownloadStatus.COMPLETED }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Video Frame Container supporting Horizontal (16:9, 21:9) & Vertical (9:16, 4:5, 1:1)
        Card(
            shape = hudShape,
            colors = CardDefaults.cardColors(containerColor = StageCard.copy(alpha = 0.88f)),
            border = BorderStroke(1.dp, palette.primaryGlow.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = if (videoFormat.isLandscape) 240.dp else 400.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .then(
                                if (videoFormat.isLandscape) {
                                    Modifier.fillMaxWidth().aspectRatio(videoFormat.aspectRatio)
                                } else {
                                    Modifier.height(340.dp).aspectRatio(videoFormat.aspectRatio)
                                }
                            )
                            .clip(ChamferedHudShape(10.dp))
                            .background(StageVoid)
                            .border(1.dp, palette.secondaryGlow.copy(alpha = 0.5f), ChamferedHudShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        MediaVideoSurface(
                            player = mediaViewModel.engine.mainPlayer,
                            format = videoFormat,
                            resizeMode = resizeMode,
                            isFullscreen = false,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (playback.currentTrack == null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.VideoFile,
                                    contentDescription = null,
                                    tint = palette.secondaryGlow.copy(alpha = 0.7f),
                                    modifier = Modifier.size(38.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Sin archivo activo · Formato ${videoFormat.label}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkMuted
                                )
                            }
                        }

                        if (playback.isBuffering) {
                            CircularProgressIndicator(
                                color = palette.primaryGlow,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Seekbar + Transport
                val duration = playback.durationMs.coerceAtLeast(1L)
                val pos = playback.positionMs.coerceIn(0L, duration)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatMediaTime(pos),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = palette.primaryGlow
                    )
                    Text(
                        text = playback.currentTrack?.title ?: "Listo para reproducir",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkCream,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    Text(
                        text = formatMediaTime(playback.durationMs),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = InkMuted
                    )
                }

                Slider(
                    value = if (playback.durationMs > 0L) pos.toFloat() / duration.toFloat() else 0f,
                    onValueChange = { frac ->
                        if (playback.durationMs > 0L) {
                            mediaViewModel.seekTo((frac * duration).toLong())
                        }
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = palette.primaryGlow,
                        activeTrackColor = palette.primaryGlow
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { mediaViewModel.togglePlayPause() },
                            shape = chipShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primaryGlow,
                                contentColor = StageVoid
                            ),
                            modifier = Modifier.testTag("video_play_pause_btn")
                        ) {
                            Icon(
                                imageVector = if (playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Reproducir o pausar",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (playback.isPlaying) "Pausar" else "Reproducir", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { localMediaPicker.launch(arrayOf("video/*", "audio/*")) },
                            shape = chipShape,
                            border = BorderStroke(1.dp, palette.secondaryGlow),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.testTag("video_pick_local_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FolderOpen,
                                contentDescription = null,
                                tint = palette.secondaryGlow,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Archivo", fontSize = 12.sp, color = palette.secondaryGlow)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { mediaViewModel.setLoopEnabled(!playback.loopEnabled) }) {
                            Icon(
                                imageVector = if (playback.loopEnabled) Icons.Filled.RepeatOn else Icons.Filled.Repeat,
                                contentDescription = "Repetir",
                                tint = if (playback.loopEnabled) palette.secondaryGlow else InkMuted
                            )
                        }
                        IconButton(
                            onClick = {
                                val next = if (videoFormat.isLandscape) {
                                    VideoOrientationFormat.VERTICAL_9_16
                                } else {
                                    VideoOrientationFormat.HORIZONTAL_16_9
                                }
                                mediaViewModel.setVideoFormat(next)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ScreenRotation,
                                contentDescription = "Alternar Horizontal/Vertical",
                                tint = palette.primaryGlow
                            )
                        }
                        IconButton(
                            onClick = { mediaViewModel.setVideoFullscreen(true) },
                            modifier = Modifier.testTag("video_fullscreen_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Fullscreen,
                                contentDescription = "Pantalla completa",
                                tint = InkCream
                            )
                        }
                    }
                }
            }
        }

        // Horizontal & Vertical Aspect / Resize / Speed Parameters
        Card(
            shape = hudShape,
            colors = CardDefaults.cardColors(containerColor = StageCard.copy(alpha = 0.84f)),
            border = BorderStroke(1.dp, StageBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Parámetros Horizontal / Vertical",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.secondaryGlow
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(VideoOrientationFormat.entries) { fmt ->
                        FilterChip(
                            selected = videoFormat == fmt,
                            onClick = { mediaViewModel.setVideoFormat(fmt) },
                            shape = chipShape,
                            label = { Text(fmt.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.primaryGlow,
                                selectedLabelColor = StageVoid,
                                containerColor = StageVoid,
                                labelColor = InkCream
                            ),
                            modifier = Modifier.testTag("video_fmt_${fmt.id}")
                        )
                    }
                }

                Text("Ajuste de lienzo", style = MaterialTheme.typography.labelLarge, color = InkMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VideoResizeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = resizeMode == mode,
                            onClick = { mediaViewModel.setVideoResizeMode(mode) },
                            shape = chipShape,
                            label = { Text(mode.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.secondaryGlow.copy(alpha = 0.25f),
                                selectedLabelColor = palette.secondaryGlow,
                                containerColor = StageVoid,
                                labelColor = InkCream
                            ),
                            modifier = Modifier.testTag("video_resize_${mode.id}")
                        )
                    }
                }

                Text(
                    text = "Velocidad de reproducción · ${String.format(Locale.US, "%.2fx", mixerState.playbackSpeed)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkMuted
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                        FilterChip(
                            selected = kotlin.math.abs(mixerState.playbackSpeed - spd) < 0.05f,
                            onClick = { mediaViewModel.setPlaybackSpeed(spd) },
                            shape = chipShape,
                            label = { Text("${spd}x", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        if (completedDownloads.isNotEmpty()) {
            Card(
                shape = hudShape,
                colors = CardDefaults.cardColors(containerColor = StageCard.copy(alpha = 0.84f)),
                border = BorderStroke(1.dp, StageBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Biblioteca Local Descargada (${completedDownloads.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.primaryGlow
                    )
                    completedDownloads.take(6).forEach { item ->
                        Surface(
                            onClick = { mediaViewModel.playDownloadedItem(item) },
                            shape = chipShape,
                            color = StageVoid.copy(alpha = 0.72f),
                            border = BorderStroke(0.8.dp, StageBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = InkCream,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.formatKind.label} · ${item.artist}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = palette.secondaryGlow
                                    )
                                }
                                Text(
                                    text = "ABRIR",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = palette.primaryGlow
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@OptIn(UnstableApi::class)
@Composable
fun MediaVideoSurface(
    player: ExoPlayer,
    format: VideoOrientationFormat,
    resizeMode: VideoResizeMode,
    isFullscreen: Boolean,
    modifier: Modifier = Modifier
) {
    val aspectMode = when (resizeMode) {
        VideoResizeMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        VideoResizeMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        VideoResizeMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                this.player = player
                this.resizeMode = aspectMode
                keepScreenOn = true
            }
        },
        update = { pv ->
            if (pv.player !== player) pv.player = player
            if (pv.resizeMode != aspectMode) pv.resizeMode = aspectMode
        },
        modifier = modifier
    )
}

fun formatMediaTime(millis: Long): String {
    val totalSec = (millis / 1000L).coerceAtLeast(0L)
    val hrs = totalSec / 3600L
    val mins = (totalSec % 3600L) / 60L
    val secs = totalSec % 60L
    return if (hrs > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}
