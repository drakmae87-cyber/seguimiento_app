package com.example.media

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.model.DownloadFormatKind
import com.example.model.DownloadFormatOption
import com.example.model.DownloadStatus
import com.example.model.DownloadTaskState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

class MediaDownloadManager(private val context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val _downloads = MutableStateFlow(loadSavedDownloads())
    val downloadsFlow: StateFlow<List<DownloadTaskState>> = _downloads.asStateFlow()

    suspend fun downloadMedia(
        title: String,
        artist: String,
        option: DownloadFormatOption,
        onCompleted: (DownloadTaskState) -> Unit = {},
        onFailed: (String) -> Unit = {}
    ): DownloadTaskState = withContext(Dispatchers.IO) {
        val taskId = UUID.randomUUID().toString()
        val initial = DownloadTaskState(
            id = taskId,
            title = title,
            artist = artist,
            formatKind = option.formatKind,
            qualityLabel = option.qualityLabel,
            progress = 0f,
            status = DownloadStatus.DOWNLOADING
        )
        upsertTask(initial)

        val safeTitle = title.replace(Regex("[^a-zA-Z0-9._\\- ]"), "_").take(48).trim().ifBlank { "media_rgen" }
        val fileName = "${safeTitle}_${System.currentTimeMillis()}.${option.formatKind.ext}"
        val dir = File(context.filesDir, "media_downloads").apply { mkdirs() }
        val outFile = File(dir, fileName)

        try {
            val req = Request.Builder()
                .url(option.url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IllegalStateException("HTTP ${resp.code}")
                }
                val body = resp.body ?: throw IllegalStateException("Respuesta multimedia vacía")
                val totalBytes = body.contentLength().takeIf { it > 0L } ?: option.contentLengthBytes

                body.byteStream().use { input ->
                    outFile.outputStream().use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var copied = 0L
                        var lastReported = -1
                        var read = input.read(buffer)
                        while (read >= 0) {
                            output.write(buffer, 0, read)
                            copied += read
                            if (totalBytes > 0L) {
                                val pct = ((copied * 100L) / totalBytes).toInt().coerceIn(0, 99)
                                if (pct != lastReported && pct % 4 == 0) {
                                    lastReported = pct
                                    upsertTask(initial.copy(progress = pct / 100f, status = DownloadStatus.DOWNLOADING))
                                }
                            }
                            read = input.read(buffer)
                        }
                    }
                }
            }

            // Export copy to public Music/RGEN_time or Movies/RGEN_time on Android 10+ (zero broad storage permissions)
            exportToMediaStoreIfSupported(outFile, fileName, option.formatKind)

            val done = initial.copy(
                progress = 1f,
                status = DownloadStatus.COMPLETED,
                localFilePath = outFile.absolutePath
            )
            upsertTask(done)
            persistCompletedList()
            withContext(Dispatchers.Main) { onCompleted(done) }
            done
        } catch (e: Exception) {
            runCatching { if (outFile.exists()) outFile.delete() }
            val err = e.message ?: "Error al descargar archivo"
            val failed = initial.copy(
                progress = 0f,
                status = DownloadStatus.FAILED,
                errorMessage = err
            )
            upsertTask(failed)
            withContext(Dispatchers.Main) { onFailed(err) }
            failed
        }
    }

    fun deleteDownload(taskId: String) {
        val target = _downloads.value.firstOrNull { it.id == taskId }
        if (target?.localFilePath != null) {
            runCatching { File(target.localFilePath).takeIf { it.exists() }?.delete() }
        }
        _downloads.update { list -> list.filterNot { it.id == taskId } }
        persistCompletedList()
    }

    private fun exportToMediaStoreIfSupported(sourceFile: File, fileName: String, kind: DownloadFormatKind) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        runCatching {
            val resolver = context.contentResolver
            val mime = when (kind) {
                DownloadFormatKind.VIDEO_MP4 -> "video/mp4"
                DownloadFormatKind.VIDEO_WEBM -> "video/webm"
                DownloadFormatKind.AUDIO_MP3 -> "audio/mpeg"
                DownloadFormatKind.AUDIO_M4A -> "audio/mp4"
                DownloadFormatKind.AUDIO_OPUS -> "audio/webm"
            }
            val collection = if (kind.isVideo) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
            val relDir = if (kind.isVideo) {
                "${Environment.DIRECTORY_MOVIES}/RGEN_time"
            } else {
                "${Environment.DIRECTORY_MUSIC}/RGEN_time"
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relDir)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(collection, values) ?: return
            resolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { inp -> inp.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
    }

    private fun upsertTask(task: DownloadTaskState) {
        _downloads.update { current ->
            val idx = current.indexOfFirst { it.id == task.id }
            if (idx >= 0) {
                current.toMutableList().apply { this[idx] = task }
            } else {
                listOf(task) + current
            }
        }
    }

    private fun persistCompletedList() {
        val completed = _downloads.value.filter { it.status == DownloadStatus.COMPLETED && !it.localFilePath.isNullOrBlank() }
        val arr = JSONArray()
        completed.forEach { d ->
            arr.put(
                JSONObject()
                    .put("id", d.id)
                    .put("title", d.title)
                    .put("artist", d.artist)
                    .put("formatKind", d.formatKind.id)
                    .put("qualityLabel", d.qualityLabel)
                    .put("localFilePath", d.localFilePath)
                    .put("timestampMillis", d.timestampMillis)
            )
        }
        prefs.edit().putString(KEY_DOWNLOADS, arr.toString()).apply()
    }

    private fun loadSavedDownloads(): List<DownloadTaskState> {
        val raw = prefs.getString(KEY_DOWNLOADS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val path = obj.optString("localFilePath")
                    if (path.isBlank() || !File(path).exists()) continue
                    val kind = DownloadFormatKind.entries.firstOrNull {
                        it.id == obj.optString("formatKind")
                    } ?: DownloadFormatKind.AUDIO_M4A
                    add(
                        DownloadTaskState(
                            id = obj.getString("id"),
                            title = obj.optString("title", "Archivo"),
                            artist = obj.optString("artist", "RGEN_time"),
                            formatKind = kind,
                            qualityLabel = obj.optString("qualityLabel", kind.label),
                            progress = 1f,
                            status = DownloadStatus.COMPLETED,
                            localFilePath = path,
                            timestampMillis = obj.optLong("timestampMillis", 0L)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val PREFS_NAME = "rgen_media_downloads"
        const val KEY_DOWNLOADS = "completed_downloads_v1"
    }
}
