package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MediaDiscoveryRepository
import com.example.media.MediaDownloadManager
import com.example.media.PlayerPlaybackSnapshot
import com.example.media.RgenMediaEngine
import com.example.model.DownloadFormatOption
import com.example.model.DownloadTaskState
import com.example.model.EqualizerPreset
import com.example.model.EqualizerState
import com.example.model.LyricLine
import com.example.model.MediaSourceType
import com.example.model.MediaTabSection
import com.example.model.MediaTrackItem
import com.example.model.MixerState
import com.example.model.TelemetryPushCategory
import com.example.model.VideoOrientationFormat
import com.example.model.VideoResizeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class MediaViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    val engine = RgenMediaEngine(appContext)
    private val discoveryRepo = MediaDiscoveryRepository()
    private val downloadManager = MediaDownloadManager(appContext)

    var onPushLog: ((TelemetryPushCategory, String, String) -> Unit)? = null

    val playback: StateFlow<PlayerPlaybackSnapshot> = engine.playback
    val equalizerState: StateFlow<EqualizerState> = engine.equalizerState
    val mixerState: StateFlow<MixerState> = engine.mixerState
    val downloads: StateFlow<List<DownloadTaskState>> = downloadManager.downloadsFlow

    private val _activeSection = MutableStateFlow(MediaTabSection.PLAYER)
    val activeSection: StateFlow<MediaTabSection> = _activeSection.asStateFlow()

    private val _videoFormat = MutableStateFlow(VideoOrientationFormat.HORIZONTAL_16_9)
    val videoFormat: StateFlow<VideoOrientationFormat> = _videoFormat.asStateFlow()

    private val _videoResizeMode = MutableStateFlow(VideoResizeMode.FIT)
    val videoResizeMode: StateFlow<VideoResizeMode> = _videoResizeMode.asStateFlow()

    private val _isVideoFullscreen = MutableStateFlow(false)
    val isVideoFullscreen: StateFlow<Boolean> = _isVideoFullscreen.asStateFlow()

    private val _searchSource = MutableStateFlow(MediaSourceType.YOUTUBE_MUSIC)
    val searchSource: StateFlow<MediaSourceType> = _searchSource.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MediaTrackItem>>(emptyList())
    val searchResults: StateFlow<List<MediaTrackItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    private val _resolvingTrackId = MutableStateFlow<String?>(null)
    val resolvingTrackId: StateFlow<String?> = _resolvingTrackId.asStateFlow()

    private val _downloadTargetTrack = MutableStateFlow<MediaTrackItem?>(null)
    val downloadTargetTrack: StateFlow<MediaTrackItem?> = _downloadTargetTrack.asStateFlow()

    private val _availableDownloadFormats = MutableStateFlow<List<DownloadFormatOption>>(emptyList())
    val availableDownloadFormats: StateFlow<List<DownloadFormatOption>> = _availableDownloadFormats.asStateFlow()

    private val _isResolvingDownload = MutableStateFlow(false)
    val isResolvingDownload: StateFlow<Boolean> = _isResolvingDownload.asStateFlow()

    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()

    private val _customLyricsRaw = MutableStateFlow("")
    val customLyricsRaw: StateFlow<String> = _customLyricsRaw.asStateFlow()

    private val _isLoadingLyrics = MutableStateFlow(false)
    val isLoadingLyrics: StateFlow<Boolean> = _isLoadingLyrics.asStateFlow()

    private val _isForeground = MutableStateFlow(true)
    private val _isMediaTabVisible = MutableStateFlow(false)
    private val _batteryOpt = MutableStateFlow(true)

    init {
        // Poll ExoPlayer position strictly when playing and visible on the Media tab
        viewModelScope.launch {
            combine(
                playback,
                _isForeground,
                _isMediaTabVisible,
                _batteryOpt
            ) { snap, fg, visible, opt ->
                Triple(snap.isPlaying, fg && visible, opt)
            }.collectLatest { (playing, visible, opt) ->
                if (!playing || !visible) return@collectLatest
                while (isActive) {
                    engine.refreshPositionTick()
                    delay(if (opt) 320L else 180L)
                }
            }
        }
    }

    fun updateLifecycleAndVisibility(
        isForeground: Boolean,
        isMediaTabVisible: Boolean,
        batteryOptimized: Boolean
    ) {
        _isForeground.value = isForeground
        _isMediaTabVisible.value = isMediaTabVisible
        _batteryOpt.value = batteryOptimized
        if (isForeground && isMediaTabVisible) {
            engine.refreshPositionTick()
        }
    }

    fun selectSection(section: MediaTabSection) {
        _activeSection.value = section
    }

    fun setVideoFormat(format: VideoOrientationFormat) {
        _videoFormat.value = format
        onPushLog?.invoke(
            TelemetryPushCategory.MEDIA,
            "Formato de pantalla actualizado",
            "Relación de aspecto: ${format.label}"
        )
    }

    fun setVideoResizeMode(mode: VideoResizeMode) {
        _videoResizeMode.value = mode
        onPushLog?.invoke(
            TelemetryPushCategory.MEDIA,
            "Modo de ajuste de video",
            "Parámetro: ${mode.label}"
        )
    }

    fun setVideoFullscreen(fullscreen: Boolean) {
        _isVideoFullscreen.value = fullscreen
    }

    fun setSearchSource(source: MediaSourceType) {
        _searchSource.value = source
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun performSearch(overrideQuery: String? = null) {
        val q = (overrideQuery ?: _searchQuery.value).trim()
        if (q.isEmpty()) return
        _searchQuery.value = q
        val src = _searchSource.value

        viewModelScope.launch {
            _isSearching.value = true
            _searchError.value = null
            try {
                val results = discoveryRepo.searchMedia(src, q)
                _searchResults.value = results
                if (results.isEmpty()) {
                    _searchError.value = "Sin resultados en ${src.label} para \"$q\""
                } else {
                    onPushLog?.invoke(
                        TelemetryPushCategory.MEDIA,
                        "Búsqueda en ${src.label}",
                        "${results.size} coincidencias encontradas para \"$q\""
                    )
                }
            } catch (e: Exception) {
                _searchError.value = e.localizedMessage ?: "Error de conexión al buscar contenido"
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun loadLocalMediaUri(uri: Uri, targetDeckB: Boolean = false, openMusicTab: Boolean = false) {
        runCatching {
            appContext.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val mime = appContext.contentResolver.getType(uri) ?: "audio/mpeg"
        val displayName = resolveDisplayName(uri)
        val isVideo = mime.startsWith("video/", ignoreCase = true)
        val track = MediaTrackItem(
            id = "local_${UUID.randomUUID()}",
            title = displayName.substringBeforeLast('.'),
            artist = "Archivo local",
            sourceType = MediaSourceType.LOCAL,
            streamUrl = uri.toString(),
            isVideo = isVideo,
            mimeType = mime,
            localFilePath = uri.toString()
        )
        if (targetDeckB) {
            engine.loadDeckB(track, uri.toString())
            onPushLog?.invoke(TelemetryPushCategory.MUSIC, "Deck B Mixer cargado", track.title)
        } else {
            engine.loadAndPlayMain(track, uri.toString())
            _activeSection.value = if (isVideo && !openMusicTab) MediaTabSection.PLAYER else MediaTabSection.MUSIC
            fetchLyricsForTrack(track)
            onPushLog?.invoke(
                if (isVideo) TelemetryPushCategory.MEDIA else TelemetryPushCategory.MUSIC,
                if (isVideo) "Video local cargado" else "Audio local cargado",
                track.title
            )
        }
    }

    fun loadTrack(
        track: MediaTrackItem,
        openInMusicStudio: Boolean = !track.isVideo,
        forDeckB: Boolean = false
    ) {
        viewModelScope.launch {
            _resolvingTrackId.value = track.id
            try {
                val resolvedUrl = if (!track.localFilePath.isNullOrBlank()) {
                    track.localFilePath
                } else if (!track.streamUrl.isNullOrBlank() && track.videoId.isNullOrBlank()) {
                    track.streamUrl
                } else {
                    val (best, _) = discoveryRepo.resolveStreamAndDownloads(track)
                    best
                }

                if (resolvedUrl.isNullOrBlank()) {
                    onPushLog?.invoke(
                        TelemetryPushCategory.MEDIA,
                        "Flujo directo no disponible",
                        "Use \"Abrir en Web\" o pegue otro enlace compatible para ${track.title}"
                    )
                    return@launch
                }

                val enriched = track.copy(streamUrl = resolvedUrl)
                if (forDeckB) {
                    engine.loadDeckB(enriched, resolvedUrl)
                    _activeSection.value = MediaTabSection.MUSIC
                    onPushLog?.invoke(
                        TelemetryPushCategory.MUSIC,
                        "Deck B Mixer activo",
                        "${track.title} · ${track.artist}"
                    )
                } else {
                    engine.loadAndPlayMain(enriched, resolvedUrl)
                    _activeSection.value = if (openInMusicStudio) MediaTabSection.MUSIC else MediaTabSection.PLAYER
                    fetchLyricsForTrack(enriched)
                    onPushLog?.invoke(
                        if (openInMusicStudio) TelemetryPushCategory.MUSIC else TelemetryPushCategory.MEDIA,
                        "Reproducción iniciada",
                        "${track.title} · ${track.artist}"
                    )
                }
            } finally {
                _resolvingTrackId.value = null
            }
        }
    }

    fun playDownloadedItem(task: DownloadTaskState) {
        val path = task.localFilePath ?: return
        val isVideo = task.formatKind.isVideo
        val track = MediaTrackItem(
            id = "dl_${task.id}",
            title = task.title,
            artist = task.artist,
            sourceType = MediaSourceType.LOCAL,
            streamUrl = path,
            isVideo = isVideo,
            mimeType = if (isVideo) "video/${task.formatKind.ext}" else "audio/${task.formatKind.ext}",
            localFilePath = path
        )
        engine.loadAndPlayMain(track, path)
        _activeSection.value = if (isVideo) MediaTabSection.PLAYER else MediaTabSection.MUSIC
        fetchLyricsForTrack(track)
        onPushLog?.invoke(
            if (isVideo) TelemetryPushCategory.MEDIA else TelemetryPushCategory.MUSIC,
            "Archivo descargado abierto",
            "${task.title} (${task.formatKind.label})"
        )
    }

    fun requestDownloadOptions(track: MediaTrackItem) {
        _downloadTargetTrack.value = track
        _availableDownloadFormats.value = emptyList()
        _isResolvingDownload.value = true
        viewModelScope.launch {
            try {
                val (_, formats) = discoveryRepo.resolveStreamAndDownloads(track)
                _availableDownloadFormats.value = formats
                if (formats.isEmpty()) {
                    onPushLog?.invoke(
                        TelemetryPushCategory.DOWNLOAD,
                        "Sin flujos descargables",
                        "No se encontraron formatos directos para ${track.title}"
                    )
                }
            } finally {
                _isResolvingDownload.value = false
            }
        }
    }

    fun dismissDownloadDialog() {
        _downloadTargetTrack.value = null
        _availableDownloadFormats.value = emptyList()
        _isResolvingDownload.value = false
    }

    fun startDownload(track: MediaTrackItem, option: DownloadFormatOption) {
        dismissDownloadDialog()
        onPushLog?.invoke(
            TelemetryPushCategory.DOWNLOAD,
            "Descarga iniciada (${option.formatKind.ext.uppercase()})",
            "${track.title} · ${option.qualityLabel}"
        )
        viewModelScope.launch {
            downloadManager.downloadMedia(
                title = track.title,
                artist = track.artist,
                option = option,
                onCompleted = { done ->
                    onPushLog?.invoke(
                        TelemetryPushCategory.DOWNLOAD,
                        "Descarga completada",
                        "${done.title}.${done.formatKind.ext} guardada en biblioteca multimedia"
                    )
                },
                onFailed = { err ->
                    onPushLog?.invoke(
                        TelemetryPushCategory.DOWNLOAD,
                        "Error en descarga",
                        "${track.title}: $err"
                    )
                }
            )
        }
    }

    fun deleteDownload(taskId: String) {
        downloadManager.deleteDownload(taskId)
        onPushLog?.invoke(TelemetryPushCategory.DOWNLOAD, "Archivo eliminado", "Descarga removida del almacenamiento")
    }

    fun togglePlayPause() {
        val current = playback.value.currentTrack
        engine.toggleMainPlayPause()
        val nowPlaying = engine.mainPlayer.isPlaying
        onPushLog?.invoke(
            TelemetryPushCategory.MUSIC,
            if (nowPlaying) "Reproducción activa" else "Reproducción en pausa",
            current?.title ?: "Sin pista cargada"
        )
    }

    fun seekTo(positionMs: Long) {
        engine.seekMainTo(positionMs)
    }

    fun setLoopEnabled(enabled: Boolean) {
        engine.setLoopEnabled(enabled)
        onPushLog?.invoke(
            TelemetryPushCategory.MEDIA,
            if (enabled) "Repetición en bucle activada" else "Repetición desactivada",
            playback.value.currentTrack?.title ?: "Reproductor"
        )
    }

    // --- Lyrics ---
    fun fetchLyricsForTrack(track: MediaTrackItem? = playback.value.currentTrack) {
        val target = track ?: return
        viewModelScope.launch {
            _isLoadingLyrics.value = true
            try {
                val found = discoveryRepo.fetchLyrics(target.title, target.artist)
                _lyrics.value = found
                _customLyricsRaw.value = found.joinToString("\n") { it.text }
                if (found.isNotEmpty()) {
                    onPushLog?.invoke(
                        TelemetryPushCategory.MUSIC,
                        "Letra sincronizada cargada",
                        "${target.title} (${found.size} líneas)"
                    )
                }
            } finally {
                _isLoadingLyrics.value = false
            }
        }
    }

    fun updateManualLyrics(rawText: String) {
        _customLyricsRaw.value = rawText
        val lrcParsed = MediaDiscoveryRepository.parseLrcLyrics(rawText)
        _lyrics.value = if (lrcParsed.isNotEmpty()) {
            lrcParsed
        } else {
            rawText.lines()
                .filter { it.isNotBlank() }
                .mapIndexed { idx, line -> LyricLine(idx * 4_000L, line.trim()) }
        }
    }

    // --- Equalizer ---
    fun setEqualizerEnabled(enabled: Boolean) {
        engine.setEqualizerEnabled(enabled)
        onPushLog?.invoke(
            TelemetryPushCategory.MUSIC,
            if (enabled) "Ecualizador activado" else "Ecualizador en bypass",
            "Preset: ${equalizerState.value.preset.label}"
        )
    }

    fun applyEqualizerPreset(preset: EqualizerPreset) {
        engine.applyEqualizerPreset(preset)
        onPushLog?.invoke(TelemetryPushCategory.MUSIC, "Ecualizador: ${preset.label}", "Curva de frecuencias aplicada")
    }

    fun setEqualizerBand(index: Int, levelMb: Int) {
        engine.setEqualizerBand(index, levelMb)
    }

    fun setBassBoostStrength(strength: Int) {
        engine.setBassBoostStrength(strength)
    }

    fun setVirtualizerStrength(strength: Int) {
        engine.setVirtualizerStrength(strength)
    }

    fun setReverbPreset(code: Int) {
        engine.setReverbPreset(code)
    }

    // --- Dual-Deck Mixer ---
    fun setDeckAGain(gain: Float) {
        engine.updateMixer { it.copy(deckAGain = gain.coerceIn(0f, 1f)) }
    }

    fun setDeckBGain(gain: Float) {
        engine.updateMixer { it.copy(deckBGain = gain.coerceIn(0f, 1f)) }
    }

    fun setCrossfader(value: Float) {
        engine.updateMixer { it.copy(crossfader = value.coerceIn(-1f, 1f)) }
    }

    fun setMasterGain(gain: Float) {
        engine.updateMixer { it.copy(masterGain = gain.coerceIn(0f, 1f)) }
    }

    fun setPlaybackSpeed(speed: Float) {
        engine.updateMixer { it.copy(playbackSpeed = speed.coerceIn(0.5f, 2.0f)) }
    }

    fun setPlaybackPitch(pitch: Float) {
        engine.updateMixer { it.copy(playbackPitch = pitch.coerceIn(0.6f, 1.6f)) }
    }

    fun toggleDeckBPlayPause() {
        engine.toggleDeckBPlayPause()
        val active = engine.deckBPlayer.isPlaying
        onPushLog?.invoke(
            TelemetryPushCategory.MUSIC,
            if (active) "Mixer Deck B activo" else "Mixer Deck B en pausa",
            mixerState.value.deckBTrack?.title ?: "Canal B"
        )
    }

    private fun resolveDisplayName(uri: Uri): String {
        runCatching {
            appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) return cursor.getString(idx)
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "Pista multimedia"
    }

    override fun onCleared() {
        super.onCleared()
        engine.release()
    }
}
