package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.media.PlayerPlaybackSnapshot
import com.example.media.RgenMediaEngine
import com.example.model.EqualizerPreset
import com.example.model.EqualizerState
import com.example.model.LyricLine
import com.example.model.MixerState
import com.example.ui.components.ChamferedHudShape
import com.example.ui.theme.InkCream
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalNekoPalette
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.StageBorder
import com.example.ui.theme.StageCard
import com.example.ui.theme.StageCardElevated
import com.example.ui.theme.StageVoid
import com.example.viewmodel.MediaViewModel
import java.util.Locale

@Composable
fun MusicStudioSection(
    mediaViewModel: MediaViewModel,
    playback: PlayerPlaybackSnapshot,
    eqState: EqualizerState,
    mixerState: MixerState,
    lyrics: List<LyricLine>,
    customLyricsRaw: String,
    isLoadingLyrics: Boolean,
    batteryOptimized: Boolean
) {
    val palette = LocalNekoPalette.current
    val context = LocalContext.current
    val hudShape = remember { ChamferedHudShape(14.dp) }
    val chipShape = remember { ChamferedHudShape(8.dp) }

    var showEditLyricsDialog by remember { mutableStateOf(false) }

    val mainAudioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) mediaViewModel.loadLocalMediaUri(uri, targetDeckB = false, openMusicTab = true)
    }

    val deckBPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) mediaViewModel.loadLocalMediaUri(uri, targetDeckB = true, openMusicTab = true)
    }

    // Smooth optical disc spin ONLY when actively playing (0 animation overhead when paused!)
    val discAngle = if (playback.isPlaying) {
        val transition = rememberInfiniteTransition(label = "discSpin")
        val anim by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(if (batteryOptimized) 7500 else 5500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "angle"
        )
        anim
    } else {
        0f
    }

    val activeLyricIndex = remember(lyrics, playback.positionMs) {
        lyrics.indexOfLast { it.timeMs <= playback.positionMs }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. VERTICAL MUSIC DECK: Portada + Disco Giratorio + Transporte
        Card(
            shape = hudShape,
            colors = CardDefaults.cardColors(containerColor = StageCard.copy(alpha = 0.88f)),
            border = BorderStroke(1.2.dp, palette.primaryGlow.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ESTUDIO MUSICAL VERTICAL",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = palette.secondaryGlow
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Vertical Stage: Portada behind/side + Rotating Optical Vinyl Disc
                Box(
                    modifier = Modifier.size(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Rotating Outer Vinyl / Optical Disc
                    Box(
                        modifier = Modifier
                            .size(214.dp)
                            .rotate(discAngle)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFF0A1220),
                                        Color(0xFF050810),
                                        Color(0xFF101D30),
                                        Color(0xFF04070D)
                                    )
                                )
                            )
                            .border(2.dp, palette.primaryGlow.copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val r = size.minDimension / 2f
                            for (ring in 1..6) {
                                drawCircle(
                                    color = palette.primaryGlow.copy(alpha = 0.14f),
                                    radius = r * (0.45f + ring * 0.08f),
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }
                            drawLine(
                                color = palette.secondaryGlow.copy(alpha = 0.6f),
                                start = Offset(center.x, center.y - r * 0.88f),
                                end = Offset(center.x, center.y - r * 0.52f),
                                strokeWidth = 2.5.dp.toPx()
                            )
                        }
                    }

                    // Cover Art ("Portada") inside rotating disc hub
                    Box(
                        modifier = Modifier
                            .size(126.dp)
                            .rotate(discAngle)
                            .clip(CircleShape)
                            .background(StageVoid)
                            .border(2.dp, palette.secondaryGlow, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val thumb = playback.currentTrack?.thumbnailUrl
                        if (!thumb.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(thumb).crossfade(true).build(),
                                contentDescription = "Portada del álbum",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = palette.primaryGlow,
                                modifier = Modifier.size(46.dp)
                            )
                        }
                        // Center spindle hole
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(StageVoid)
                                .border(1.5.dp, palette.primaryGlow, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = playback.currentTrack?.title ?: "Sin pista seleccionada",
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = InkCream,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = playback.currentTrack?.artist ?: "Cargue audio local o busque en YouTube Music / Google",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondaryGlow,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                val dur = playback.durationMs.coerceAtLeast(1L)
                val pos = playback.positionMs.coerceIn(0L, dur)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatMediaTime(pos), fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = palette.primaryGlow)
                    Text(formatMediaTime(playback.durationMs), fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = InkMuted)
                }
                Slider(
                    value = if (playback.durationMs > 0L) pos.toFloat() / dur.toFloat() else 0f,
                    onValueChange = { frac ->
                        if (playback.durationMs > 0L) mediaViewModel.seekTo((frac * dur).toLong())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = palette.primaryGlow,
                        activeTrackColor = palette.primaryGlow
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { mediaViewModel.togglePlayPause() },
                        shape = chipShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primaryGlow,
                            contentColor = StageVoid
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("music_play_pause_btn")
                    ) {
                        Icon(
                            imageVector = if (playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (playback.isPlaying) "Pausar" else "Reproducir")
                    }

                    OutlinedButton(
                        onClick = { mainAudioPicker.launch(arrayOf("audio/*", "video/*")) },
                        shape = chipShape,
                        border = BorderStroke(1.dp, palette.secondaryGlow),
                        modifier = Modifier.testTag("music_pick_local_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FolderOpen,
                            contentDescription = null,
                            tint = palette.secondaryGlow,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cargar MP3/Audio", fontSize = 12.sp, color = palette.secondaryGlow)
                    }
                }
            }
        }

        // 2. LYRICS ("Letra") PANEL
        Card(
            shape = hudShape,
            colors = CardDefaults.cardColors(containerColor = StageCard.copy(alpha = 0.84f)),
            border = BorderStroke(1.dp, StageBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Lyrics, contentDescription = null, tint = palette.secondaryGlow, modifier = Modifier.size(18.dp))
                        Text("Letra Sincronizada", style = MaterialTheme.typography.titleMedium, color = palette.secondaryGlow)
                    }
                    Row {
                        IconButton(onClick = { mediaViewModel.fetchLyricsForTrack() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Buscar letra", tint = palette.primaryGlow)
                        }
                        IconButton(onClick = { showEditLyricsDialog = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar letra", tint = InkCream)
                        }
                    }
                }

                if (isLoadingLyrics) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = palette.primaryGlow, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Consultando base de letras LrcLib…", fontSize = 12.sp, color = InkMuted)
                    }
                } else if (lyrics.isEmpty()) {
                    Text(
                        text = "Sin letra cargada. Pulse Actualizar para buscar automáticamente o Editar para ingresar texto/LRC.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        lyrics.forEachIndexed { idx, line ->
                            val isActive = idx == activeLyricIndex
                            Text(
                                text = line.text,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isActive) palette.primaryGlow else InkCream.copy(alpha = 0.72f)
                            )
                        }
                    }
                }
            }
        }

        // 3. COMPLETE HARDWARE EQUALIZER ("Ecualizador Completo")
        Card(
            shape = hudShape,
            colors = CardDefaults.cardColors(containerColor = StageCard.copy(alpha = 0.86f)),
            border = BorderStroke(1.dp, palette.primaryGlow.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Equalizer, contentDescription = null, tint = palette.primaryGlow, modifier = Modifier.size(20.dp))
                        Text("Ecualizador Completo", style = MaterialTheme.typography.titleMedium, color = palette.primaryGlow)
                    }
                    Switch(
                        checked = eqState.enabled,
                        onCheckedChange = mediaViewModel::setEqualizerEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = StageVoid,
                            checkedTrackColor = palette.primaryGlow
                        ),
                        modifier = Modifier.testTag("eq_master_switch")
                    )
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(EqualizerPreset.entries.filter { it != EqualizerPreset.CUSTOM }) { preset ->
                        FilterChip(
                            selected = eqState.preset == preset,
                            onClick = { mediaViewModel.applyEqualizerPreset(preset) },
                            shape = chipShape,
                            label = { Text(preset.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.primaryGlow,
                                selectedLabelColor = StageVoid,
                                containerColor = StageVoid,
                                labelColor = InkCream
                            ),
                            modifier = Modifier.testTag("eq_preset_${preset.id}")
                        )
                    }
                }

                // Multi-band EQ sliders with dB readouts
                eqState.bandLevelsMb.forEachIndexed { idx, mbLevel ->
                    val label = eqState.bandFreqLabels.getOrElse(idx) { "B${idx + 1}" }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = InkMuted,
                            modifier = Modifier.width(56.dp)
                        )
                        Slider(
                            value = mbLevel.toFloat(),
                            onValueChange = { mediaViewModel.setEqualizerBand(idx, it.toInt()) },
                            valueRange = eqState.minBandLevelMb.toFloat()..eqState.maxBandLevelMb.toFloat(),
                            enabled = eqState.enabled,
                            colors = SliderDefaults.colors(
                                thumbColor = palette.secondaryGlow,
                                activeTrackColor = palette.secondaryGlow
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = RgenMediaEngine.formatMbAsDb(mbLevel),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = palette.secondaryGlow,
                            modifier = Modifier.width(46.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }

                // Bass Boost + Virtualizer + Reverb
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bass Boost · ${eqState.bassBoostStrength / 10}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkMuted
                        )
                        Slider(
                            value = eqState.bassBoostStrength.toFloat(),
                            onValueChange = { mediaViewModel.setBassBoostStrength(it.toInt()) },
                            valueRange = 0f..1000f,
                            enabled = eqState.enabled,
                            colors = SliderDefaults.colors(thumbColor = palette.primaryGlow, activeTrackColor = palette.primaryGlow)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Espacial 3D · ${eqState.virtualizerStrength / 10}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkMuted
                        )
                        Slider(
                            value = eqState.virtualizerStrength.toFloat(),
                            onValueChange = { mediaViewModel.setVirtualizerStrength(it.toInt()) },
                            valueRange = 0f..1000f,
                            enabled = eqState.enabled,
                            colors = SliderDefaults.colors(thumbColor = palette.secondaryGlow, activeTrackColor = palette.secondaryGlow)
                        )
                    }
                }

                Text("Reverberación acústica", style = MaterialTheme.typography.labelSmall, color = InkMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Ninguna", "Estudio", "Sala Media", "Auditorio").forEachIndexed { code, label ->
                        FilterChip(
                            selected = eqState.reverbPreset == code,
                            onClick = { mediaViewModel.setReverbPreset(code) },
                            shape = chipShape,
                            label = { Text(label, fontSize = 10.sp) }
                        )
                    }
                }
            }
        }

        // 4. DUAL-DECK MIXER ("Mixer")
        Card(
            shape = hudShape,
            colors = CardDefaults.cardColors(containerColor = StageCard.copy(alpha = 0.86f)),
            border = BorderStroke(1.dp, palette.secondaryGlow.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Tune, contentDescription = null, tint = palette.secondaryGlow, modifier = Modifier.size(18.dp))
                        Text("Mixer Dual-Deck A/B", style = MaterialTheme.typography.titleMedium, color = palette.secondaryGlow)
                    }
                    OutlinedButton(
                        onClick = { deckBPicker.launch(arrayOf("audio/*", "video/*")) },
                        shape = chipShape,
                        border = BorderStroke(1.dp, palette.primaryGlow),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("mixer_load_deck_b_btn")
                    ) {
                        Icon(Icons.Filled.Album, contentDescription = null, tint = palette.primaryGlow, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cargar Deck B", fontSize = 11.sp, color = palette.primaryGlow)
                    }
                }

                if (mixerState.deckBTrack != null) {
                    Surface(
                        shape = chipShape,
                        color = StageVoid.copy(alpha = 0.72f),
                        border = BorderStroke(0.8.dp, palette.secondaryGlow.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("DECK B", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = palette.secondaryGlow)
                                Text(mixerState.deckBTrack.title, style = MaterialTheme.typography.bodySmall, color = InkCream, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = mediaViewModel::toggleDeckBPlayPause) {
                                Icon(
                                    imageVector = if (mixerState.deckBPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Play/Pause Deck B",
                                    tint = palette.secondaryGlow
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Crossfader A ↔ B · ${
                        when {
                            mixerState.crossfader < -0.15f -> "Foco A"
                            mixerState.crossfader > 0.15f -> "Foco B"
                            else -> "Centro 50/50"
                        }
                    }",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkCream
                )
                Slider(
                    value = mixerState.crossfader,
                    onValueChange = mediaViewModel::setCrossfader,
                    valueRange = -1f..1f,
                    colors = SliderDefaults.colors(thumbColor = palette.primaryGlow, activeTrackColor = palette.secondaryGlow),
                    modifier = Modifier.testTag("mixer_crossfader")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ganancia A · ${(mixerState.deckAGain * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = InkMuted)
                        Slider(value = mixerState.deckAGain, onValueChange = mediaViewModel::setDeckAGain)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ganancia B · ${(mixerState.deckBGain * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = InkMuted)
                        Slider(value = mixerState.deckBGain, onValueChange = mediaViewModel::setDeckBGain)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Master · ${(mixerState.masterGain * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = InkMuted)
                        Slider(value = mixerState.masterGain, onValueChange = mediaViewModel::setMasterGain)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tempo · ${String.format(Locale.US, "%.2fx", mixerState.playbackSpeed)}", style = MaterialTheme.typography.labelSmall, color = InkMuted)
                        Slider(value = mixerState.playbackSpeed, onValueChange = mediaViewModel::setPlaybackSpeed, valueRange = 0.5f..2.0f)
                    }
                }

                Text("Tono (Pitch) · ${String.format(Locale.US, "%.2fx", mixerState.playbackPitch)}", style = MaterialTheme.typography.labelSmall, color = InkMuted)
                Slider(value = mixerState.playbackPitch, onValueChange = mediaViewModel::setPlaybackPitch, valueRange = 0.6f..1.6f)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }

    if (showEditLyricsDialog) {
        var draft by remember(customLyricsRaw) { mutableStateOf(customLyricsRaw) }
        AlertDialog(
            onDismissRequest = { showEditLyricsDialog = false },
            containerColor = StageCardElevated,
            titleContentColor = InkCream,
            textContentColor = InkCream,
            title = { Text("EDITAR LETRA / LRC", style = MaterialTheme.typography.titleMedium) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("[00:12.00] Línea sincronizada o texto plano…") },
                    minLines = 6,
                    maxLines = 10,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.primaryGlow,
                        unfocusedBorderColor = StageBorder,
                        focusedTextColor = InkCream,
                        unfocusedTextColor = InkCream
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        mediaViewModel.updateManualLyrics(draft)
                        showEditLyricsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primaryGlow, contentColor = StageVoid)
                ) {
                    Text("Aplicar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditLyricsDialog = false }) {
                    Text("Cancelar", color = InkMuted)
                }
            }
        )
    }
}
