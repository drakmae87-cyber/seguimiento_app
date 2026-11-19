package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.ChamferedHudShape
import com.example.ui.components.CurvedSciFiHudBanner
import com.example.ui.theme.BioEmerald
import com.example.ui.theme.InkCream
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalNekoPalette
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.StageBorder
import com.example.ui.theme.StageCard
import com.example.ui.theme.StageVoid
import com.example.viewmodel.ClockFormatters
import com.example.viewmodel.StopwatchUiState

@Composable
fun StopwatchScreen(
    state: StopwatchUiState,
    onToggleStartPause: () -> Unit,
    onRecordLap: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNekoPalette.current
    val hudShape = remember { ChamferedHudShape(14.dp) }
    val chipShape = remember { ChamferedHudShape(7.dp) }

    val fastestSplit = remember(state.laps) {
        if (state.laps.size >= 2) state.laps.minOf { it.splitMillis } else null
    }
    val slowestSplit = remember(state.laps) {
        if (state.laps.size >= 2) state.laps.maxOf { it.splitMillis } else null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.stopwatch_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = InkCream
                )
                Surface(
                    shape = chipShape,
                    color = if (state.isRunning) BioEmerald.copy(alpha = 0.22f) else StageCard.copy(alpha = 0.85f),
                    border = BorderStroke(
                        1.dp,
                        if (state.isRunning) BioEmerald else StageBorder
                    )
                ) {
                    Text(
                        text = if (state.isRunning) "REC ●" else "READY",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.isRunning) BioEmerald else InkMuted,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            CurvedSciFiHudBanner(
                primaryText = "TELEMETRÍA DE PRECISIÓN",
                secondaryText = "${state.laps.size} VUELTAS REGISTRADAS"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Chamfered Readout HUD Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(hudShape)
                    .background(
                        Brush.linearGradient(
                            listOf(StageCard.copy(alpha = 0.86f), palette.stageBackdropTop.copy(alpha = 0.82f))
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.horizontalGradient(
                            listOf(palette.primaryGlow, palette.secondaryGlow)
                        ),
                        shape = hudShape
                    )
                    .padding(vertical = 28.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ClockFormatters.formatStopwatch(state.elapsedMillis),
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 42.sp,
                    color = InkCream,
                    modifier = Modifier.testTag("stopwatch_display_text")
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = if (state.isRunning) onRecordLap else onReset,
                    enabled = state.elapsedMillis > 0L,
                    shape = hudShape,
                    border = BorderStroke(1.dp, StageBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = InkCream),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag(if (state.isRunning) "stopwatch_lap_button" else "stopwatch_reset_button")
                ) {
                    Icon(
                        imageVector = if (state.isRunning) Icons.Filled.Flag else Icons.Filled.Refresh,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.isRunning) stringResource(R.string.stopwatch_lap)
                        else stringResource(R.string.timer_reset)
                    )
                }

                Button(
                    onClick = onToggleStartPause,
                    shape = hudShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isRunning) palette.secondaryGlow else palette.primaryGlow,
                        contentColor = StageVoid
                    ),
                    modifier = Modifier
                        .weight(1.35f)
                        .height(54.dp)
                        .testTag("stopwatch_start_pause_button")
                ) {
                    Icon(
                        imageVector = if (state.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.isRunning) stringResource(R.string.timer_pause)
                        else stringResource(R.string.timer_start),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.laps.isEmpty()) {
                Card(
                    shape = hudShape,
                    colors = CardDefaults.cardColors(containerColor = StageCard.copy(alpha = 0.78f)),
                    border = BorderStroke(1.dp, StageBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = null,
                            tint = palette.secondaryGlow,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.stopwatch_laps_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(state.laps, key = { it.number }) { lap ->
                        val isFastest = fastestSplit != null && lap.splitMillis == fastestSplit
                        val isSlowest = slowestSplit != null && lap.splitMillis == slowestSplit && !isFastest
                        val badgeColor = when {
                            isFastest -> BioEmerald
                            isSlowest -> palette.primaryGlow
                            else -> null
                        }

                        Surface(
                            shape = hudShape,
                            color = StageCard.copy(alpha = 0.82f),
                            border = BorderStroke(
                                1.dp,
                                badgeColor?.copy(alpha = 0.65f) ?: StageBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("stopwatch_lap_item_${lap.number}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "#${lap.number.toString().padStart(2, '0')}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor ?: palette.secondaryGlow
                                    )
                                    if (isFastest || isSlowest) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isFastest) stringResource(R.string.stopwatch_fastest)
                                            else stringResource(R.string.stopwatch_slowest),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = badgeColor ?: InkMuted
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "+${ClockFormatters.formatStopwatch(lap.splitMillis)}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = InkCream
                                    )
                                    Text(
                                        text = ClockFormatters.formatStopwatch(lap.totalMillis),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InkMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
