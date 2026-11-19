package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TelemetryPushCategory
import com.example.model.TelemetryPushMessage
import com.example.ui.theme.InkCream
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalNekoPalette
import com.example.ui.theme.StageBorder
import com.example.ui.theme.StageCardElevated
import com.example.ui.theme.StageVoid
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TelemetryPushBanner(
    latestLog: TelemetryPushMessage?,
    historyCount: Int,
    onDismiss: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNekoPalette.current
    val shape = remember { ChamferedHudShape(12.dp) }

    AnimatedVisibility(
        visible = latestLog != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        if (latestLog != null) {
            val accent = categoryAccent(latestLog.category, palette.primaryGlow, palette.secondaryGlow)
            Surface(
                onClick = onOpenHistory,
                shape = shape,
                color = StageCardElevated.copy(alpha = 0.96f),
                border = BorderStroke(1.2.dp, accent.copy(alpha = 0.85f)),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .testTag("telemetry_push_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(ChamferedHudShape(8.dp))
                            .background(accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon(latestLog.category),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = ChamferedHudShape(4.dp),
                                color = accent.copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = "PUSH LOG // ${latestLog.category.tag}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = accent,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                            if (!latestLog.countdownHhMmSs.isNullOrBlank()) {
                                Surface(
                                    shape = ChamferedHudShape(4.dp),
                                    color = palette.primaryGlow.copy(alpha = 0.24f),
                                    border = BorderStroke(0.6.dp, palette.primaryGlow)
                                ) {
                                    Text(
                                        text = latestLog.countdownHhMmSs,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        color = palette.primaryGlow,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = latestLog.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = InkCream,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = latestLog.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = InkMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (historyCount > 1) {
                        IconButton(
                            onClick = onOpenHistory,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = "Historial de telemetría",
                                tint = palette.secondaryGlow,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("dismiss_push_banner")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cerrar aviso",
                            tint = InkMuted,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PushLogHistoryDialog(
    history: List<TelemetryPushMessage>,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalNekoPalette.current
    val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StageCardElevated,
        titleContentColor = InkCream,
        textContentColor = InkCream,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Terminal, contentDescription = null, tint = palette.primaryGlow)
                Text("REGISTRO PUSH DE TELEMETRÍA", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            if (history.isEmpty()) {
                Text(
                    text = "Sin eventos registrados en la sesión actual.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(history, key = { it.id }) { item ->
                        val accent = categoryAccent(item.category, palette.primaryGlow, palette.secondaryGlow)
                        Surface(
                            shape = ChamferedHudShape(8.dp),
                            color = StageVoid.copy(alpha = 0.72f),
                            border = BorderStroke(0.8.dp, accent.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "[${item.category.tag}] ${item.title}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = accent,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = sdf.format(Date(item.timestampMillis)),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = InkMuted
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkCream
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = palette.primaryGlow)
            }
        },
        dismissButton = {
            if (history.isNotEmpty()) {
                TextButton(onClick = onClear) {
                    Text("Limpiar historial", color = InkMuted)
                }
            }
        }
    )
}

private fun categoryIcon(cat: TelemetryPushCategory) = when (cat) {
    TelemetryPushCategory.CLOCK -> Icons.Filled.Schedule
    TelemetryPushCategory.ALARM -> Icons.Filled.Alarm
    TelemetryPushCategory.TIMER -> Icons.Filled.Timer
    TelemetryPushCategory.FX -> Icons.Filled.Layers
    TelemetryPushCategory.MEDIA -> Icons.Filled.PlayCircle
    TelemetryPushCategory.MUSIC -> Icons.Filled.LibraryMusic
    TelemetryPushCategory.DOWNLOAD -> Icons.Filled.Download
    TelemetryPushCategory.CALENDAR -> Icons.Filled.CalendarMonth
    TelemetryPushCategory.AI -> Icons.Filled.AutoAwesome
    TelemetryPushCategory.SYSTEM -> Icons.Filled.Terminal
}

private fun categoryAccent(
    cat: TelemetryPushCategory,
    primary: Color,
    secondary: Color
): Color = when (cat) {
    TelemetryPushCategory.ALARM -> Color(0xFFFFB84D)
    TelemetryPushCategory.FX -> Color(0xFF9D4EDD)
    TelemetryPushCategory.MUSIC -> Color(0xFF00FFA3)
    TelemetryPushCategory.DOWNLOAD -> secondary
    TelemetryPushCategory.AI -> primary
    else -> primary
}
