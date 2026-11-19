package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

/**
 * Exact 5-panel Multi-Frame Collage slicer for COLLAGER4.
 *
 * Divides a single uploaded image continuously across the 5 staggered panels
 * (Day, Hours, Minutes, Seconds, Date) matching their exact width weights and
 * centered staggered vertical heights rather than repeating the image in each frame.
 */
object CollageImageSlicer {

    val PANEL_WEIGHTS = floatArrayOf(0.88f, 1.06f, 1.22f, 1.06f, 0.88f)
    val PANEL_HEIGHT_FRACTIONS = floatArrayOf(0.64f, 0.82f, 1.00f, 0.82f, 0.64f)
    private const val GAP_FRACTION_PER_DIVIDER = 0.014f
    private const val VIRTUAL_COLLAGE_WIDTH = 1500
    private const val VIRTUAL_COLLAGE_HEIGHT = 900

    data class PanelSliceRegion(
        val xStartFrac: Float,
        val widthFrac: Float,
        val yStartFrac: Float,
        val heightFrac: Float
    )

    fun computePanelRegions(): List<PanelSliceRegion> {
        val totalWeight = PANEL_WEIGHTS.sum()
        val totalGaps = GAP_FRACTION_PER_DIVIDER * (PANEL_WEIGHTS.size - 1)
        val usableWidth = (1f - totalGaps).coerceAtLeast(0.5f)

        var cursorX = 0f
        return PANEL_WEIGHTS.indices.map { index ->
            val widthFrac = (PANEL_WEIGHTS[index] / totalWeight) * usableWidth
            val heightFrac = PANEL_HEIGHT_FRACTIONS[index]
            val yStartFrac = (1f - heightFrac) * 0.5f
            val region = PanelSliceRegion(
                xStartFrac = cursorX,
                widthFrac = widthFrac,
                yStartFrac = yStartFrac,
                heightFrac = heightFrac
            )
            cursorX += widthFrac + GAP_FRACTION_PER_DIVIDER
            region
        }
    }

    /**
     * Generates 5 cropped PNG slice files for static images (.png, .jpg) and .svg.
     * Returns emptyList() for animated .gif so Compose renders aligned animated viewports.
     */
    suspend fun sliceImageForCollager4(
        context: Context,
        sourceFilePath: String
    ): List<String> = withContext(Dispatchers.IO) {
        val sourceFile = File(sourceFilePath)
        if (!sourceFile.exists()) return@withContext emptyList()

        // Preserve animated formats (.gif, .json, .lottie, .riv) via aligned viewports in Compose
        val ext = sourceFile.extension.lowercase()
        if (ext in setOf("gif", "json", "lottie", "riv")) {
            deleteOldSliceFiles(context)
            return@withContext emptyList()
        }

        val sourceBitmap = decodeMasterBitmap(context, sourceFile) ?: return@withContext emptyList()
        val virtualCanvasBitmap = centerCropToCollageCanvas(
            source = sourceBitmap,
            targetWidth = VIRTUAL_COLLAGE_WIDTH,
            targetHeight = VIRTUAL_COLLAGE_HEIGHT
        )
        if (sourceBitmap !== virtualCanvasBitmap) {
            sourceBitmap.recycle()
        }

        val dir = File(context.filesDir, "custom_hud_media").apply { mkdirs() }
        deleteOldSliceFiles(context)

        val regions = computePanelRegions()
        val timestamp = System.currentTimeMillis()
        val outputPaths = mutableListOf<String>()

        try {
            regions.forEachIndexed { index, region ->
                val x = (region.xStartFrac * VIRTUAL_COLLAGE_WIDTH).roundToInt()
                    .coerceIn(0, VIRTUAL_COLLAGE_WIDTH - 2)
                val y = (region.yStartFrac * VIRTUAL_COLLAGE_HEIGHT).roundToInt()
                    .coerceIn(0, VIRTUAL_COLLAGE_HEIGHT - 2)
                val w = (region.widthFrac * VIRTUAL_COLLAGE_WIDTH).roundToInt()
                    .coerceIn(2, VIRTUAL_COLLAGE_WIDTH - x)
                val h = (region.heightFrac * VIRTUAL_COLLAGE_HEIGHT).roundToInt()
                    .coerceIn(2, VIRTUAL_COLLAGE_HEIGHT - y)

                val sliceBmp = Bitmap.createBitmap(virtualCanvasBitmap, x, y, w, h)
                val outFile = File(dir, "panel_slice_${index}_$timestamp.png")
                FileOutputStream(outFile).use { fos ->
                    sliceBmp.compress(Bitmap.CompressFormat.PNG, 95, fos)
                }
                sliceBmp.recycle()
                outputPaths.add(outFile.absolutePath)
            }
        } finally {
            virtualCanvasBitmap.recycle()
        }

        outputPaths
    }

    fun deleteOldSliceFiles(context: Context) {
        val dir = File(context.filesDir, "custom_hud_media")
        if (dir.exists()) {
            dir.listFiles()?.filter { it.name.startsWith("panel_slice_") }?.forEach { it.delete() }
        }
    }

    private suspend fun decodeMasterBitmap(context: Context, file: File): Bitmap? {
        return try {
            if (file.extension.equals("svg", ignoreCase = true)) {
                val loader = CustomMediaHelper.createMediaImageLoader(context)
                val req = ImageRequest.Builder(context)
                    .data(file)
                    .size(VIRTUAL_COLLAGE_WIDTH, VIRTUAL_COLLAGE_HEIGHT)
                    .allowHardware(false)
                    .build()
                val result = loader.execute(req)
                val drawable = (result as? SuccessResult)?.drawable
                when (drawable) {
                    is BitmapDrawable -> drawable.bitmap
                    null -> null
                    else -> {
                        val bmp = Bitmap.createBitmap(
                            VIRTUAL_COLLAGE_WIDTH,
                            VIRTUAL_COLLAGE_HEIGHT,
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = Canvas(bmp)
                        drawable.setBounds(0, 0, canvas.width, canvas.height)
                        drawable.draw(canvas)
                        bmp
                    }
                }
            } else {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                var sampleSize = 1
                while (bounds.outWidth / sampleSize > VIRTUAL_COLLAGE_WIDTH * 2 ||
                    bounds.outHeight / sampleSize > VIRTUAL_COLLAGE_HEIGHT * 2
                ) {
                    sampleSize *= 2
                }
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeFile(file.absolutePath, opts)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun centerCropToCollageCanvas(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap {
        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val srcW = source.width.toFloat().coerceAtLeast(1f)
        val srcH = source.height.toFloat().coerceAtLeast(1f)
        val scale = maxOf(targetWidth / srcW, targetHeight / srcH)
        val scaledW = srcW * scale
        val scaledH = srcH * scale
        val left = (targetWidth - scaledW) * 0.5f
        val top = (targetHeight - scaledH) * 0.5f
        val dstRect = RectF(left, top, left + scaledW, top + scaledH)
        canvas.drawBitmap(source, Rect(0, 0, source.width, source.height), dstRect, null)
        return output
    }
}
