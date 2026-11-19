package com.example.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.model.AlarmItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object AlarmPlaybackManager {

    private const val TAG = "AlarmPlaybackManager"

    private var mediaPlayer: MediaPlayer? = null
    private var previewPlayer: MediaPlayer? = null

    private val _ringingAlarm = MutableStateFlow<AlarmItem?>(null)
    val ringingAlarm: StateFlow<AlarmItem?> = _ringingAlarm.asStateFlow()

    private val _previewingAlarmId = MutableStateFlow<String?>(null)
    val previewingAlarmId: StateFlow<String?> = _previewingAlarmId.asStateFlow()

    fun startRinging(context: Context, alarm: AlarmItem) {
        stopPreview()
        stopAudioOnly()
        _ringingAlarm.value = alarm

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                val customFile = alarm.audioPath?.let { File(it) }?.takeIf { it.exists() }
                if (customFile != null) {
                    setDataSource(customFile.absolutePath)
                } else {
                    val fallbackUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    setDataSource(context.applicationContext, fallbackUri)
                }
                isLooping = true
                prepare()
                start()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Error playing alarm audio, trying default ringtone fallback", e)
            runCatching {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                val fallback = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(context.applicationContext, fallbackUri)
                    isLooping = true
                    prepare()
                    start()
                }
                mediaPlayer = fallback
            }
        }

        if (alarm.vibrate) {
            startVibration(context)
        }
    }

    fun stopRinging(context: Context) {
        stopAudioOnly()
        stopVibration(context)
        _ringingAlarm.value = null
    }

    fun togglePreview(context: Context, alarmId: String, audioPath: String?) {
        if (_previewingAlarmId.value == alarmId) {
            stopPreview()
            return
        }
        stopPreview()
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                val file = audioPath?.let { File(it) }?.takeIf { it.exists() }
                if (file != null) {
                    setDataSource(file.absolutePath)
                } else {
                    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    setDataSource(context.applicationContext, uri)
                }
                setOnCompletionListener {
                    stopPreview()
                }
                prepare()
                start()
            }
            previewPlayer = player
            _previewingAlarmId.value = alarmId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to preview audio", e)
            stopPreview()
        }
    }

    fun stopPreview() {
        runCatching {
            previewPlayer?.stop()
        }
        runCatching {
            previewPlayer?.release()
        }
        previewPlayer = null
        _previewingAlarmId.value = null
    }

    private fun stopAudioOnly() {
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
    }

    private fun startVibration(context: Context) {
        runCatching {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return
            val pattern = longArrayOf(0L, 450L, 350L, 450L)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, 0)
            }
        }
    }

    private fun stopVibration(context: Context) {
        runCatching { getVibrator(context)?.cancel() }
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
