package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MediaTransform
import com.example.ui.theme.InkCream
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalNekoPalette
import com.example.ui.theme.StageVoid
import com.example.viewmodel.ClockFormatters
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PlasmaInterstellarClock(
    nowMillis: Long,
    customCoreArtPath: String?,
    panelTransform: MediaTransform = MediaTransform(),
    panelAlpha: Float = 0.45f,
    use24Hour: Boolean,
    showSeconds: Boolean,
    beatPulseEnabled: Boolean,
    onDialClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNekoPalette.current

    val calendar = remember(nowMillis) {
        Calendar.getInstance().apply { timeInMillis = nowMillis }
    }
    val hours = calendar.get(Calendar.HOUR)
    val minutes = calendar.get(Calendar.MINUTE)
    val seconds = calendar.get(Calendar.SECOND)
    val millisInSecond = calendar.get(Calendar.MILLISECOND)

    val infiniteTransition = rememberInfiniteTransition(label = "interstellar_plasma")
    val accretionRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "accretion_rotation"
    )
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle"
    )

    val mainTimeText = ClockFormatters.formatMainTime(nowMillis, use24Hour)
    val secondsText = ClockFormatters.formatSeconds(nowMillis)
    val amPmText = if (!use24Hour) ClockFormatters.formatAmPm(nowMillis) else ""

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDialClick
            )
            .testTag("cat_clock_dial"),
        contentAlignment = Alignment.Center
    ) {
        val dialSize = minOf(maxWidth, 326.dp)

        Box(
            modifier = Modifier.size(dialSize),
            contentAlignment = Alignment.Center
        ) {
            // Event Horizon Core (pure cosmic void or optional user-picked gallery art)
            val coreDiameter = dialSize * 0.68f
            Box(
                modifier = Modifier
                    .size(coreDiameter)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF010308),
                                palette.stageBackdropTop.copy(alpha = 0.85f),
                                StageVoid
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!customCoreArtPath.isNullOrBlank()) {
                    UniversalMediaLayer(
                        mediaPath = customCoreArtPath,
                        transform = panelTransform,
                        alpha = panelAlpha,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        StageVoid.copy(alpha = 0.25f),
                                        StageVoid.copy(alpha = 0.82f)
                                    )
                                )
                            )
                    )
                }
            }

            // Interstellar Atmospheric Accretion Rings & Aerospace Clock Canvas (Zero cat ears)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val radius = size.minDimension * 0.41f

                // 1. Wide atmospheric gravitational lensing bloom
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            palette.primaryGlow.copy(alpha = 0.14f),
                            palette.secondaryGlow.copy(alpha = 0.24f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = radius * 1.24f
                    ),
                    radius = radius * 1.24f,
                    center = Offset(cx, cy)
                )

                // 2. Rotating Accretion Plasma Ring
                rotate(degrees = accretionRotation, pivot = Offset(cx, cy)) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                palette.primaryGlow.copy(alpha = 0.9f),
                                palette.highlightPop.copy(alpha = 0.65f),
                                Color.Transparent,
                                palette.secondaryGlow.copy(alpha = 0.85f),
                                palette.primaryGlow.copy(alpha = 0.9f)
                            )
                        ),
                        radius = radius * 1.04f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 3.2.dp.toPx())
                    )
                }

                // 3. Tilted Interstellar Elliptical Orbit Trajectories
                rotate(degrees = -22f, pivot = Offset(cx, cy)) {
                    val rx = radius * 1.16f
                    val ry = radius * 0.56f
                    drawOval(
                        color = palette.primaryGlow.copy(alpha = 0.34f),
                        topLeft = Offset(cx - rx, cy - ry),
                        size = Size(rx * 2f, ry * 2f),
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                    val satRad = Math.toRadians(orbitAngle.toDouble())
                    val sx = cx + (rx * cos(satRad)).toFloat()
                    val sy = cy + (ry * sin(satRad)).toFloat()
                    drawCircle(
                        color = palette.highlightPop,
                        radius = 3.5.dp.toPx(),
                        center = Offset(sx, sy)
                    )
                }

                rotate(degrees = 26f, pivot = Offset(cx, cy)) {
                    val rx = radius * 1.12f
                    val ry = radius * 0.44f
                    drawOval(
                        color = palette.secondaryGlow.copy(alpha = 0.26f),
                        topLeft = Offset(cx - rx, cy - ry),
                        size = Size(rx * 2f, ry * 2f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // 4. Inner Atmospheric Horizon Rim
                drawCircle(
                    color = InkCream.copy(alpha = 0.18f),
                    radius = radius * 0.86f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.dp.toPx())
                )

                // 5. 60 Aerospace Telemetry Ticks
                for (i in 0 until 60) {
                    val angleRad = ((i * 6f) - 90f) * (PI / 180.0)
                    val isMajor = i % 5 == 0
                    val isQuarter = i % 15 == 0
                    val isCurrentSecond = showSeconds && i == seconds

                    val innerR = when {
                        isQuarter -> radius * 0.77f
                        isMajor -> radius * 0.82f
                        else -> radius * 0.88f
                    }
                    val outerR = if (isCurrentSecond && beatPulseEnabled) {
                        radius * 0.96f
                    } else {
                        radius * 0.92f
                    }

                    val start = Offset(
                        cx + (innerR * cos(angleRad)).toFloat(),
                        cy + (innerR * sin(angleRad)).toFloat()
                    )
                    val end = Offset(
                        cx + (outerR * cos(angleRad)).toFloat(),
                        cy + (outerR * sin(angleRad)).toFloat()
                    )

                    val tickColor = when {
                        isCurrentSecond -> palette.highlightPop
                        isQuarter -> palette.primaryGlow
                        isMajor -> InkCream.copy(alpha = 0.85f)
                        else -> InkCream.copy(alpha = 0.24f)
                    }
                    val tickWidth = when {
                        isCurrentSecond -> 3.dp.toPx()
                        isQuarter -> 2.6.dp.toPx()
                        isMajor -> 1.8.dp.toPx()
                        else -> 1.dp.toPx()
                    }

                    drawLine(
                        color = tickColor,
                        start = start,
                        end = end,
                        strokeWidth = tickWidth,
                        cap = StrokeCap.Round
                    )
                }

                // 6. Luminous Interstellar Hands
                val exactSeconds = seconds + (millisInSecond / 1000f)
                val minuteAngle = (minutes + exactSeconds / 60f) * 6f
                val hourAngle = ((hours % 12) + minutes / 60f) * 30f
                val secondAngle = exactSeconds * 6f

                // Hour hand
                rotate(degrees = hourAngle, pivot = Offset(cx, cy)) {
                    drawLine(
                        color = palette.primaryGlow,
                        start = Offset(cx, cy + radius * 0.08f),
                        end = Offset(cx, cy - radius * 0.48f),
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = InkCream,
                        start = Offset(cx, cy - radius * 0.06f),
                        end = Offset(cx, cy - radius * 0.44f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Minute hand
                rotate(degrees = minuteAngle, pivot = Offset(cx, cy)) {
                    drawLine(
                        color = palette.secondaryGlow,
                        start = Offset(cx, cy + radius * 0.10f),
                        end = Offset(cx, cy - radius * 0.70f),
                        strokeWidth = 4.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = InkCream,
                        start = Offset(cx, cy - radius * 0.08f),
                        end = Offset(cx, cy - radius * 0.66f),
                        strokeWidth = 1.6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Second sweep
                if (showSeconds) {
                    rotate(degrees = secondAngle, pivot = Offset(cx, cy)) {
                        drawLine(
                            color = palette.highlightPop,
                            start = Offset(cx, cy + radius * 0.18f),
                            end = Offset(cx, cy - radius * 0.79f),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawCircle(
                            color = palette.highlightPop,
                            radius = 4.dp.toPx(),
                            center = Offset(cx, cy + radius * 0.18f),
                            style = Stroke(width = 1.6.dp.toPx())
                        )
                    }
                }

                // Singularity Center Core
                drawCircle(
                    color = StageVoid,
                    radius = 7.dp.toPx(),
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = palette.primaryGlow,
                    radius = 3.8.dp.toPx(),
                    center = Offset(cx, cy)
                )
            }

            // Chamfered Interstellar Digital Telemetry Pill
            val pillShape = remember { ChamferedHudShape(8.dp) }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 26.dp)
                    .clip(pillShape)
                    .background(StageVoid.copy(alpha = 0.86f))
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(palette.primaryGlow, palette.secondaryGlow)
                        ),
                        shape = pillShape
                    )
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = mainTimeText,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = InkCream
                )
                if (showSeconds) {
                    Text(
                        text = ":$secondsText",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = palette.highlightPop
                    )
                }
                if (amPmText.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = amPmText,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.secondaryGlow
                    )
                }
            }
        }
    }
}
