package com.example.model

enum class MediaTabSection(val label: String) {
    PLAYER("Reproductor"),
    MUSIC("Música & EQ"),
    DISCOVER("YouTube / Google")
}

enum class VideoOrientationFormat(
    val id: String,
    val label: String,
    val aspectRatio: Float,
    val isLandscape: Boolean
) {
    HORIZONTAL_16_9("h_16_9", "16:9 Horizontal", 16f / 9f, true),
    CINEMA_21_9("h_21_9", "21:9 Cinema", 21f / 9f, true),
    VERTICAL_9_16("v_9_16", "9:16 Vertical", 9f / 16f, false),
    PORTRAIT_4_5("v_4_5", "4:5 Vertical", 4f / 5f, false),
    SQUARE_1_1("sq_1_1", "1:1 Cuadrado", 1f, false);

    companion object {
        fun fromId(id: String?): VideoOrientationFormat =
            entries.firstOrNull { it.id == id } ?: HORIZONTAL_16_9
    }
}

enum class VideoResizeMode(val id: String, val label: String) {
    FIT("fit", "Ajustar"),
    FILL("fill", "Estirar"),
    ZOOM("zoom", "Zoom");

    companion object {
        fun fromId(id: String?): VideoResizeMode =
            entries.firstOrNull { it.id == id } ?: FIT
    }
}

enum class MediaSourceType(val id: String, val label: String) {
    LOCAL("local", "Local"),
    YOUTUBE("youtube", "YouTube"),
    YOUTUBE_MUSIC("yt_music", "YouTube Music"),
    GOOGLE("google", "Google");

    companion object {
        fun fromId(id: String?): MediaSourceType =
            entries.firstOrNull { it.id == id } ?: LOCAL
    }
}

data class LyricLine(
    val timeMs: Long,
    val text: String
)

data class MediaTrackItem(
    val id: String,
    val title: String,
    val artist: String = "Desconocido",
    val album: String = "",
    val durationMs: Long = 0L,
    val sourceType: MediaSourceType = MediaSourceType.LOCAL,
    val streamUrl: String? = null,
    val pageUrl: String? = null,
    val videoId: String? = null,
    val thumbnailUrl: String? = null,
    val lyricsText: String = "",
    val isVideo: Boolean = false,
    val mimeType: String = "audio/mpeg",
    val localFilePath: String? = null
)

enum class EqualizerPreset(val id: String, val label: String) {
    FLAT("flat", "Plano"),
    CORPORATE_CLARITY("clarity", "Alta Definición"),
    BASS_SUB("bass", "Graves Sub"),
    NEON_CLUB("club", "Estudio Dinámico"),
    VOCAL_STUDIO("vocal", "Vocal / Podcast"),
    SPATIAL_3D("spatial", "Espacial 3D"),
    CUSTOM("custom", "Manual");

    companion object {
        fun fromId(id: String?): EqualizerPreset =
            entries.firstOrNull { it.id == id } ?: CORPORATE_CLARITY
    }
}

data class EqualizerState(
    val enabled: Boolean = true,
    val preset: EqualizerPreset = EqualizerPreset.CORPORATE_CLARITY,
    val bandLevelsMb: List<Int> = listOf(150, 50, 0, 150, 300),
    val bandFreqLabels: List<String> = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz"),
    val minBandLevelMb: Int = -1500,
    val maxBandLevelMb: Int = 1500,
    val bassBoostStrength: Int = 350, // 0..1000
    val virtualizerStrength: Int = 250, // 0..1000
    val reverbPreset: Int = 0 // 0=None, 1=SmallRoom, 2=MediumHall, 3=LargeHall
)

data class MixerState(
    val deckAGain: Float = 1.0f,
    val deckBGain: Float = 0.85f,
    val crossfader: Float = 0.0f, // -1.0 (Full A) .. 0.0 (Center) .. +1.0 (Full B)
    val masterGain: Float = 1.0f,
    val playbackSpeed: Float = 1.0f,
    val playbackPitch: Float = 1.0f,
    val balancePan: Float = 0.0f, // -1.0 Left .. 0.0 Center .. +1.0 Right
    val deckBTrack: MediaTrackItem? = null,
    val deckBPlaying: Boolean = false
)

enum class DownloadFormatKind(val id: String, val ext: String, val label: String, val isVideo: Boolean) {
    VIDEO_MP4("video_mp4", "mp4", "Video MP4", true),
    VIDEO_WEBM("video_webm", "webm", "Video WEBM", true),
    AUDIO_M4A("audio_m4a", "m4a", "Audio M4A", false),
    AUDIO_MP3("audio_mp3", "mp3", "Audio MP3", false),
    AUDIO_OPUS("audio_opus", "webm", "Audio OPUS", false)
}

data class DownloadFormatOption(
    val id: String,
    val url: String,
    val formatKind: DownloadFormatKind,
    val qualityLabel: String,
    val contentLengthBytes: Long = 0L
)

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

data class DownloadTaskState(
    val id: String,
    val title: String,
    val artist: String,
    val formatKind: DownloadFormatKind,
    val qualityLabel: String,
    val progress: Float = 0f,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val localFilePath: String? = null,
    val errorMessage: String? = null,
    val timestampMillis: Long = System.currentTimeMillis()
)
