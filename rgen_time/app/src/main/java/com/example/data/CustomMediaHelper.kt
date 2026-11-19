package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

enum class MediaFormatKind {
    RASTER_IMAGE,
    ANIMATED_GIF,
    VECTOR_SVG,
    LOTTIE_JSON,
    DOT_LOTTIE,
    RIVE_ANIM;

    companion object {
        fun fromPath(path: String?): MediaFormatKind {
            val ext = path?.substringAfterLast('.', "")?.lowercase(Locale.US).orEmpty()
            return when (ext) {
                "gif" -> ANIMATED_GIF
                "svg" -> VECTOR_SVG
                "json" -> LOTTIE_JSON
                "lottie" -> DOT_LOTTIE
                "riv" -> RIVE_ANIM
                else -> RASTER_IMAGE
            }
        }
    }
}

object CustomMediaHelper {

    fun createMediaImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context.applicationContext)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(SvgDecoder.Factory())
            }
            .build()
    }

    fun resolveDisplayName(context: Context, uri: Uri): String {
        var name: String? = null
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) name = cursor.getString(idx)
                }
            }
        }
        return name?.takeIf { it.isNotBlank() } ?: (uri.lastPathSegment?.substringAfterLast('/') ?: "archivo")
    }

    private fun resolveMediaExtension(context: Context, uri: Uri, displayName: String): String {
        val nameExt = displayName.substringAfterLast('.', "").lowercase(Locale.US)
        if (nameExt in setOf("png", "jpg", "jpeg", "webp", "gif", "svg", "json", "lottie", "riv")) {
            return if (nameExt == "jpeg") "jpg" else nameExt
        }
        val mimeType = context.contentResolver.getType(uri).orEmpty().lowercase(Locale.US)
        return when {
            mimeType.contains("svg") -> "svg"
            mimeType.contains("gif") -> "gif"
            mimeType.contains("webp") -> "webp"
            mimeType.contains("png") -> "png"
            mimeType.contains("json") -> "json"
            mimeType.contains("zip") || mimeType.contains("lottie") -> "lottie"
            mimeType.contains("rive") -> "riv"
            else -> "jpg"
        }
    }

    private fun resolveAudioExtension(context: Context, uri: Uri, displayName: String): String {
        val nameExt = displayName.substringAfterLast('.', "").lowercase(Locale.US)
        if (nameExt in setOf("mp3", "wav", "ogg", "m4a", "aac", "flac", "opus", "mid", "3gp")) {
            return nameExt
        }
        val mimeType = context.contentResolver.getType(uri).orEmpty().lowercase(Locale.US)
        return when {
            mimeType.contains("mpeg") || mimeType.contains("mp3") -> "mp3"
            mimeType.contains("wav") -> "wav"
            mimeType.contains("ogg") -> "ogg"
            mimeType.contains("flac") -> "flac"
            mimeType.contains("aac") -> "aac"
            mimeType.contains("mp4") || mimeType.contains("m4a") -> "m4a"
            else -> "mp3"
        }
    }

    suspend fun importUriToInternalStorage(
        context: Context,
        uri: Uri,
        slotName: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val displayName = resolveDisplayName(context, uri)
            val ext = resolveMediaExtension(context, uri, displayName)
            val dir = File(context.filesDir, "custom_hud_media").apply { mkdirs() }
            dir.listFiles()?.filter { it.name.startsWith("${slotName}_") }?.forEach { it.delete() }

            val target = File(dir, "${slotName}_${System.currentTimeMillis()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            target.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveGeneratedLottieJson(
        context: Context,
        lottieJson: String,
        slotName: String = "eye"
    ): String? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "custom_hud_media").apply { mkdirs() }
            dir.listFiles()?.filter { it.name.startsWith("${slotName}_") }?.forEach { it.delete() }
            val target = File(dir, "${slotName}_ai_${System.currentTimeMillis()}.json")
            target.writeText(lottieJson, Charsets.UTF_8)
            target.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    suspend fun importAudioToInternalStorage(
        context: Context,
        uri: Uri,
        alarmId: String
    ): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val displayName = resolveDisplayName(context, uri)
            val ext = resolveAudioExtension(context, uri, displayName)
            val dir = File(context.filesDir, "alarm_audio").apply { mkdirs() }
            val safeId = alarmId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            dir.listFiles()?.filter { it.name.startsWith("alarm_${safeId}_") }?.forEach { it.delete() }

            val target = File(dir, "alarm_${safeId}_${System.currentTimeMillis()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            target.absolutePath to displayName
        } catch (_: Exception) {
            null
        }
    }

    fun deleteSlotFiles(context: Context, slotName: String) {
        val dir = File(context.filesDir, "custom_hud_media")
        if (dir.exists()) {
            dir.listFiles()?.filter {
                it.name.startsWith("${slotName}_") ||
                    (slotName == "panel" && it.name.startsWith("panel_slice_"))
            }?.forEach { it.delete() }
        }
    }

    fun deleteAudioFile(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }

    /**
     * Produces a resized Bitmap (<= 640px) of the active background media so multimodal AI
     * models can inspect its color palette, lighting, and visual theme.
     */
    suspend fun decodeBackgroundSnapshotForAi(context: Context, path: String?): Bitmap? =
        withContext(Dispatchers.IO) {
            if (path.isNullOrBlank()) return@withContext null
            val file = File(path)
            if (!file.exists()) return@withContext null

            when (MediaFormatKind.fromPath(path)) {
                MediaFormatKind.LOTTIE_JSON,
                MediaFormatKind.DOT_LOTTIE,
                MediaFormatKind.RIVE_ANIM -> null

                MediaFormatKind.VECTOR_SVG,
                MediaFormatKind.ANIMATED_GIF -> {
                    try {
                        val loader = createMediaImageLoader(context)
                        val req = ImageRequest.Builder(context)
                            .data(file)
                            .size(512, 512)
                            .allowHardware(false)
                            .build()
                        val res = loader.execute(req)
                        val drawable = (res as? SuccessResult)?.drawable
                        when (drawable) {
                            is BitmapDrawable -> drawable.bitmap
                            null -> null
                            else -> {
                                val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
                                val canvas = Canvas(bmp)
                                drawable.setBounds(0, 0, canvas.width, canvas.height)
                                drawable.draw(canvas)
                                bmp
                            }
                        }
                    } catch (_: Exception) {
                        null
                    }
                }

                MediaFormatKind.RASTER_IMAGE -> {
                    try {
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(file.absolutePath, bounds)
                        var sample = 1
                        while (bounds.outWidth / sample > 640 || bounds.outHeight / sample > 640) {
                            sample *= 2
                        }
                        val opts = BitmapFactory.Options().apply {
                            inSampleSize = sample
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        }
                        BitmapFactory.decodeFile(file.absolutePath, opts)
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        }
}
