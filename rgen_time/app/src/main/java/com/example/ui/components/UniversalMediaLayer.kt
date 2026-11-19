package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Fit
import app.rive.runtime.kotlin.core.Rive
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.data.CustomMediaHelper
import com.example.data.MediaFormatKind
import com.example.model.MediaFitMode
import com.example.model.MediaTransform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream

@Composable
fun UniversalMediaLayer(
    mediaPath: String,
    transform: MediaTransform = MediaTransform(),
    alpha: Float = 1f,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val kind = remember(mediaPath) { MediaFormatKind.fromPath(mediaPath) }
    val contentScale = remember(transform.fitMode) {
        when (transform.fitMode) {
            MediaFitMode.CROP -> ContentScale.Crop
            MediaFitMode.FIT -> ContentScale.Fit
            MediaFitMode.FILL -> ContentScale.FillBounds
            MediaFitMode.INSIDE -> ContentScale.Inside
        }
    }

    val transformModifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
            scaleX = transform.scale.coerceIn(0.4f, 2.2f)
            scaleY = transform.scale.coerceIn(0.4f, 2.2f)
            translationX = transform.offsetX * size.width
            translationY = transform.offsetY * size.height
            this.alpha = alpha.coerceIn(0.05f, 1f)
        }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (kind) {
            MediaFormatKind.LOTTIE_JSON,
            MediaFormatKind.DOT_LOTTIE -> {
                var jsonPayload by remember(mediaPath) { mutableStateOf<String?>(null) }
                LaunchedEffect(mediaPath, kind) {
                    jsonPayload = withContext(Dispatchers.IO) {
                        readLottieJsonSafely(File(mediaPath), kind)
                    }
                }
                val spec = if (!jsonPayload.isNullOrBlank()) {
                    LottieCompositionSpec.JsonString(jsonPayload!!)
                } else {
                    LottieCompositionSpec.File(mediaPath)
                }
                val composition by rememberLottieComposition(spec)
                val progress by animateLottieCompositionAsState(
                    composition = composition,
                    iterations = LottieConstants.IterateForever
                )
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    contentScale = contentScale,
                    modifier = transformModifier
                )
            }

            MediaFormatKind.RIVE_ANIM -> {
                var riveBytes by remember(mediaPath) { mutableStateOf<ByteArray?>(null) }
                LaunchedEffect(mediaPath) {
                    riveBytes = withContext(Dispatchers.IO) {
                        runCatching { Rive.init(context.applicationContext) }
                        runCatching { File(mediaPath).takeIf { it.exists() }?.readBytes() }.getOrNull()
                    }
                }
                val bytes = riveBytes
                if (bytes != null && bytes.isNotEmpty()) {
                    val riveFit = when (transform.fitMode) {
                        MediaFitMode.CROP -> Fit.COVER
                        MediaFitMode.FIT -> Fit.CONTAIN
                        MediaFitMode.FILL -> Fit.FILL
                        MediaFitMode.INSIDE -> Fit.SCALE_DOWN
                    }
                    AndroidView(
                        factory = { ctx ->
                            runCatching { Rive.init(ctx.applicationContext) }
                            RiveAnimationView(ctx).apply {
                                runCatching {
                                    setRiveBytes(bytes, autoplay = true, fit = riveFit)
                                }
                            }
                        },
                        update = { view ->
                            runCatching {
                                view.fit = riveFit
                            }
                        },
                        modifier = transformModifier
                    )
                }
            }

            MediaFormatKind.RASTER_IMAGE,
            MediaFormatKind.ANIMATED_GIF,
            MediaFormatKind.VECTOR_SVG -> {
                val imageLoader = remember(context) { CustomMediaHelper.createMediaImageLoader(context) }
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(mediaPath))
                        .crossfade(true)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = transformModifier
                )
            }
        }
    }
}

private fun readLottieJsonSafely(file: File, kind: MediaFormatKind): String? {
    if (!file.exists()) return null
    return try {
        if (kind == MediaFormatKind.LOTTIE_JSON) {
            file.readText(Charsets.UTF_8)
        } else {
            // .lottie is a zip container; look for animations/*.json if present
            ZipInputStream(file.inputStream().buffered()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.endsWith(".json", ignoreCase = true) &&
                        !entry.name.endsWith("manifest.json", ignoreCase = true)
                    ) {
                        return zis.readBytes().toString(Charsets.UTF_8)
                    }
                    entry = zis.nextEntry
                }
            }
            null
        }
    } catch (_: Exception) {
        null
    }
}
