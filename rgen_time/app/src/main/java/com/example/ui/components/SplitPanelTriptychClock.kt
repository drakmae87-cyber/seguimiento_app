package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.CollageImageSlicer
import com.example.data.CustomMediaHelper
import com.example.model.ClockPreferences
import com.example.model.DialStyle
import com.example.model.MediaFitMode
import com.example.model.MediaTransform
import com.example.ui.theme.InkCream
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalNekoPalette
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.StageCard
import com.example.ui.theme.StageVoid
import com.example.ui.theme.resolveEyeColor
import com.example.viewmodel.ClockFormatters
import java.io.File
import java.util.Calendar
import kotlin.math.roundToInt

@Composable
fun SplitPanelCatClock(
    nowMillis: Long,
    preferences: ClockPreferences,
    onClockClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNekoPalette.current
    val calendar = remember(nowMillis) {
        Calendar.getInstance().apply { timeInMillis = nowMillis }
    }
    val seconds = calendar.get(Calendar.SECOND)
    val millisInSecond = calendar.get(Calendar.MILLISECOND)

    val beatPulse = if (preferences.beatPulseEnabled) {
        (1f - millisInSecond / 1000f).coerceIn(0f, 1f)
    } else {
        0.35f
    }

    val eyeScale by animateFloatAsState(
        targetValue = (1f + beatPulse * 0.025f) * preferences.eyeCanvasScale,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "biolum_eye_pulse"
    )

    val eyeColor = remember(preferences.eyeColorPreset, preferences.customEyeColorHex) {
        resolveEyeColor(preferences.eyeColorPreset, preferences.customEyeColorHex)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClockClick
            )
            .testTag("cat_clock_dial"),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = preferences.dialStyle,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "clock_architecture_switch"
        ) { currentStyle ->
            when (currentStyle) {
                DialStyle.COLLAGER4 -> {
                    Collager4Content(
                        nowMillis = nowMillis,
                        preferences = preferences,
                        eyeColor = eyeColor,
                        seconds = seconds,
                        beatPulse = beatPulse,
                        eyeScale = eyeScale
                    )
                }

                DialStyle.PLASMA -> {
                    PlasmaInterstellarClock(
                        nowMillis = nowMillis,
                        customCoreArtPath = preferences.customPanelArtPath,
                        panelTransform = preferences.panelTransform,
                        panelAlpha = preferences.panelAlpha,
                        use24Hour = preferences.use24Hour,
                        showSeconds = preferences.showSeconds,
                        beatPulseEnabled = preferences.beatPulseEnabled,
                        onDialClick = onClockClick
                    )
                }

                DialStyle.HUD_4 -> {
                    Hud4DigitalStage(
                        nowMillis = nowMillis,
                        preferences = preferences,
                        eyeColor = eyeColor,
                        beatPulse = beatPulse,
                        eyeScale = eyeScale,
                        accentPrimary = palette.primaryGlow,
                        accentSecondary = palette.secondaryGlow
                    )
                }
            }
        }
    }
}

@Composable
fun EyeModuleOverlay(
    preferences: ClockPreferences,
    eyeColor: Color,
    secondaryGlow: Color,
    beatPulse: Float,
    eyeScale: Float,
    collageProportionalMode: Boolean,
    modifier: Modifier = Modifier
) {
    if (!preferences.biolumEyesOverlayEnabled) return

    val customPath = preferences.customEyeMediaPath
    if (!customPath.isNullOrBlank()) {
        UniversalMediaLayer(
            mediaPath = customPath,
            transform = preferences.eyeMediaTransform.copy(scale = eyeScale),
            alpha = preferences.customEyeMediaAlpha,
            contentDescription = stringResource(R.string.cd_eye_art),
            modifier = modifier
        )
    } else {
        BiolumCatEyesCanvas(
            eyeColor = eyeColor,
            secondaryGlow = secondaryGlow,
            glowIntensity = preferences.eyeGlowIntensity,
            showWhiskers = preferences.eyeWhiskersEnabled,
            beatPulse = beatPulse,
            contentDesc = stringResource(R.string.cd_eye_art),
            overlayStyle = preferences.canvasOverlayStyle,
            pupilStyle = preferences.irisPupilStyle,
            showBrows = preferences.eyeBrowsEnabled,
            blinkEnabled = preferences.eyeBlinkEnabled,
            verticalOffsetFrac = preferences.eyeVerticalOffset,
            horizontalOffsetFrac = preferences.eyeHorizontalOffset,
            collageProportionalMode = collageProportionalMode,
            modifier = modifier.scale(eyeScale)
        )
    }
}

@Composable
private fun Collager4Content(
    nowMillis: Long,
    preferences: ClockPreferences,
    eyeColor: Color,
    seconds: Int,
    beatPulse: Float,
    eyeScale: Float
) {
    val palette = LocalNekoPalette.current

    val dayText = ClockFormatters.formatDayShortEs(nowMillis)
    val amPmOr24 = if (preferences.use24Hour) "24H" else ClockFormatters.formatAmPm(nowMillis)
    val hoursText = ClockFormatters.formatHourTwoDigits(nowMillis, preferences.use24Hour)
    val minutesText = ClockFormatters.formatMinuteTwoDigits(nowMillis)
    val secondsText = if (preferences.showSeconds) ClockFormatters.formatSeconds(nowMillis) else "--"
    val dayNumberText = ClockFormatters.formatDayNumber(nowMillis)
    val monthText = ClockFormatters.formatMonthShortEs(nowMillis)
    val yearText = "'${ClockFormatters.formatYearShort(nowMillis)}"

    val slicePaths = preferences.customPanelSlices
    val sliceRegions = remember { CollageImageSlicer.computePanelRegions() }
    val hasCustomTransform = preferences.panelScale != 1.0f ||
        preferences.panelOffsetX != 0.0f ||
        preferences.panelOffsetY != 0.0f ||
        preferences.panelFitMode != MediaFitMode.CROP

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(312.dp),
        contentAlignment = Alignment.Center
    ) {
        val totalHeight = 304.dp

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TriptychFramePanel(
                panelIndex = 0,
                heightFraction = 0.64f,
                slicePath = if (hasCustomTransform) null else slicePaths.getOrNull(0),
                fallbackFullMediaPath = preferences.customPanelArtPath,
                panelTransform = preferences.panelTransform,
                panelAlpha = preferences.panelAlpha,
                sliceRegion = sliceRegions[0],
                accentColor = palette.secondaryGlow,
                isCenterHero = false,
                label = preferences.panelLabelDay.ifBlank { stringResource(R.string.layer_day) },
                primaryValue = dayText,
                secondaryValue = amPmOr24,
                modifier = Modifier
                    .weight(0.88f)
                    .testTag("triptych_panel_day")
            )

            TriptychFramePanel(
                panelIndex = 1,
                heightFraction = 0.82f,
                slicePath = if (hasCustomTransform) null else slicePaths.getOrNull(1),
                fallbackFullMediaPath = preferences.customPanelArtPath,
                panelTransform = preferences.panelTransform,
                panelAlpha = preferences.panelAlpha,
                sliceRegion = sliceRegions[1],
                accentColor = palette.primaryGlow,
                isCenterHero = false,
                label = preferences.panelLabelHours.ifBlank { stringResource(R.string.layer_hours) },
                primaryValue = hoursText,
                secondaryValue = "HR",
                modifier = Modifier
                    .weight(1.06f)
                    .testTag("triptych_panel_hours")
            )

            TriptychFramePanel(
                panelIndex = 2,
                heightFraction = 1.00f,
                slicePath = if (hasCustomTransform) null else slicePaths.getOrNull(2),
                fallbackFullMediaPath = preferences.customPanelArtPath,
                panelTransform = preferences.panelTransform,
                panelAlpha = preferences.panelAlpha,
                sliceRegion = sliceRegions[2],
                accentColor = eyeColor,
                isCenterHero = true,
                label = preferences.panelLabelMinutes.ifBlank { stringResource(R.string.layer_minutes) },
                primaryValue = minutesText,
                secondaryValue = "SYNC",
                beatPulse = beatPulse,
                modifier = Modifier
                    .weight(1.22f)
                    .testTag("triptych_panel_minutes")
            )

            TriptychFramePanel(
                panelIndex = 3,
                heightFraction = 0.82f,
                slicePath = if (hasCustomTransform) null else slicePaths.getOrNull(3),
                fallbackFullMediaPath = preferences.customPanelArtPath,
                panelTransform = preferences.panelTransform,
                panelAlpha = preferences.panelAlpha,
                sliceRegion = sliceRegions[3],
                accentColor = palette.highlightPop,
                isCenterHero = false,
                label = preferences.panelLabelSeconds.ifBlank { stringResource(R.string.layer_seconds) },
                primaryValue = secondsText,
                secondaryValue = if (preferences.showSeconds) "${((seconds / 60f) * 100).toInt()}%" else "OFF",
                verticalProgress = if (preferences.showSeconds) (seconds + 1) / 60f else 0f,
                modifier = Modifier
                    .weight(1.06f)
                    .testTag("triptych_panel_seconds")
            )

            TriptychFramePanel(
                panelIndex = 4,
                heightFraction = 0.64f,
                slicePath = if (hasCustomTransform) null else slicePaths.getOrNull(4),
                fallbackFullMediaPath = preferences.customPanelArtPath,
                panelTransform = preferences.panelTransform,
                panelAlpha = preferences.panelAlpha,
                sliceRegion = sliceRegions[4],
                accentColor = palette.secondaryGlow,
                isCenterHero = false,
                label = preferences.panelLabelDate.ifBlank { stringResource(R.string.layer_date_month) },
                primaryValue = dayNumberText,
                secondaryValue = "$monthText $yearText",
                modifier = Modifier
                    .weight(0.88f)
                    .testTag("triptych_panel_date")
            )
        }

        EyeModuleOverlay(
            preferences = preferences,
            eyeColor = eyeColor,
            secondaryGlow = palette.secondaryGlow,
            beatPulse = beatPulse,
            eyeScale = eyeScale,
            collageProportionalMode = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
        )
    }
}

@Composable
private fun TriptychFramePanel(
    panelIndex: Int,
    heightFraction: Float,
    slicePath: String?,
    fallbackFullMediaPath: String?,
    panelTransform: MediaTransform,
    panelAlpha: Float,
    sliceRegion: CollageImageSlicer.PanelSliceRegion,
    accentColor: Color,
    isCenterHero: Boolean,
    label: String,
    primaryValue: String,
    secondaryValue: String,
    beatPulse: Float = 0.3f,
    verticalProgress: Float? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalNekoPalette.current
    val context = LocalContext.current
    val imageLoader = remember(context) { CustomMediaHelper.createMediaImageLoader(context) }
    val frameShape = remember { RoundedCornerShape(8.dp) }

    Box(
        modifier = modifier
            .fillMaxHeight(heightFraction)
            .clip(frameShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        palette.stageBackdropTop.copy(alpha = 0.78f),
                        StageCard.copy(alpha = 0.85f),
                        StageVoid.copy(alpha = 0.94f)
                    )
                )
            )
            .drawBehind {
                drawLine(
                    color = accentColor.copy(alpha = if (isCenterHero) 0.9f else 0.55f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = accentColor.copy(alpha = 0.45f),
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawLine(
                    color = Color.Black.copy(alpha = 0.85f),
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, size.height),
                    strokeWidth = 2.5.dp.toPx()
                )
            }
            .border(
                width = if (isCenterHero) 1.5.dp else 0.8.dp,
                color = accentColor.copy(alpha = if (isCenterHero) 0.75f else 0.35f),
                shape = frameShape
            )
    ) {
        val effectiveAlpha = (panelAlpha + if (isCenterHero) 0.08f else 0f).coerceIn(0.15f, 1.0f)
        if (!slicePath.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(slicePath))
                    .crossfade(true)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(effectiveAlpha)
                    .testTag("collager4_slice_$panelIndex")
            )
        } else if (!fallbackFullMediaPath.isNullOrBlank()) {
            SlicedViewportMedia(
                mediaPath = fallbackFullMediaPath,
                sliceRegion = sliceRegion,
                transform = panelTransform,
                alpha = effectiveAlpha,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (!slicePath.isNullOrBlank() || !fallbackFullMediaPath.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                StageVoid.copy(alpha = 0.16f),
                                StageVoid.copy(alpha = 0.54f),
                                StageVoid.copy(alpha = 0.92f)
                            )
                        )
                    )
            )
        }

        if (verticalProgress != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 3.dp, bottom = 6.dp, top = 6.dp)
                    .width(3.dp)
                    .fillMaxHeight(verticalProgress.coerceIn(0.05f, 1f))
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 3.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.5.sp,
                color = accentColor,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = primaryValue,
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = if (isCenterHero) 30.sp else if (primaryValue.length <= 2) 23.sp else 17.sp,
                color = InkCream,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (isCenterHero) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accentColor.copy(alpha = 0.45f + beatPulse * 0.55f))
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                text = secondaryValue,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 9.sp,
                color = InkMuted,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SlicedViewportMedia(
    mediaPath: String,
    sliceRegion: CollageImageSlicer.PanelSliceRegion,
    transform: MediaTransform,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    Layout(
        modifier = modifier,
        content = {
            UniversalMediaLayer(
                mediaPath = mediaPath,
                transform = transform,
                alpha = alpha,
                modifier = Modifier.fillMaxSize()
            )
        }
    ) { measurables, constraints ->
        val panelW = constraints.maxWidth.coerceAtLeast(1)
        val panelH = constraints.maxHeight.coerceAtLeast(1)
        val totalW = (panelW / sliceRegion.widthFrac).roundToInt().coerceAtLeast(panelW)
        val totalH = (panelH / sliceRegion.heightFrac).roundToInt().coerceAtLeast(panelH)
        val offsetX = -(sliceRegion.xStartFrac * totalW).roundToInt()
        val offsetY = -(sliceRegion.yStartFrac * totalH).roundToInt()

        val placeable = measurables.first().measure(Constraints.fixed(totalW, totalH))
        layout(panelW, panelH) {
            placeable.placeRelative(offsetX, offsetY)
        }
    }
}

@Composable
private fun Hud4DigitalStage(
    nowMillis: Long,
    preferences: ClockPreferences,
    eyeColor: Color,
    beatPulse: Float,
    eyeScale: Float,
    accentPrimary: Color,
    accentSecondary: Color
) {
    val hudShape = remember { ChamferedHudShape(18.dp) }
    val mainTime = ClockFormatters.formatMainTime(nowMillis, preferences.use24Hour)
    val secondsText = ClockFormatters.formatSeconds(nowMillis)
    val amPm = if (!preferences.use24Hour) ClockFormatters.formatAmPm(nowMillis) else ""

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .height(186.dp)
                .clip(hudShape)
                .background(
                    Brush.verticalGradient(
                        listOf(StageCard.copy(alpha = 0.72f), StageVoid.copy(alpha = 0.90f))
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(listOf(accentPrimary, accentSecondary)),
                    shape = hudShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!preferences.customPanelArtPath.isNullOrBlank()) {
                UniversalMediaLayer(
                    mediaPath = preferences.customPanelArtPath,
                    transform = preferences.panelTransform,
                    alpha = preferences.panelAlpha,
                    modifier = Modifier.fillMaxSize()
                )
            }
            EyeModuleOverlay(
                preferences = preferences,
                eyeColor = eyeColor,
                secondaryGlow = accentSecondary,
                beatPulse = beatPulse,
                eyeScale = eyeScale,
                collageProportionalMode = false,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = mainTime,
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 54.sp,
                color = InkCream
            )
            if (preferences.showSeconds || amPm.isNotEmpty()) {
                Column(modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)) {
                    if (amPm.isNotEmpty()) {
                        Text(
                            text = amPm,
                            style = MaterialTheme.typography.labelLarge,
                            color = accentSecondary
                        )
                    }
                    if (preferences.showSeconds) {
                        Text(
                            text = ":$secondsText",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = eyeColor
                        )
                    }
                }
            }
        }
    }
}
