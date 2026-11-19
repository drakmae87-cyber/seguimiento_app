package com.example.media

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.model.EqualizerPreset
import com.example.model.EqualizerState
import com.example.model.MediaTrackItem
import com.example.model.MixerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import kotlin.math.roundToInt

private const val ENGINE_TAG = "RgenMediaEngine"

data class PlayerPlaybackSnapshot(
    val currentTrack: MediaTrackItem? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val loopEnabled: Boolean = false,
    val errorMessage: String? = null
)

class RgenMediaEngine(context: Context) {

    private val appContext = context.applicationContext

    val mainPlayer: ExoPlayer = ExoPlayer.Builder(appContext)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    val deckBPlayer: ExoPlayer = ExoPlayer.Builder(appContext)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            false
        )
        .build()

    private val _playback = MutableStateFlow(PlayerPlaybackSnapshot())
    val playback: StateFlow<PlayerPlaybackSnapshot> = _playback.asStateFlow()

    private val _equalizerState = MutableStateFlow(EqualizerState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    private val _mixerState = MutableStateFlow(MixerState())
    val mixerState: StateFlow<MixerState> = _mixerState.asStateFlow()

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null
    private var attachedAudioSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    init {
        mainPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playback.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val buffering = playbackState == Player.STATE_BUFFERING
                val dur = mainPlayer.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: _playback.value.durationMs
                _playback.update { it.copy(isBuffering = buffering, durationMs = dur) }
                if (playbackState == Player.STATE_READY) {
                    attachAudioEffectsIfNeeded(mainPlayer.audioSessionId)
                }
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                attachAudioEffectsIfNeeded(audioSessionId)
            }

            override fun onPlayerError(error: PlaybackException) {
                _playback.update {
                    it.copy(
                        isPlaying = false,
                        isBuffering = false,
                        errorMessage = error.localizedMessage ?: "Error al reproducir el recurso multimedia"
                    )
                }
            }
        })

        deckBPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _mixerState.update { it.copy(deckBPlaying = isPlaying) }
            }
        })
    }

    fun loadAndPlayMain(track: MediaTrackItem, playableSource: String) {
        val uri = toMediaUri(playableSource)
        _playback.update {
            it.copy(
                currentTrack = track,
                isBuffering = true,
                positionMs = 0L,
                durationMs = track.durationMs,
                errorMessage = null
            )
        }
        mainPlayer.setMediaItem(MediaItem.fromUri(uri))
        mainPlayer.prepare()
        applyMixerToPlayers()
        mainPlayer.play()
        attachAudioEffectsIfNeeded(mainPlayer.audioSessionId)
    }

    fun toggleMainPlayPause() {
        if (mainPlayer.isPlaying) {
            mainPlayer.pause()
        } else {
            if (mainPlayer.playbackState == Player.STATE_ENDED) {
                mainPlayer.seekTo(0L)
            }
            mainPlayer.play()
        }
    }

    fun seekMainTo(positionMs: Long) {
        val clamped = positionMs.coerceAtLeast(0L)
        mainPlayer.seekTo(clamped)
        _playback.update { it.copy(positionMs = clamped) }
    }

    fun refreshPositionTick() {
        val pos = mainPlayer.currentPosition.coerceAtLeast(0L)
        val dur = mainPlayer.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: _playback.value.durationMs
        _playback.update { it.copy(positionMs = pos, durationMs = dur) }
    }

    fun setLoopEnabled(enabled: Boolean) {
        mainPlayer.repeatMode = if (enabled) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        _playback.update { it.copy(loopEnabled = enabled) }
    }

    fun loadDeckB(track: MediaTrackItem, playableSource: String) {
        val uri = toMediaUri(playableSource)
        deckBPlayer.setMediaItem(MediaItem.fromUri(uri))
        deckBPlayer.repeatMode = Player.REPEAT_MODE_ONE
        deckBPlayer.prepare()
        _mixerState.update { it.copy(deckBTrack = track) }
        applyMixerToPlayers()
        deckBPlayer.play()
    }

    fun toggleDeckBPlayPause() {
        if (deckBPlayer.isPlaying) deckBPlayer.pause() else deckBPlayer.play()
    }

    fun updateMixer(transform: (MixerState) -> MixerState) {
        _mixerState.update(transform)
        applyMixerToPlayers()
    }

    private fun applyMixerToPlayers() {
        val m = _mixerState.value
        val cf = m.crossfader.coerceIn(-1f, 1f)
        // Smooth constant-power style crossfade weights
        val weightA = ((1f - cf) * 0.5f * 2f).coerceIn(0f, 1f)
        val weightB = ((1f + cf) * 0.5f * 2f).coerceIn(0f, 1f)
        mainPlayer.volume = (m.deckAGain * weightA * m.masterGain).coerceIn(0f, 1f)
        deckBPlayer.volume = (m.deckBGain * weightB * m.masterGain).coerceIn(0f, 1f)

        val params = PlaybackParameters(
            m.playbackSpeed.coerceIn(0.5f, 2.0f),
            m.playbackPitch.coerceIn(0.6f, 1.6f)
        )
        mainPlayer.playbackParameters = params
        deckBPlayer.playbackParameters = params
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        _equalizerState.update { it.copy(enabled = enabled) }
        runCatching {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled && _equalizerState.value.bassBoostStrength > 0
            virtualizer?.enabled = enabled && _equalizerState.value.virtualizerStrength > 0
            presetReverb?.enabled = enabled && _equalizerState.value.reverbPreset > 0
        }
    }

    fun applyEqualizerPreset(preset: EqualizerPreset) {
        val cur = _equalizerState.value
        val count = cur.bandLevelsMb.size.coerceAtLeast(5)
        val min = cur.minBandLevelMb
        val max = cur.maxBandLevelMb
        val rawCurve = when (preset) {
            EqualizerPreset.FLAT -> listOf(0, 0, 0, 0, 0)
            EqualizerPreset.CORPORATE_CLARITY -> listOf(150, 50, 100, 350, 450)
            EqualizerPreset.BASS_SUB -> listOf(900, 550, 50, -100, 150)
            EqualizerPreset.NEON_CLUB -> listOf(750, 300, -150, 450, 700)
            EqualizerPreset.VOCAL_STUDIO -> listOf(-300, 100, 550, 450, 100)
            EqualizerPreset.SPATIAL_3D -> listOf(400, -100, 200, 500, 800)
            EqualizerPreset.CUSTOM -> cur.bandLevelsMb
        }
        val bands = List(count) { idx ->
            rawCurve.getOrElse(idx) { 0 }.coerceIn(min, max)
        }
        _equalizerState.update { it.copy(preset = preset, bandLevelsMb = bands) }
        syncAllHardwareBands()
    }

    fun setEqualizerBand(bandIndex: Int, levelMb: Int) {
        val cur = _equalizerState.value
        if (bandIndex !in cur.bandLevelsMb.indices) return
        val clamped = levelMb.coerceIn(cur.minBandLevelMb, cur.maxBandLevelMb)
        val updated = cur.bandLevelsMb.toMutableList().apply { this[bandIndex] = clamped }
        _equalizerState.update { it.copy(preset = EqualizerPreset.CUSTOM, bandLevelsMb = updated) }
        runCatching { equalizer?.setBandLevel(bandIndex.toShort(), clamped.toShort()) }
    }

    fun setBassBoostStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _equalizerState.update { it.copy(bassBoostStrength = clamped) }
        runCatching {
            bassBoost?.enabled = _equalizerState.value.enabled && clamped > 0
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(clamped.toShort())
            }
        }
    }

    fun setVirtualizerStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _equalizerState.update { it.copy(virtualizerStrength = clamped) }
        runCatching {
            virtualizer?.enabled = _equalizerState.value.enabled && clamped > 0
            if (virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength(clamped.toShort())
            }
        }
    }

    fun setReverbPreset(presetCode: Int) {
        val clamped = presetCode.coerceIn(0, 3)
        _equalizerState.update { it.copy(reverbPreset = clamped) }
        runCatching {
            val androidPreset = when (clamped) {
                1 -> PresetReverb.PRESET_SMALLROOM
                2 -> PresetReverb.PRESET_MEDIUMHALL
                3 -> PresetReverb.PRESET_LARGEHALL
                else -> PresetReverb.PRESET_NONE
            }
            presetReverb?.preset = androidPreset
            presetReverb?.enabled = _equalizerState.value.enabled && clamped > 0
        }
    }

    private fun attachAudioEffectsIfNeeded(sessionId: Int) {
        if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId <= 0 || sessionId == attachedAudioSessionId) {
            return
        }
        releaseEffects()
        attachedAudioSessionId = sessionId

        runCatching {
            val eq = Equalizer(0, sessionId)
            val bandsCount = eq.numberOfBands.toInt().coerceIn(1, 10)
            val range = eq.bandLevelRange
            val minMb = range.getOrNull(0)?.toInt() ?: -1500
            val maxMb = range.getOrNull(1)?.toInt() ?: 1500
            val freqLabels = (0 until bandsCount).map { idx ->
                val hz = eq.getCenterFreq(idx.toShort()) / 1000
                if (hz >= 1000) String.format("%.1fk", hz / 1000f) else "${hz}Hz"
            }
            val currentLevels = _equalizerState.value.bandLevelsMb
            val normalized = List(bandsCount) { idx ->
                currentLevels.getOrElse(idx) { 0 }.coerceIn(minMb, maxMb)
            }
            eq.enabled = _equalizerState.value.enabled
            normalized.forEachIndexed { idx, lvl ->
                eq.setBandLevel(idx.toShort(), lvl.toShort())
            }
            equalizer = eq
            _equalizerState.update {
                it.copy(
                    bandFreqLabels = freqLabels,
                    bandLevelsMb = normalized,
                    minBandLevelMb = minMb,
                    maxBandLevelMb = maxMb
                )
            }
        }.onFailure { Log.w(ENGINE_TAG, "Hardware Equalizer unavailable on this session: ${it.message}") }

        runCatching {
            bassBoost = BassBoost(0, sessionId).apply {
                val strength = _equalizerState.value.bassBoostStrength
                enabled = _equalizerState.value.enabled && strength > 0
                if (strengthSupported) setStrength(strength.toShort())
            }
        }

        runCatching {
            virtualizer = Virtualizer(0, sessionId).apply {
                val strength = _equalizerState.value.virtualizerStrength
                enabled = _equalizerState.value.enabled && strength > 0
                if (strengthSupported) setStrength(strength.toShort())
            }
        }

        runCatching {
            presetReverb = PresetReverb(0, sessionId).apply {
                val code = _equalizerState.value.reverbPreset
                enabled = _equalizerState.value.enabled && code > 0
                if (code > 0) preset = code.toShort()
            }
        }
    }

    private fun syncAllHardwareBands() {
        val state = _equalizerState.value
        runCatching {
            val eq = equalizer ?: return
            val count = minOf(eq.numberOfBands.toInt(), state.bandLevelsMb.size)
            for (i in 0 until count) {
                eq.setBandLevel(i.toShort(), state.bandLevelsMb[i].toShort())
            }
        }
    }

    private fun toMediaUri(source: String): Uri {
        return if (source.startsWith("http://", true) ||
            source.startsWith("https://", true) ||
            source.startsWith("content://", true) ||
            source.startsWith("file://", true)
        ) {
            Uri.parse(source)
        } else {
            Uri.fromFile(File(source))
        }
    }

    private fun releaseEffects() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { presetReverb?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        presetReverb = null
    }

    fun release() {
        releaseEffects()
        mainPlayer.release()
        deckBPlayer.release()
    }

    companion object {
        fun formatMbAsDb(mb: Int): String {
            val db = (mb / 100f).roundToInt()
            return if (db > 0) "+${db}dB" else "${db}dB"
        }
    }
}
