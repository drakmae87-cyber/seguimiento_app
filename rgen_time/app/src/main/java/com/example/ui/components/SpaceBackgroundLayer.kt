package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.model.CustomFxLayer
import com.example.model.FxCategory
import com.example.model.MediaTransform
import com.example.ui.theme.LocalNekoPalette
import com.example.ui.theme.StageVoid
import com.example.ui.theme.parseHexColorOrNull
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

private data class RetroStar(
    val xFrac: Float,
    val yFrac: Float,
    val sizeDp: Float,
    val twinkleOffset: Float,
    val isFlare: Boolean
)

private data class StreamParticle(
    val lane: Float,
    val phaseOffset: Float,
    val speed: Float,
    val radiusDp: Float,
    val hueIndex: Int
)

private data class PhotonStreak(
    val xFrac: Float,
    val phase: Float,
    val speed: Float,
    val lenFrac: Float
)

@Composable
fun SpaceBackgroundLayer(
    customBackgroundPath: String?,
    customBackgroundAlpha: Float,
    starfieldEnabled: Boolean,
    particleStreamEnabled: Boolean,
    auroraNebulaEnabled: Boolean = true,
    anamorphicFlareEnabled: Boolean = true,
    gravityHorizonEnabled: Boolean = false,
    cinematicScanlinesEnabled: Boolean = false,
    perspectiveGrid3dEnabled: Boolean = true,
    holoPrism3dEnabled: Boolean = false,
    neonPulseRingsEnabled: Boolean = true,
    cyberMatrixRainEnabled: Boolean = false,
    atmosphericFogEnabled: Boolean = true,
    fxIntensity: Float = 0.85f,
    customFxLayers: List<CustomFxLayer> = emptyList(),
    batteryOptimized: Boolean = true,
    reducedBackdropLoad: Boolean = false,
    bgTransform: MediaTransform = MediaTransform(),
    modifier: Modifier = Modifier
) {
    val palette = LocalNekoPalette.current
    val intensity = fxIntensity.coerceIn(0.2f, 1.5f)
    val starCount = if (batteryOptimized && reducedBackdropLoad) 42 else 82
    val particleCount = if (batteryOptimized && reducedBackdropLoad) 36 else 88

    val stars = remember {
        val rng = Random(1998)
        List(82) { idx ->
            RetroStar(
                xFrac = rng.nextFloat(),
                yFrac = rng.nextFloat(),
                sizeDp = 0.8f + rng.nextFloat() * 1.9f,
                twinkleOffset = rng.nextFloat() * (2f * PI.toFloat()),
                isFlare = idx % 10 == 0
            )
        }
    }

    val particles = remember {
        val rng = Random(2099)
        List(88) { idx ->
            StreamParticle(
                lane = (rng.nextFloat() - 0.5f) * 2f,
                phaseOffset = rng.nextFloat(),
                speed = 0.7f + rng.nextFloat() * 0.75f,
                radiusDp = 1.1f + rng.nextFloat() * 1.9f,
                hueIndex = idx % 3
            )
        }
    }

    val streaks = remember {
        val rng = Random(3077)
        List(26) {
            PhotonStreak(
                xFrac = rng.nextFloat(),
                phase = rng.nextFloat(),
                speed = 0.65f + rng.nextFloat() * 0.9f,
                lenFrac = 0.06f + rng.nextFloat() * 0.12f
            )
        }
    }

    val animPhase: Float
    val slowCinematicPhase: Float
    if (batteryOptimized && reducedBackdropLoad) {
        animPhase = 0.25f
        slowCinematicPhase = 0.35f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "cosmos_stream")
        val fast by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 7200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "stream_phase"
        )
        val slow by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 18000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "cinematic_phase"
        )
        animPhase = fast
        slowCinematicPhase = slow
    }

    val enabledCustomLayers = remember(customFxLayers) {
        customFxLayers.filter { it.enabled }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        palette.stageBackdropTop,
                        StageVoid,
                        palette.stageBackdropBottom
                    )
                )
            )
    ) {
        if (!customBackgroundPath.isNullOrBlank()) {
            UniversalMediaLayer(
                mediaPath = customBackgroundPath,
                transform = bgTransform,
                alpha = customBackgroundAlpha.coerceIn(0.10f, 1.0f),
                modifier = Modifier.fillMaxSize()
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Ambient corner volumetric pools
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.primaryGlow.copy(alpha = (0.16f * intensity).coerceAtMost(0.32f)),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.14f, h * 0.20f),
                    radius = w * 0.62f
                ),
                center = Offset(w * 0.14f, h * 0.20f),
                radius = w * 0.62f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.secondaryGlow.copy(alpha = (0.13f * intensity).coerceAtMost(0.28f)),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.86f, h * 0.78f),
                    radius = w * 0.56f
                ),
                center = Offset(w * 0.86f, h * 0.78f),
                radius = w * 0.56f
            )

            if (atmosphericFogEnabled) {
                val fogShift = sin(slowCinematicPhase * 2f * PI).toFloat() * w * 0.08f
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            palette.primaryGlow.copy(alpha = (0.07f * intensity).coerceAtMost(0.18f)),
                            palette.secondaryGlow.copy(alpha = (0.09f * intensity).coerceAtMost(0.20f)),
                            Color.Transparent
                        )
                    ),
                    topLeft = Offset(0f, h * 0.42f + fogShift * 0.1f),
                    size = Size(w, h * 0.52f)
                )
            }

            if (auroraNebulaEnabled) {
                val drift = slowCinematicPhase * 2f * PI.toFloat()
                for (band in 0..1) {
                    val baseY = h * (0.24f + band * 0.16f)
                    val wavePath = Path().apply {
                        moveTo(0f, baseY)
                        val steps = 8
                        for (i in 1..steps) {
                            val xf = i / steps.toFloat()
                            val x = xf * w
                            val y = baseY + sin(drift + xf * 4.5f + band * 1.4f) * (h * 0.065f)
                            lineTo(x, y)
                        }
                        lineTo(w, baseY + h * 0.26f)
                        lineTo(0f, baseY + h * 0.26f)
                        close()
                    }
                    val bandColor = if (band == 0) palette.primaryGlow else palette.secondaryGlow
                    drawPath(
                        path = wavePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                bandColor.copy(alpha = (0.11f * intensity).coerceAtMost(0.24f)),
                                bandColor.copy(alpha = (0.03f * intensity).coerceAtMost(0.08f)),
                                Color.Transparent
                            ),
                            startY = baseY - h * 0.07f,
                            endY = baseY + h * 0.28f
                        )
                    )
                }
            }

            if (perspectiveGrid3dEnabled) {
                drawPerspectiveGrid3D(
                    w = w,
                    h = h,
                    phase = animPhase,
                    primary = palette.primaryGlow,
                    secondary = palette.secondaryGlow,
                    intensity = intensity
                )
            }

            if (holoPrism3dEnabled && !reducedBackdropLoad) {
                drawFloating3dWireCube(
                    w = w,
                    h = h,
                    phase = slowCinematicPhase,
                    primary = palette.primaryGlow,
                    secondary = palette.secondaryGlow,
                    intensity = intensity
                )
            }

            if (gravityHorizonEnabled) {
                val center = Offset(w * 0.5f, h * 0.42f)
                val maxR = maxOf(w, h) * 0.65f
                for (ring in 0..3) {
                    val t = (slowCinematicPhase + ring * 0.25f) % 1f
                    val r = t * maxR
                    val alpha = (1f - t).pow(1.6f) * (0.22f * intensity).coerceAtMost(0.4f)
                    drawCircle(
                        color = if (ring % 2 == 0) palette.primaryGlow.copy(alpha = alpha) else palette.secondaryGlow.copy(alpha = alpha),
                        radius = r,
                        center = center,
                        style = Stroke(width = (1.5f + (1f - t) * 2f).dp.toPx())
                    )
                }
            }

            if (neonPulseRingsEnabled) {
                val pulseA = (animPhase * 1.0f) % 1f
                val pulseB = (animPhase + 0.5f) % 1f
                val center = Offset(w * 0.5f, h * 0.38f)
                listOf(pulseA to palette.primaryGlow, pulseB to palette.secondaryGlow).forEach { (p, col) ->
                    val r = w * (0.16f + p * 0.58f)
                    val a = ((1f - p) * 0.24f * intensity).coerceIn(0f, 0.42f)
                    drawCircle(
                        color = col.copy(alpha = a),
                        radius = r,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            if (starfieldEnabled) {
                for (i in 0 until starCount) {
                    val star = stars[i]
                    val sx = star.xFrac * w
                    val sy = star.yFrac * h
                    val twinkle = 0.35f + 0.65f * (0.5f + 0.5f * sin(animPhase * 2f * PI + star.twinkleOffset).toFloat())
                    val radius = star.sizeDp.dp.toPx()

                    drawCircle(
                        color = Color.White.copy(alpha = (twinkle * 0.88f * intensity).coerceAtMost(0.98f)),
                        radius = radius,
                        center = Offset(sx, sy)
                    )

                    if (star.isFlare) {
                        val flareLen = radius * 3.8f
                        drawLine(
                            color = palette.secondaryGlow.copy(alpha = twinkle * 0.60f),
                            start = Offset(sx - flareLen, sy),
                            end = Offset(sx + flareLen, sy),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = palette.secondaryGlow.copy(alpha = twinkle * 0.60f),
                            start = Offset(sx, sy - flareLen),
                            end = Offset(sx, sy + flareLen),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
            }

            if (anamorphicFlareEnabled) {
                val flareAngle = slowCinematicPhase * 2f * PI.toFloat()
                val flareY = h * (0.31f + sin(flareAngle) * 0.03f)
                val flareX = w * (0.5f + cos(flareAngle * 0.5f) * 0.14f)
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            palette.primaryGlow.copy(alpha = (0.14f * intensity).coerceAtMost(0.32f)),
                            Color.White.copy(alpha = (0.24f * intensity).coerceAtMost(0.45f)),
                            palette.secondaryGlow.copy(alpha = (0.14f * intensity).coerceAtMost(0.32f)),
                            Color.Transparent
                        )
                    ),
                    topLeft = Offset(0f, flareY - 2.dp.toPx()),
                    size = Size(w, 4.dp.toPx())
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.primaryGlow.copy(alpha = (0.18f * intensity).coerceAtMost(0.35f)),
                            Color.Transparent
                        ),
                        center = Offset(flareX, flareY),
                        radius = w * 0.24f
                    ),
                    center = Offset(flareX, flareY),
                    radius = w * 0.24f
                )
            }

            if (cyberMatrixRainEnabled && !reducedBackdropLoad) {
                streaks.forEach { st ->
                    val yHead = ((st.phase + animPhase * st.speed * 1.4f) % 1.15f) * h
                    val x = st.xFrac * w
                    val len = st.lenFrac * h
                    drawLine(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, palette.secondaryGlow.copy(alpha = 0.42f * intensity), Color.White.copy(alpha = 0.5f)),
                            startY = (yHead - len).coerceAtLeast(0f),
                            endY = yHead
                        ),
                        start = Offset(x, (yHead - len).coerceAtLeast(0f)),
                        end = Offset(x, yHead),
                        strokeWidth = 1.3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            if (particleStreamEnabled) {
                val centerX = w * 0.5f
                for (i in 0 until particleCount) {
                    val p = particles[i]
                    val t = (p.phaseOffset + animPhase * p.speed) % 1f
                    val yFrac = 1.02f - t * 1.05f
                    val spread = (1f - t).pow(2.4f) * (w * 0.52f) + (w * 0.055f)
                    val px = centerX + p.lane * spread + sin((t * 6.0 + p.phaseOffset * 10.0)).toFloat() * 6.dp.toPx()
                    val py = yFrac * h

                    val particleAlpha = (when {
                        t < 0.12f -> t / 0.12f
                        t > 0.88f -> (1f - t) / 0.12f
                        else -> 1f
                    }.coerceIn(0f, 1f) * 0.78f * intensity).coerceAtMost(0.95f)

                    val color = when (p.hueIndex) {
                        0 -> palette.primaryGlow
                        1 -> palette.secondaryGlow
                        else -> palette.highlightPop
                    }

                    val dotRadius = p.radiusDp.dp.toPx() * (0.65f + (1f - t) * 0.6f)
                    drawCircle(
                        color = color.copy(alpha = particleAlpha),
                        radius = dotRadius,
                        center = Offset(px, py)
                    )
                    if (!reducedBackdropLoad) {
                        drawLine(
                            color = color.copy(alpha = particleAlpha * 0.45f),
                            start = Offset(px, py),
                            end = Offset(px, py + 10.dp.toPx() * t),
                            strokeWidth = dotRadius * 0.9f
                        )
                    }
                }
            }

            if (cinematicScanlinesEnabled) {
                val sweepY = ((animPhase * 1.15f) % 1f) * h
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            palette.primaryGlow.copy(alpha = (0.14f * intensity).coerceAtMost(0.28f)),
                            Color.Transparent
                        ),
                        startY = sweepY - 24.dp.toPx(),
                        endY = sweepY + 24.dp.toPx()
                    ),
                    topLeft = Offset(0f, (sweepY - 24.dp.toPx()).coerceAtLeast(0f)),
                    size = Size(w, 48.dp.toPx())
                )
            }

            // User-added Custom FX Layers from the FX Studio section
            if (!reducedBackdropLoad) {
                enabledCustomLayers.forEachIndexed { idx, layer ->
                    val fxColor = parseHexColorOrNull(layer.colorHex) ?: palette.primaryGlow
                    val effPhase = ((animPhase * layer.speed) + idx * 0.17f) % 1f
                    drawUserCustomFxLayer(
                        layer = layer,
                        color = fxColor,
                        phase = effPhase,
                        w = w,
                        h = h,
                        intensity = intensity
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawPerspectiveGrid3D(
    w: Float,
    h: Float,
    phase: Float,
    primary: Color,
    secondary: Color,
    intensity: Float
) {
    val horizonY = h * 0.62f
    val bottomY = h
    val vpX = w * 0.5f
    val lineAlpha = (0.20f * intensity).coerceAtMost(0.42f)

    // Horizon neon glow bar
    drawLine(
        brush = Brush.horizontalGradient(
            listOf(Color.Transparent, primary.copy(alpha = lineAlpha * 1.5f), secondary.copy(alpha = lineAlpha * 1.5f), Color.Transparent)
        ),
        start = Offset(0f, horizonY),
        end = Offset(w, horizonY),
        strokeWidth = 1.4.dp.toPx()
    )

    // Converging perspective rays
    for (i in -6..6) {
        val bottomX = vpX + i * (w * 0.16f)
        drawLine(
            brush = Brush.verticalGradient(
                listOf(primary.copy(alpha = 0.04f), secondary.copy(alpha = lineAlpha)),
                startY = horizonY,
                endY = bottomY
            ),
            start = Offset(vpX + i * (w * 0.025f), horizonY),
            end = Offset(bottomX, bottomY),
            strokeWidth = 1.dp.toPx()
        )
    }

    // Receding horizontal grid lines (perspective depth spacing)
    val rows = 6
    for (r in 0 until rows) {
        val t = ((r.toFloat() / rows) + phase) % 1f
        val depth = t * t
        val y = horizonY + depth * (bottomY - horizonY)
        drawLine(
            color = primary.copy(alpha = (depth * lineAlpha).coerceIn(0.02f, lineAlpha)),
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 1.dp.toPx()
        )
    }
}

private fun DrawScope.drawFloating3dWireCube(
    w: Float,
    h: Float,
    phase: Float,
    primary: Color,
    secondary: Color,
    intensity: Float
) {
    val cx = w * 0.80f
    val cy = h * 0.22f
    val radius = minOf(w, h) * 0.085f
    val ax = phase * 2.0 * PI
    val ay = phase * 4.0 * PI * 0.5

    val verts = listOf(
        Triple(-1.0, -1.0, -1.0), Triple(1.0, -1.0, -1.0),
        Triple(1.0, 1.0, -1.0), Triple(-1.0, 1.0, -1.0),
        Triple(-1.0, -1.0, 1.0), Triple(1.0, -1.0, 1.0),
        Triple(1.0, 1.0, 1.0), Triple(-1.0, 1.0, 1.0)
    )

    val pts = verts.map { (x, y, z) ->
        val x1 = x * cos(ay) - z * sin(ay)
        val z1 = x * sin(ay) + z * cos(ay)
        val y2 = y * cos(ax) - z1 * sin(ax)
        val z2 = y * sin(ax) + z1 * cos(ax)
        val persp = 3.0 / (3.0 - z2 * 0.5)
        Offset(cx + (x1 * radius * persp).toFloat(), cy + (y2 * radius * persp).toFloat())
    }

    val edges = listOf(
        0 to 1, 1 to 2, 2 to 3, 3 to 0,
        4 to 5, 5 to 6, 6 to 7, 7 to 4,
        0 to 4, 1 to 5, 2 to 6, 3 to 7
    )
    val a = (0.30f * intensity).coerceAtMost(0.60f)
    edges.forEachIndexed { i, (p1, p2) ->
        drawLine(
            color = if (i < 4) primary.copy(alpha = a) else secondary.copy(alpha = a),
            start = pts[p1],
            end = pts[p2],
            strokeWidth = 1.2.dp.toPx()
        )
    }
}

private fun DrawScope.drawUserCustomFxLayer(
    layer: CustomFxLayer,
    color: Color,
    phase: Float,
    w: Float,
    h: Float,
    intensity: Float
) {
    val alphaBase = (0.36f * layer.density * intensity).coerceIn(0.08f, 0.72f)
    when (layer.category) {
        FxCategory.PARTICLES -> {
            val count = (12 * layer.density).toInt().coerceIn(5, 24)
            for (i in 0 until count) {
                val frac = (i / count.toFloat() + phase) % 1f
                val px = ((i * 0.37f + sin(frac * 6.28f) * 0.05f) % 1f) * w
                val py = (1f - frac) * h
                drawCircle(
                    color = color.copy(alpha = alphaBase),
                    radius = (2.2f + (i % 3)).dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }

        FxCategory.ATMOSPHERE -> {
            val waveY = h * (0.28f + 0.14f * sin(phase * 2f * PI).toFloat())
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = alphaBase * 0.55f), Color.Transparent),
                    center = Offset(w * 0.5f, waveY),
                    radius = w * 0.55f
                ),
                center = Offset(w * 0.5f, waveY),
                radius = w * 0.55f
            )
        }

        FxCategory.THREE_D -> {
            val cx = w * 0.22f
            val cy = h * 0.24f
            val r = minOf(w, h) * 0.075f * layer.density.coerceIn(0.6f, 1.3f)
            val angle = phase * 2f * PI
            val pts = (0 until 6).map { k ->
                val a = angle + k * (PI / 3.0)
                Offset(cx + (cos(a) * r).toFloat(), cy + (sin(a) * r * 0.6f).toFloat())
            }
            for (k in pts.indices) {
                drawLine(
                    color = color.copy(alpha = alphaBase),
                    start = pts[k],
                    end = pts[(k + 1) % pts.size],
                    strokeWidth = 1.4.dp.toPx()
                )
                drawLine(
                    color = color.copy(alpha = alphaBase * 0.5f),
                    start = Offset(cx, cy - r * 0.7f),
                    end = pts[k],
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        FxCategory.NEON_EFX -> {
            val sweep = phase * w
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, color.copy(alpha = alphaBase), Color.Transparent),
                    startX = sweep - w * 0.2f,
                    endX = sweep + w * 0.2f
                ),
                topLeft = Offset(0f, h * 0.14f),
                size = Size(w, 2.5.dp.toPx())
            )
            drawCircle(
                color = color.copy(alpha = alphaBase * 0.6f),
                radius = w * (0.12f + phase * 0.35f),
                center = Offset(w * 0.5f, h * 0.5f),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}
