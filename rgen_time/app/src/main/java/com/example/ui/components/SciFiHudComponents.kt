package com.example.ui.components

import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.ui.theme.InkCream
import com.example.ui.theme.InkMuted
import com.example.ui.theme.LocalNekoPalette
import com.example.ui.theme.StageCard
import com.example.ui.theme.StageVoid

class ChamferedHudShape(private val cutSize: Dp = 10.dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cut = with(density) { cutSize.toPx() }.coerceAtMost(minOf(size.width, size.height) * 0.35f)
        val path = Path().apply {
            moveTo(cut, 0f)
            lineTo(size.width - cut, 0f)
            lineTo(size.width, cut)
            lineTo(size.width, size.height - cut)
            lineTo(size.width - cut, size.height)
            lineTo(cut, size.height)
            lineTo(0f, size.height - cut)
            lineTo(0f, cut)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun SciFiTopHudBar(
    envCode: String,
    skinCode: String,
    onCycleEnvironment: () -> Unit,
    onEnterDeskMode: () -> Unit,
    modifier: Modifier = Modifier,
    brandTag: String = "RGEN"
) {
    val palette = LocalNekoPalette.current
    val hudShape = remember { ChamferedHudShape(9.dp) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Chamfered Top-Left HUD Navigation Strip
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(hudShape)
                .background(StageCard.copy(alpha = 0.78f))
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(palette.primaryGlow.copy(alpha = 0.75f), palette.secondaryGlow.copy(alpha = 0.45f))
                    ),
                    shape = hudShape
                )
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = palette.secondaryGlow,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = brandTag.ifBlank { "RGEN" },
                    style = MaterialTheme.typography.labelLarge,
                    color = InkCream
                )
                Text(
                    text = " | ",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.primaryGlow
                )
                Text(
                    text = envCode,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.secondaryGlow,
                    maxLines = 1
                )
            }

            Text(
                text = skinCode,
                style = MaterialTheme.typography.labelSmall,
                color = InkMuted,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Quick 3D Environment Switcher HUD Tile
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(hudShape)
                .background(StageCard.copy(alpha = 0.85f))
                .border(1.dp, palette.secondaryGlow.copy(alpha = 0.6f), hudShape)
                .clickable(onClick = onCycleEnvironment)
                .padding(10.dp)
                .testTag("cycle_env_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Layers,
                contentDescription = stringResource(R.string.studio_theme_header),
                tint = palette.secondaryGlow,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Desk Mode HUD Tile
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(hudShape)
                .background(StageCard.copy(alpha = 0.85f))
                .border(1.dp, palette.primaryGlow.copy(alpha = 0.65f), hudShape)
                .clickable(onClick = onEnterDeskMode)
                .padding(10.dp)
                .testTag("desk_mode_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Fullscreen,
                contentDescription = stringResource(R.string.cd_desk_mode),
                tint = palette.primaryGlow,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun CurvedSciFiHudBanner(
    primaryText: String,
    secondaryText: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalNekoPalette.current
    val context = LocalContext.current
    val orbitronTypeface = remember(context) {
        ResourcesCompat.getFont(context, R.font.orbitron)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(62.dp)
    ) {
        val w = size.width
        val h = size.height
        val padX = 14.dp.toPx()
        val archRise = 13.dp.toPx()

        // Curved translucent HUD ribbon polygon
        val ribbonPath = Path().apply {
            moveTo(padX, h * 0.34f)
            quadraticTo(w * 0.5f, h * 0.34f - archRise * 1.45f, w - padX, h * 0.34f)
            lineTo(w - padX * 1.35f, h * 0.86f)
            quadraticTo(w * 0.5f, h * 0.86f - archRise * 1.35f, padX * 1.35f, h * 0.86f)
            close()
        }

        drawPath(
            path = ribbonPath,
            brush = Brush.horizontalGradient(
                listOf(
                    StageVoid.copy(alpha = 0.58f),
                    StageCard.copy(alpha = 0.82f),
                    StageVoid.copy(alpha = 0.58f)
                )
            )
        )

        // Upper and lower neon HUD arc edges
        val topEdge = Path().apply {
            moveTo(padX, h * 0.34f)
            quadraticTo(w * 0.5f, h * 0.34f - archRise * 1.45f, w - padX, h * 0.34f)
        }
        val bottomEdge = Path().apply {
            moveTo(padX * 1.35f, h * 0.86f)
            quadraticTo(w * 0.5f, h * 0.86f - archRise * 1.35f, w - padX * 1.35f, h * 0.86f)
        }

        drawPath(
            path = topEdge,
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, palette.primaryGlow, palette.secondaryGlow, Color.Transparent)
            ),
            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
        )
        drawPath(
            path = bottomEdge,
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, palette.secondaryGlow.copy(alpha = 0.7f), Color.Transparent)
            ),
            style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw curved HUD text along an arc path using nativeCanvas.drawTextOnPath
        val primaryArcPath = AndroidPath().apply {
            moveTo(padX * 1.4f, h * 0.54f)
            quadTo(w * 0.5f, h * 0.54f - archRise * 1.4f, w - padX * 1.4f, h * 0.54f)
        }
        val secondaryArcPath = AndroidPath().apply {
            moveTo(padX * 1.8f, h * 0.77f)
            quadTo(w * 0.5f, h * 0.77f - archRise * 1.35f, w - padX * 1.8f, h * 0.77f)
        }

        val primaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = InkCream.toArgb()
            textSize = 11.5.dp.toPx()
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.14f
            typeface = orbitronTypeface
        }
        val secondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.secondaryGlow.toArgb()
            textSize = 9.dp.toPx()
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.16f
            typeface = orbitronTypeface
        }

        drawContext.canvas.nativeCanvas.drawTextOnPath(primaryText, primaryArcPath, 0f, 0f, primaryPaint)
        drawContext.canvas.nativeCanvas.drawTextOnPath(secondaryText, secondaryArcPath, 0f, 0f, secondaryPaint)
    }
}
