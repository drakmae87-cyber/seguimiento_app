package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ClockPreferences
import com.example.ui.components.ChamferedHudShape
import com.example.ui.components.CurvedSciFiHudBanner
import com.example.ui.components.EyeModuleOverlay
import com.example.ui.theme.InkCream
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalNekoPalette
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.StageBorder
import com.example.ui.theme.StageCard
import com.example.ui.theme.StageVoid
import com.example.ui.theme.resolveEyeColor
import com.example.viewmodel.ClockFormatters
import com.example.viewmodel.TimerUiState

private val TIMER_PRESETS = listOf(1, 5, 15, 25, 45)

@Composable
fun TimerScreen(
    state: TimerUiState,
    preferences: ClockPreferences,
    onSelectPreset: (Int) -> Unit,
    onAddOneMinute: () -> Unit,
    onToggleStartPause: () -> Unit,
    onReset: () -> Unit,
    onDismissFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNekoPalette.current
    val context = LocalContext.current
    val hudShape = remember { ChamferedHudShape(12.dp) }
    val eyeColor = remember(preferences.eyeColorPreset, preferences.customEyeColorHex) {
        resolveEyeColor(preferences.eyeColorPreset, preferences.customEyeColorHex)
    }

    LaunchedEffect(state.isFinished) {
        if (state.isFinished) {
            triggerTimerCompletionVibration(context)
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = state.progress,
        animationSpec = tween(durationMillis = 120),
        label = "timer_progress"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 600.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.timer_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = InkCream
                )
                FilledTonalButton(
                    onClick = onAddOneMinute,
                    shape = hudShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = StageCard.copy(alpha = 0.85f),
                        contentColor = palette.secondaryGlow
                    ),
                    modifier = Modifier.testTag("timer_add_minute_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.timer_add_minute))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            CurvedSciFiHudBanner(
                primaryText = "MODO FOCUS · ${state.selectedPresetMinutes} MIN",
                secondaryText = if (state.isRunning) "CUENTA REGRESIVA ACTIVA" else "LISTO PARA SINCRONIZAR"
            )

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(visible = state.isFinished) {
                Card(
                    shape = hudShape,
                    colors = CardDefaults.cardColors(
                        containerColor = palette.primaryGlow.copy(alpha = 0.24f)
                    ),
                    border = BorderStroke(1.5.dp, palette.primaryGlow),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .testTag("timer_finished_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = eyeColor,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.timer_finished_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = InkCream
                                )
                                Text(
                                    text = stringResource(R.string.timer_finished_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkMuted
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onDismissFinished,
                            shape = hudShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = eyeColor,
                                contentColor = StageVoid
                            ),
                            modifier = Modifier.testTag("timer_dismiss_button")
                        ) {
                            Text(stringResource(R.string.timer_dismiss))
                        }
                    }
                }
            }

            // Orbital Countdown Ring + Eye Module
            Box(
                modifier = Modifier.size(258.dp),
                contentAlignment = Alignment.Center
            ) {
                if (preferences.biolumEyesOverlayEnabled) {
                    EyeModuleOverlay(
                        preferences = preferences,
                        eyeColor = eyeColor,
                        secondaryGlow = palette.secondaryGlow,
                        beatPulse = if (state.isRunning) 0.55f else 0.2f,
                        eyeScale = 1.0f,
                        collageProportionalMode = false,
                        modifier = Modifier
                            .size(164.dp)
                            .padding(bottom = 58.dp)
                    )
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 11.dp.toPx()
                    val pad = strokeWidth / 2f + 6.dp.toPx()
                    val arcSize = Size(size.width - pad * 2, size.height - pad * 2)
                    val topLeft = Offset(pad, pad)

                    drawArc(
                        color = StageCard.copy(alpha = 0.75f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                palette.primaryGlow,
                                palette.secondaryGlow,
                                eyeColor,
                                palette.primaryGlow
                            )
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 26.dp)
                ) {
                    Text(
                        text = ClockFormatters.formatTimerCountdown(state.remainingMillis),
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 40.sp,
                        color = InkCream,
                        modifier = Modifier.testTag("timer_countdown_text")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${state.selectedPresetMinutes} MIN · FOCUS",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.secondaryGlow
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.timer_presets_title),
                style = MaterialTheme.typography.labelLarge,
                color = InkMuted,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            val chipShape = remember { ChamferedHudShape(7.dp) }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(TIMER_PRESETS) { mins ->
                    val selected = state.selectedPresetMinutes == mins && !state.isRunning
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectPreset(mins) },
                        shape = chipShape,
                        label = {
                            Text(
                                text = "${mins}m",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.primaryGlow,
                            selectedLabelColor = StageVoid,
                            containerColor = StageCard.copy(alpha = 0.82f),
                            labelColor = InkCream
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = StageBorder,
                            selectedBorderColor = palette.primaryGlow
                        ),
                        modifier = Modifier.testTag("timer_preset_${mins}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onReset,
                    shape = hudShape,
                    border = BorderStroke(1.dp, StageBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = InkCream),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("timer_reset_button")
                ) {
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.timer_reset))
                }

                Button(
                    onClick = onToggleStartPause,
                    shape = hudShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isRunning) palette.secondaryGlow else palette.primaryGlow,
                        contentColor = StageVoid
                    ),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(54.dp)
                        .testTag("timer_start_pause_button")
                ) {
                    Icon(
                        imageVector = if (state.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            state.isRunning -> stringResource(R.string.timer_pause)
                            state.remainingMillis < state.totalMillis && state.remainingMillis > 0L ->
                                stringResource(R.string.timer_resume)
                            else -> stringResource(R.string.timer_start)
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

private fun triggerTimerCompletionVibration(context: Context) {
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return

        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0L, 180L, 120L, 220L), -1)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0L, 180L, 120L, 220L), -1)
        }
    } catch (_: SecurityException) {
        // Ignore gracefully if device restricts vibration
    }
}
