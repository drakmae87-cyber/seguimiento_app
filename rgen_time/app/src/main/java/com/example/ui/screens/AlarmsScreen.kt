package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.alarm.AlarmScheduler
import com.example.model.AlarmDays
import com.example.model.AlarmItem
import com.example.model.AlarmRepeatUnit
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
import com.example.ui.theme.StageCardElevated
import com.example.ui.theme.StageVoid
import java.util.UUID

private val SUPPORTED_AUDIO_MIMES = arrayOf(
    "audio/*",
    "application/ogg",
    "application/octet-stream"
)

@Composable
fun AlarmsScreen(
    alarms: List<AlarmItem>,
    previewingAlarmId: String?,
    onSaveAlarm: (AlarmItem) -> Unit,
    onToggleAlarmEnabled: (String, Boolean) -> Unit,
    onDeleteAlarm: (String) -> Unit,
    onImportAlarmAudio: (uri: Uri, alarmId: String, onImported: (String, String) -> Unit) -> Unit,
    onTogglePreviewAudio: (alarmId: String, audioPath: String?) -> Unit,
    onStopPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNekoPalette.current
    val context = LocalContext.current
    val hudShape = remember { ChamferedHudShape(14.dp) }
    val chipShape = remember { ChamferedHudShape(8.dp) }

    var editingAlarm by remember { mutableStateOf<AlarmItem?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    val canScheduleExact = remember(alarms.size) { AlarmScheduler.canScheduleExact(context) }

    DisposableEffect(Unit) {
        onDispose { onStopPreview() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.alarms_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = InkCream
                )
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                            notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        editingAlarm = null
                        showEditor = true
                    },
                    shape = chipShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primaryGlow,
                        contentColor = StageVoid
                    ),
                    modifier = Modifier.testTag("alarm_add_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.alarms_add))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            CurvedSciFiHudBanner(
                primaryText = "PROGRAMADOR DE ALERTAS · ${alarms.count { it.enabled }} ACTIVAS",
                secondaryText = "SOPORTE MP3 · WAV · OGG · M4A · REPETICIÓN CONFIGURABLE"
            )

            if (!canScheduleExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = hudShape,
                    colors = CardDefaults.cardColors(containerColor = StageCardElevated),
                    border = BorderStroke(1.dp, palette.secondaryGlow.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Habilite alarmas exactas en ajustes para precisión al segundo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkCream,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                    )
                                }
                            }
                        ) {
                            Text("Permitir", color = palette.secondaryGlow, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (alarms.isEmpty()) {
                Card(
                    shape = hudShape,
                    colors = CardDefaults.cardColors(containerColor = StageCard.copy(alpha = 0.78f)),
                    border = BorderStroke(1.dp, StageBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Alarm,
                            contentDescription = null,
                            tint = palette.primaryGlow,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.alarms_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmItemCard(
                            alarm = alarm,
                            isPreviewing = previewingAlarmId == alarm.id,
                            onToggleEnabled = { enabled -> onToggleAlarmEnabled(alarm.id, enabled) },
                            onEdit = {
                                editingAlarm = alarm
                                showEditor = true
                            },
                            onTogglePreview = { onTogglePreviewAudio(alarm.id, alarm.audioPath) },
                            onDelete = { onDeleteAlarm(alarm.id) }
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        AlarmEditorDialog(
            initialAlarm = editingAlarm,
            previewingAlarmId = previewingAlarmId,
            onDismiss = {
                onStopPreview()
                showEditor = false
            },
            onImportAudio = onImportAlarmAudio,
            onTogglePreview = onTogglePreviewAudio,
            onDelete = if (editingAlarm != null) {
                {
                    onDeleteAlarm(editingAlarm!!.id)
                    showEditor = false
                }
            } else null,
            onSave = { saved ->
                onStopPreview()
                onSaveAlarm(saved)
                showEditor = false
            }
        )
    }
}

@Composable
private fun AlarmItemCard(
    alarm: AlarmItem,
    isPreviewing: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onTogglePreview: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = LocalNekoPalette.current
    val shape = remember { ChamferedHudShape(14.dp) }
    val borderColor = if (alarm.enabled) palette.primaryGlow.copy(alpha = 0.65f) else StageBorder

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.enabled) StageCard.copy(alpha = 0.88f) else StageCard.copy(alpha = 0.56f)
        ),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .testTag("alarm_card_${alarm.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alarm.formattedTime(),
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 30.sp,
                        color = if (alarm.enabled) InkCream else InkMuted
                    )
                    Text(
                        text = alarm.label.ifBlank { "Alarma programada" },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (alarm.enabled) palette.secondaryGlow else InkMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Switch(
                    checked = alarm.enabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = StageVoid,
                        checkedTrackColor = palette.primaryGlow,
                        uncheckedThumbColor = InkMuted,
                        uncheckedTrackColor = StageVoid
                    ),
                    modifier = Modifier.testTag("alarm_switch_${alarm.id}")
                )
            }

            if (alarm.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alarm.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alarm.scheduleSummary(),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = if (alarm.enabled) BioEmerald else InkMuted
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AudioFile,
                            contentDescription = null,
                            tint = palette.primaryGlow,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = alarm.audioDisplayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = InkMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row {
                    IconButton(onClick = onTogglePreview) {
                        Icon(
                            imageVector = if (isPreviewing) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = if (isPreviewing) "Detener vista previa" else "Probar tono",
                            tint = palette.secondaryGlow
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Editar alarma",
                            tint = palette.primaryGlow
                        )
                    }
                    if (alarm.enabled) {
                        IconButton(onClick = { onToggleEnabled(false) }) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsOff,
                                contentDescription = "Desactivar alarma",
                                tint = InkMuted
                            )
                        }
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("alarm_delete_${alarm.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Eliminar alarma",
                            tint = Color(0xFFFF6B4A)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmEditorDialog(
    initialAlarm: AlarmItem?,
    previewingAlarmId: String?,
    onDismiss: () -> Unit,
    onImportAudio: (uri: Uri, alarmId: String, onImported: (String, String) -> Unit) -> Unit,
    onTogglePreview: (alarmId: String, audioPath: String?) -> Unit,
    onDelete: (() -> Unit)?,
    onSave: (AlarmItem) -> Unit
) {
    val palette = LocalNekoPalette.current
    val chipShape = remember { ChamferedHudShape(8.dp) }
    val alarmId = remember(initialAlarm) { initialAlarm?.id ?: UUID.randomUUID().toString() }

    var hourText by remember(initialAlarm) {
        mutableStateOf((initialAlarm?.hour ?: 7).toString().padStart(2, '0'))
    }
    var minuteText by remember(initialAlarm) {
        mutableStateOf((initialAlarm?.minute ?: 0).toString().padStart(2, '0'))
    }
    var label by remember(initialAlarm) { mutableStateOf(initialAlarm?.label ?: "") }
    var description by remember(initialAlarm) { mutableStateOf(initialAlarm?.description ?: "") }
    var selectedDays by remember(initialAlarm) { mutableStateOf(initialAlarm?.repeatDays ?: emptySet()) }
    var repeatUnit by remember(initialAlarm) {
        mutableStateOf(initialAlarm?.repeatIntervalUnit ?: AlarmRepeatUnit.NONE)
    }
    var repeatCountText by remember(initialAlarm) {
        mutableStateOf((initialAlarm?.repeatIntervalCount ?: 1).toString())
    }
    var audioPath by remember(initialAlarm) { mutableStateOf(initialAlarm?.audioPath) }
    var audioDisplayName by remember(initialAlarm) {
        mutableStateOf(initialAlarm?.audioDisplayName ?: "Tono del sistema predeterminado")
    }
    var enabled by remember(initialAlarm) { mutableStateOf(initialAlarm?.enabled ?: true) }
    var vibrate by remember(initialAlarm) { mutableStateOf(initialAlarm?.vibrate ?: true) }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportAudio(uri, alarmId) { savedPath, name ->
                audioPath = savedPath
                audioDisplayName = name
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StageCardElevated,
        titleContentColor = InkCream,
        textContentColor = InkCream,
        title = {
            Text(
                text = if (initialAlarm == null) "NUEVA ALARMA" else "EDITAR ALARMA",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Time HH : MM inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }.take(2)
                            hourText = digits
                        },
                        label = { Text("Hora (00–23)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.primaryGlow,
                            unfocusedBorderColor = StageBorder,
                            focusedTextColor = InkCream,
                            unfocusedTextColor = InkCream
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("alarm_hour_input")
                    )
                    Text(":", fontFamily = OrbitronFontFamily, fontSize = 22.sp, color = palette.primaryGlow)
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }.take(2)
                            minuteText = digits
                        },
                        label = { Text("Minuto (00–59)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.primaryGlow,
                            unfocusedBorderColor = StageBorder,
                            focusedTextColor = InkCream,
                            unfocusedTextColor = InkCream
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("alarm_minute_input")
                    )
                }

                // Editable Label & Description
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(60) },
                    label = { Text("Título de alarma") },
                    placeholder = { Text("Ej. Reunión diaria de ingeniería") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.primaryGlow,
                        unfocusedBorderColor = StageBorder,
                        focusedTextColor = InkCream,
                        unfocusedTextColor = InkCream
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alarm_label_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(240) },
                    label = { Text("Descripción / detalle operativo") },
                    placeholder = { Text("Notas o recordatorio técnico al sonar…") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.primaryGlow,
                        unfocusedBorderColor = StageBorder,
                        focusedTextColor = InkCream,
                        unfocusedTextColor = InkCream
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alarm_description_input")
                )

                // Days of week selector
                Text(
                    text = "Días de ejecución",
                    style = MaterialTheme.typography.labelLarge,
                    color = InkMuted
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AlarmDays.ordered.forEach { dayOpt ->
                        val active = dayOpt.calendarDay in selectedDays
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(CircleShape)
                                .background(if (active) palette.primaryGlow else StageVoid)
                                .border(
                                    1.dp,
                                    if (active) palette.primaryGlow else StageBorder,
                                    CircleShape
                                )
                                .clickable {
                                    selectedDays = if (active) {
                                        selectedDays - dayOpt.calendarDay
                                    } else {
                                        selectedDays + dayOpt.calendarDay
                                    }
                                }
                                .testTag("alarm_day_${dayOpt.shortLabel}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayOpt.shortLabel,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (active) StageVoid else InkCream
                            )
                        }
                    }
                }

                // Repeat interval ("Cada cuánto repetir")
                Text(
                    text = "Frecuencia / Intervalo de repetición",
                    style = MaterialTheme.typography.labelLarge,
                    color = InkMuted
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(AlarmRepeatUnit.entries) { unit ->
                        FilterChip(
                            selected = repeatUnit == unit,
                            onClick = { repeatUnit = unit },
                            shape = chipShape,
                            label = {
                                Text(
                                    text = when (unit) {
                                        AlarmRepeatUnit.NONE -> "Sin intervalo"
                                        AlarmRepeatUnit.MINUTES -> "Minutos"
                                        AlarmRepeatUnit.HOURS -> "Horas"
                                        AlarmRepeatUnit.DAYS -> "Días"
                                        AlarmRepeatUnit.WEEKS -> "Semanas"
                                    },
                                    fontSize = 11.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.secondaryGlow.copy(alpha = 0.24f),
                                selectedLabelColor = palette.secondaryGlow,
                                containerColor = StageVoid,
                                labelColor = InkCream
                            )
                        )
                    }
                }

                if (repeatUnit != AlarmRepeatUnit.NONE) {
                    OutlinedTextField(
                        value = repeatCountText,
                        onValueChange = { raw -> repeatCountText = raw.filter { it.isDigit() }.take(3) },
                        label = { Text("Repetir cada N ${repeatUnit.labelPlural}") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.secondaryGlow,
                            unfocusedBorderColor = StageBorder,
                            focusedTextColor = InkCream,
                            unfocusedTextColor = InkCream
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Custom Ringtone / Song (.mp3, .wav, .ogg, .m4a, etc.)
                Text(
                    text = "Audio / Ringtone (.mp3, .wav, .ogg, .m4a, .flac)",
                    style = MaterialTheme.typography.labelLarge,
                    color = InkMuted
                )
                Surface(
                    shape = chipShape,
                    color = StageVoid.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, StageBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = audioDisplayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.primaryGlow,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { audioPicker.launch(SUPPORTED_AUDIO_MIMES) },
                                shape = chipShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("alarm_pick_audio_button")
                            ) {
                                Icon(Icons.Filled.AudioFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Elegir MP3/Audio", fontSize = 11.sp)
                            }
                            IconButton(onClick = { onTogglePreview(alarmId, audioPath) }) {
                                Icon(
                                    imageVector = if (previewingAlarmId == alarmId) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                    contentDescription = "Probar audio",
                                    tint = palette.secondaryGlow
                                )
                            }
                            if (audioPath != null) {
                                TextButton(onClick = {
                                    audioPath = null
                                    audioDisplayName = "Tono del sistema predeterminado"
                                }) {
                                    Text("Quitar", color = InkMuted, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Alarma habilitada", style = MaterialTheme.typography.bodyMedium, color = InkCream)
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vibración al sonar", style = MaterialTheme.typography.bodyMedium, color = InkCream)
                    Switch(checked = vibrate, onCheckedChange = { vibrate = it })
                }

                if (onDelete != null) {
                    OutlinedButton(
                        onClick = onDelete,
                        shape = chipShape,
                        border = BorderStroke(1.dp, Color(0xFFFF6B4A)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B4A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Eliminar alarma")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val h = (hourText.toIntOrNull() ?: 7).coerceIn(0, 23)
                    val m = (minuteText.toIntOrNull() ?: 0).coerceIn(0, 59)
                    val cnt = (repeatCountText.toIntOrNull() ?: 1).coerceAtLeast(1)
                    onSave(
                        AlarmItem(
                            id = alarmId,
                            hour = h,
                            minute = m,
                            label = label.trim().ifBlank { "Alarma programada" },
                            description = description.trim(),
                            enabled = enabled,
                            repeatDays = selectedDays,
                            repeatIntervalCount = cnt,
                            repeatIntervalUnit = repeatUnit,
                            audioPath = audioPath,
                            audioDisplayName = audioDisplayName,
                            vibrate = vibrate
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primaryGlow,
                    contentColor = StageVoid
                ),
                modifier = Modifier.testTag("alarm_save_button")
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = InkMuted)
            }
        }
    )
}
