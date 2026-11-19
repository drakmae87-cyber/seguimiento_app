package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.model.CanvasOverlayStyle
import com.example.model.IrisPupilStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BiolumCatEyesCanvas(
    eyeColor: Color,
    secondaryGlow: Color,
    glowIntensity: Float,
    showWhiskers: Boolean,
    beatPulse: Float,
    contentDesc: String,
    modifier: Modifier = Modifier,
    overlayStyle: CanvasOverlayStyle = CanvasOverlayStyle.CELESTIAL_CORE,
    pupilStyle: IrisPupilStyle = IrisPupilStyle.CROSSHAIR,
    showBrows: Boolean = true,
    blinkEnabled: Boolean = true,
    verticalOffsetFrac: Float = 0f,
    horizontalOffsetFrac: Float = 0f,
    collageProportionalMode: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "core_canvas_anim")
    val cycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "anim_cycle"
    )

    val phaseModulation = if (blinkEnabled) {
        0.82f + 0.18f * sin(cycle * 2f * PI).toFloat()
    } else {
        1f
    }

    val effectiveGlow = ((glowIntensity + beatPulse * 0.16f) * phaseModulation).coerceIn(0.3f, 1.15f)

    Canvas(
        modifier = modifier.semantics { contentDescription = contentDesc }
    ) {
        val w = size.width
        val h = size.height
        val cx = w * (0.5f + horizontalOffsetFrac.coerceIn(-0.4f, 0.4f))
        val baseCenterY = if (collageProportionalMode) h * 0.34f else h * 0.49f
        val cy = baseCenterY + (h * verticalOffsetFrac.coerceIn(-0.4f, 0.4f))
        val designHeight = if (collageProportionalMode) h * 0.56f else h

        when (overlayStyle) {
            CanvasOverlayStyle.CELESTIAL_CORE -> {
                drawOrbitalCoreOverlay(
                    w = w,
                    h = designHeight,
                    cx = cx,
                    cy = cy,
                    cycle = cycle,
                    eyeColor = eyeColor,
                    secondaryGlow = secondaryGlow,
                    effectiveGlow = effectiveGlow,
                    pupilStyle = pupilStyle,
                    showArcs = showBrows,
                    showReticleTraces = showWhiskers
                )
            }

            CanvasOverlayStyle.HUD_RETICLE -> {
                drawHudReticleOverlay(
                    w = w,
                    h = designHeight,
                    cx = cx,
                    cy = cy,
                    cycle = cycle,
                    eyeColor = eyeColor,
                    secondaryGlow = secondaryGlow,
                    effectiveGlow = effectiveGlow,
                    pupilStyle = pupilStyle,
                    showArcs = showBrows,
                    showReticleTraces = showWhiskers
                )
            }

            CanvasOverlayStyle.TENSOR_PRISM -> {
                drawTensorPrism3dOverlay(
                    w = w,
                    h = designHeight,
                    cx = cx,
                    cy = cy,
                    cycle = cycle,
                    eyeColor = eyeColor,
                    secondaryGlow = secondaryGlow,
                    effectiveGlow = effectiveGlow,
                    showArcs = showBrows,
                    showReticleTraces = showWhiskers
                )
            }
        }
    }
}

private fun DrawScope.drawOrbitalCoreOverlay(
    w: Float,
    h: Float,
    cx: Float,
    cy: Float,
    cycle: Float,
    eyeColor: Color,
    secondaryGlow: Color,
    effectiveGlow: Float,
    pupilStyle: IrisPupilStyle,
    showArcs: Boolean,
    showReticleTraces: Boolean
) {
    val radius = minOf(w * 0.16f, h * 0.32f)
    val rot = cycle * 360f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                eyeColor.copy(alpha = (0.42f * effectiveGlow).coerceAtMost(0.6f)),
                secondaryGlow.copy(alpha = 0.18f),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = radius * 2.4f
        ),
        center = Offset(cx, cy),
        radius = radius * 2.4f
    )

    drawCircle(
        color = eyeColor,
        radius = radius,
        center = Offset(cx, cy),
        style = Stroke(width = 2.2.dp.toPx())
    )
    drawCircle(
        color = secondaryGlow.copy(alpha = 0.72f),
        radius = radius * 1.48f,
        center = Offset(cx, cy),
        style = Stroke(width = 1.2.dp.toPx())
    )

    if (showArcs) {
        for (i in 0..3) {
            val angle = Math.toRadians((rot + i * 90.0))
            val ox = cx + cos(angle).toFloat() * (radius * 1.48f)
            val oy = cy + sin(angle).toFloat() * (radius * 1.48f)
            drawCircle(
                color = if (i % 2 == 0) Color.White else eyeColor,
                radius = 3.5.dp.toPx(),
                center = Offset(ox, oy)
            )
        }
    }

    drawCoreIndicator(
        center = Offset(cx, cy),
        halfSpan = radius * 0.78f,
        pupilStyle = pupilStyle,
        accentColor = eyeColor
    )

    if (showReticleTraces) {
        drawTechnicalReticleTraces(w = w, h = h, cx = cx, cy = cy, accent = eyeColor, secondary = secondaryGlow)
    }
}

private fun DrawScope.drawHudReticleOverlay(
    w: Float,
    h: Float,
    cx: Float,
    cy: Float,
    cycle: Float,
    eyeColor: Color,
    secondaryGlow: Color,
    effectiveGlow: Float,
    pupilStyle: IrisPupilStyle,
    showArcs: Boolean,
    showReticleTraces: Boolean
) {
    val radius = minOf(w * 0.21f, h * 0.36f)
    val rot = cycle * 360f
    val pulseAlpha = (0.45f + 0.35f * sin(cycle * 2f * PI).toFloat()) * effectiveGlow.coerceAtMost(1f)

    // Atmospheric radial core glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                eyeColor.copy(alpha = 0.28f * effectiveGlow),
                secondaryGlow.copy(alpha = 0.12f),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = radius * 2.1f
        ),
        center = Offset(cx, cy),
        radius = radius * 2.1f
    )

    // Segmented telemetry arcs (rotating opposite directions)
    for (seg in 0..3) {
        drawArc(
            color = if (seg % 2 == 0) eyeColor else secondaryGlow,
            startAngle = rot + seg * 90f + 12f,
            sweepAngle = 56f,
            useCenter = false,
            topLeft = Offset(cx - radius, cy - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        )
    }

    if (showArcs) {
        val outerR = radius * 1.32f
        for (seg in 0..2) {
            drawArc(
                color = secondaryGlow.copy(alpha = 0.65f),
                startAngle = -rot * 0.7f + seg * 120f + 10f,
                sweepAngle = 72f,
                useCenter = false,
                topLeft = Offset(cx - outerR, cy - outerR),
                size = androidx.compose.ui.geometry.Size(outerR * 2f, outerR * 2f),
                style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }

    drawCoreIndicator(
        center = Offset(cx, cy),
        halfSpan = radius * 0.56f,
        pupilStyle = pupilStyle,
        accentColor = eyeColor
    )

    // Precision outer telemetry corner brackets
    val bracketW = w * 0.38f
    val bracketH = h * 0.44f
    for (dir in listOf(-1f, 1f)) {
        val bx = cx + dir * bracketW
        val topY = cy - bracketH * 0.54f
        val botY = cy + bracketH * 0.54f
        val cornerLen = 14.dp.toPx()
        val path = Path().apply {
            moveTo(bx - dir * cornerLen, topY)
            lineTo(bx, topY)
            lineTo(bx, botY)
            lineTo(bx - dir * cornerLen, botY)
        }
        drawPath(
            path = path,
            color = secondaryGlow.copy(alpha = pulseAlpha.coerceIn(0.25f, 0.9f)),
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        )
    }

    if (showReticleTraces) {
        drawTechnicalReticleTraces(w = w, h = h, cx = cx, cy = cy, accent = eyeColor, secondary = secondaryGlow)
    }
}

private fun DrawScope.drawTensorPrism3dOverlay(
    w: Float,
    h: Float,
    cx: Float,
    cy: Float,
    cycle: Float,
    eyeColor: Color,
    secondaryGlow: Color,
    effectiveGlow: Float,
    showArcs: Boolean,
    showReticleTraces: Boolean
) {
    val scale = minOf(w * 0.19f, h * 0.36f)
    val yaw = cycle * 2f * PI
    val pitch = 0.52 + 0.25 * sin(cycle * 2f * PI)

    // True 3D octahedron vertices projected onto 2D HUD space
    val rawVertices = listOf(
        Triple(0.0, -1.25, 0.0),
        Triple(1.0, 0.0, 0.0),
        Triple(0.0, 0.0, 1.0),
        Triple(-1.0, 0.0, 0.0),
        Triple(0.0, 0.0, -1.0),
        Triple(0.0, 1.25, 0.0)
    )

    val projected = rawVertices.map { (vx, vy, vz) ->
        val rx = vx * cos(yaw) - vz * sin(yaw)
        val rz = vx * sin(yaw) + vz * cos(yaw)
        val ry = vy * cos(pitch) - rz * sin(pitch)
        val pz = vy * sin(pitch) + rz * cos(pitch)
        val perspective = 2.6 / (2.6 - pz * 0.45)
        Offset(
            cx + (rx * scale * perspective).toFloat(),
            cy + (ry * scale * perspective).toFloat()
        )
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                eyeColor.copy(alpha = (0.34f * effectiveGlow).coerceAtMost(0.55f)),
                secondaryGlow.copy(alpha = 0.14f),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = scale * 2.1f
        ),
        center = Offset(cx, cy),
        radius = scale * 2.1f
    )

    val edges = listOf(
        0 to 1, 0 to 2, 0 to 3, 0 to 4,
        5 to 1, 5 to 2, 5 to 3, 5 to 4,
        1 to 2, 2 to 3, 3 to 4, 4 to 1
    )

    edges.forEachIndexed { idx, (a, b) ->
        drawLine(
            color = if (idx < 8) eyeColor.copy(alpha = 0.9f) else secondaryGlow.copy(alpha = 0.8f),
            start = projected[a],
            end = projected[b],
            strokeWidth = 1.9.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    if (showArcs) {
        projected.forEachIndexed { idx, pt ->
            drawCircle(
                color = if (idx == 0 || idx == 5) Color.White else secondaryGlow,
                radius = 3.dp.toPx(),
                center = pt
            )
        }
    }

    if (showReticleTraces) {
        drawTechnicalReticleTraces(w = w, h = h, cx = cx, cy = cy, accent = eyeColor, secondary = secondaryGlow)
    }
}

private fun DrawScope.drawCoreIndicator(
    center: Offset,
    halfSpan: Float,
    pupilStyle: IrisPupilStyle,
    accentColor: Color
) {
    when (pupilStyle) {
        IrisPupilStyle.SLIT -> {
            val hSpan = halfSpan * 0.28f
            val vSpan = halfSpan * 0.85f
            val path = Path().apply {
                moveTo(center.x, center.y - vSpan)
                lineTo(center.x + hSpan, center.y)
                lineTo(center.x, center.y + vSpan)
                lineTo(center.x - hSpan, center.y)
                close()
            }
            drawPath(path = path, color = Color(0xFF04090C))
            drawPath(path = path, color = accentColor, style = Stroke(width = 1.5.dp.toPx()))
        }

        IrisPupilStyle.ROUND -> {
            drawCircle(color = Color(0xFF04090C), radius = halfSpan * 0.56f, center = center)
            drawCircle(
                color = accentColor,
                radius = halfSpan * 0.36f,
                center = center,
                style = Stroke(width = 1.4.dp.toPx())
            )
        }

        IrisPupilStyle.DIAMOND -> {
            val d = halfSpan * 0.65f
            val diamond = Path().apply {
                moveTo(center.x, center.y - d)
                lineTo(center.x + d, center.y)
                lineTo(center.x, center.y + d)
                lineTo(center.x - d, center.y)
                close()
            }
            drawPath(path = diamond, color = Color(0xFF04090C))
            drawPath(path = diamond, color = accentColor, style = Stroke(width = 1.5.dp.toPx()))
        }

        IrisPupilStyle.CROSSHAIR -> {
            val r = halfSpan * 0.7f
            drawCircle(color = Color(0xFF04090C), radius = r * 0.45f, center = center)
            drawLine(
                color = accentColor,
                start = Offset(center.x - r, center.y),
                end = Offset(center.x + r, center.y),
                strokeWidth = 1.6.dp.toPx()
            )
            drawLine(
                color = accentColor,
                start = Offset(center.x, center.y - r),
                end = Offset(center.x, center.y + r),
                strokeWidth = 1.6.dp.toPx()
            )
        }
    }
}

private fun DrawScope.drawTechnicalReticleTraces(
    w: Float,
    h: Float,
    cx: Float,
    cy: Float,
    accent: Color,
    secondary: Color
) {
    for (side in listOf(-1f, 1f)) {
        val startX = cx + side * (w * 0.34f)
        val endX = cx + side * (w * 0.47f)
        for (row in -1..1) {
            val y = cy + row * (h * 0.08f)
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = if (side < 0f) {
                        listOf(Color.Transparent, if (row == 0) secondary.copy(alpha = 0.8f) else accent.copy(alpha = 0.62f))
                    } else {
                        listOf(if (row == 0) secondary.copy(alpha = 0.8f) else accent.copy(alpha = 0.62f), Color.Transparent)
                    },
                    startX = minOf(startX, endX),
                    endX = maxOf(startX, endX)
                ),
                start = Offset(startX, y),
                end = Offset(endX, y),
                strokeWidth = if (row == 0) 1.7.dp.toPx() else 1.2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
