package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AlarmItem
import com.example.model.ClockPreferences
import com.example.ui.theme.InkCream
import com.example.ui.theme.LocalNekoPalette
import com.example.ui.theme.StageBorder
import com.example.ui.theme.StageCard
import com.example.ui.theme.StageVoid
import com.example.viewmodel.StopwatchUiState
import com.example.viewmodel.TimerUiState

@Composable
fun TimeToolsScreen(
    alarms: List<AlarmItem>,
    previewingAlarmId: String?,
    timerState: TimerUiState,
    stopwatchState: StopwatchUiState,
    preferences: ClockPreferences,
    onSaveAlarm: (AlarmItem) -> Unit,
    onToggleAlarmEnabled: (String, Boolean) -> Unit,
    onDeleteAlarm: (String) -> Unit,
    onImportAlarmAudio: (uri: Uri, alarmId: String, onImported: (String, String) -> Unit) -> Unit,
    onTogglePreviewAlarmAudio: (alarmId: String, audioPath: String?) -> Unit,
    onStopAlarmPreview: () -> Unit,
    onSelectTimerPreset: (Int) -> Unit,
    onAddOneMinute: () -> Unit,
    onToggleTimerStartPause: () -> Unit,
    onResetTimer: () -> Unit,
    onDismissTimerFinished: () -> Unit,
    onToggleStopwatchStartPause: () -> Unit,
    onRecordLap: () -> Unit,
    onResetStopwatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNekoPalette.current
    var subTab by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            listOf("Alarmas", "Timer", "Cronómetro").forEachIndexed { index, label ->
                SegmentedButton(
                    selected = subTab == index,
                    onClick = { subTab = index },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = palette.primaryGlow,
                        activeContentColor = StageVoid,
                        inactiveContainerColor = StageCard.copy(alpha = 0.85f),
                        inactiveContentColor = InkCream,
                        activeBorderColor = palette.primaryGlow,
                        inactiveBorderColor = StageBorder
                    ),
                    modifier = Modifier.testTag("time_subtab_$index")
                ) {
                    Text(text = label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        when (subTab) {
            0 -> AlarmsScreen(
                alarms = alarms,
                previewingAlarmId = previewingAlarmId,
                onSaveAlarm = onSaveAlarm,
                onToggleAlarmEnabled = onToggleAlarmEnabled,
                onDeleteAlarm = onDeleteAlarm,
                onImportAlarmAudio = onImportAlarmAudio,
                onTogglePreviewAudio = onTogglePreviewAlarmAudio,
                onStopPreview = onStopAlarmPreview,
                modifier = Modifier.weight(1f)
            )

            1 -> TimerScreen(
                state = timerState,
                preferences = preferences,
                onSelectPreset = onSelectTimerPreset,
                onAddOneMinute = onAddOneMinute,
                onToggleStartPause = onToggleTimerStartPause,
                onReset = onResetTimer,
                onDismissFinished = onDismissTimerFinished,
                modifier = Modifier.weight(1f)
            )

            else -> StopwatchScreen(
                state = stopwatchState,
                onToggleStartPause = onToggleStopwatchStartPause,
                onRecordLap = onRecordLap,
                onReset = onResetStopwatch,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
