package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AvTimer
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.configureAppCheck
import com.example.model.AlarmItem
import com.example.model.AppTab
import com.example.model.ClockPreferences
import com.example.ui.components.PushLogHistoryDialog
import com.example.ui.components.SpaceBackgroundLayer
import com.example.ui.components.TelemetryPushBanner
import com.example.ui.screens.AiConnectorScreen
import com.example.ui.screens.CalendarTabScreen
import com.example.ui.screens.ClockScreen
import com.example.ui.screens.DeskModeOverlay
import com.example.ui.screens.MediaHubScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.screens.TimeToolsScreen
import com.example.ui.theme.InkCream
import com.example.ui.theme.InkMuted
import com.example.ui.theme.LocalNekoPalette
import com.example.ui.theme.NekoBeatTheme
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.StageCardElevated
import com.example.ui.theme.StageSurface
import com.example.ui.theme.StageVoid
import com.example.viewmodel.MediaViewModel
import com.example.viewmodel.NekoBeatViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(StageVoid.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(StageSurface.toArgb())
        )
        setContent {
            val viewModel: NekoBeatViewModel = viewModel()
            val mediaViewModel: MediaViewModel = viewModel()
            val lifecycleOwner = LocalLifecycleOwner.current

            val preferences by viewModel.preferences.collectAsStateWithLifecycle()
            val nowMillis by viewModel.nowMillis.collectAsStateWithLifecycle()
            val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
            val isDeskMode by viewModel.isDeskMode.collectAsStateWithLifecycle()
            val isAppForeground by viewModel.isAppForeground.collectAsStateWithLifecycle()
            val timerState by viewModel.timerState.collectAsStateWithLifecycle()
            val stopwatchState by viewModel.stopwatchState.collectAsStateWithLifecycle()
            val alarms by viewModel.alarms.collectAsStateWithLifecycle()
            val ringingAlarm by viewModel.ringingAlarm.collectAsStateWithLifecycle()
            val previewingAlarmId by viewModel.previewingAlarmId.collectAsStateWithLifecycle()
            val isSlicingCollage by viewModel.isSlicingCollage.collectAsStateWithLifecycle()
            val latestPushLog by viewModel.latestPushLog.collectAsStateWithLifecycle()
            val pushLogHistory by viewModel.pushLogHistory.collectAsStateWithLifecycle()

            var showLogHistoryDialog by remember { mutableStateOf(false) }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> viewModel.setAppInForeground(true)
                        Lifecycle.Event.ON_STOP -> viewModel.setAppInForeground(false)
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            LaunchedEffect(viewModel, mediaViewModel) {
                mediaViewModel.onPushLog = { category, title, msg ->
                    viewModel.emitPushLog(category, title, msg)
                }
            }

            LaunchedEffect(isAppForeground, activeTab, preferences.batteryOptimizationEnabled) {
                mediaViewModel.updateLifecycleAndVisibility(
                    isForeground = isAppForeground,
                    isMediaTabVisible = activeTab == AppTab.MEDIA,
                    batteryOptimized = preferences.batteryOptimizationEnabled
                )
            }

            val nextActiveAlarm = remember(alarms) {
                alarms.filter { it.enabled && it.nextTriggerAtMillis > 0L }
                    .minByOrNull { it.nextTriggerAtMillis }
                    ?: alarms.firstOrNull { it.enabled }
            }

            NekoBeatTheme(
                colorTheme = preferences.colorTheme,
                customPrimaryHex = preferences.customHudPrimaryHex,
                customSecondaryHex = preferences.customHudSecondaryHex
            ) {
                if (activeTab != AppTab.CLOCK && !isDeskMode) {
                    BackHandler {
                        viewModel.selectTab(AppTab.CLOCK)
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    NekoBeatAppShell(
                        preferences = preferences,
                        activeTab = activeTab,
                        onSelectTab = viewModel::selectTab
                    ) { innerModifier ->
                        when (activeTab) {
                            AppTab.CLOCK -> ClockScreen(
                                nowMillis = nowMillis,
                                preferences = preferences,
                                nextAlarm = nextActiveAlarm,
                                onCycleDialStyle = viewModel::cycleDialStyle,
                                onSelectDialStyle = viewModel::setDialStyle,
                                onCycleColorTheme = viewModel::cycleColorTheme,
                                onToggleBiolumEyes = viewModel::setBiolumEyesOverlayEnabled,
                                onToggleParticleStream = viewModel::setParticleStreamEnabled,
                                onToggle24Hour = viewModel::setUse24Hour,
                                onSelectWorldCity = viewModel::setSelectedWorldCity,
                                onUpdateHudTextBoxes = viewModel::updateHudTextBoxes,
                                onOpenAlarmsTab = { viewModel.selectTab(AppTab.TIME) },
                                onEnterDeskMode = { viewModel.setDeskMode(true) },
                                modifier = innerModifier
                            )

                            AppTab.TIME -> TimeToolsScreen(
                                alarms = alarms,
                                previewingAlarmId = previewingAlarmId,
                                timerState = timerState,
                                stopwatchState = stopwatchState,
                                preferences = preferences,
                                onSaveAlarm = viewModel::saveAlarm,
                                onToggleAlarmEnabled = viewModel::toggleAlarmEnabled,
                                onDeleteAlarm = viewModel::deleteAlarm,
                                onImportAlarmAudio = viewModel::importAlarmAudio,
                                onTogglePreviewAlarmAudio = viewModel::toggleAlarmPreview,
                                onStopAlarmPreview = viewModel::stopAlarmPreview,
                                onSelectTimerPreset = viewModel::selectTimerPreset,
                                onAddOneMinute = viewModel::addOneMinuteToTimer,
                                onToggleTimerStartPause = viewModel::toggleTimer,
                                onResetTimer = viewModel::resetTimer,
                                onDismissTimerFinished = viewModel::dismissTimerFinished,
                                onToggleStopwatchStartPause = viewModel::toggleStopwatch,
                                onRecordLap = viewModel::recordLap,
                                onResetStopwatch = viewModel::resetStopwatch,
                                modifier = innerModifier
                            )

                            AppTab.MEDIA -> MediaHubScreen(
                                mediaViewModel = mediaViewModel,
                                batteryOptimized = preferences.batteryOptimizationEnabled,
                                modifier = innerModifier
                            )

                            AppTab.CALENDAR -> CalendarTabScreen(
                                modifier = innerModifier,
                                onLogAction = { cat, title, msg ->
                                    viewModel.emitPushLog(cat, title, msg)
                                }
                            )

                            AppTab.AI -> AiConnectorScreen(
                                preferences = preferences,
                                onSelectBackendMode = viewModel::setAiBackendMode,
                                onSaveEdgeConfig = viewModel::updateEdgeConfig,
                                onSaveExternalApiConfig = viewModel::updateExternalApiConfig,
                                onSaveFirebaseModel = viewModel::setFirebaseGeminiModel,
                                onSaveAiSystemPrompt = viewModel::setAiSystemPrompt,
                                onApplyAiThemeProfile = viewModel::applyAiThemeProfile,
                                onLogAction = { cat, title, msg ->
                                    viewModel.emitPushLog(cat, title, msg)
                                },
                                modifier = innerModifier
                            )

                            AppTab.STUDIO -> StudioScreen(
                                preferences = preferences,
                                isSlicingCollage = isSlicingCollage,
                                onImportCustomBackground = viewModel::importCustomBackground,
                                onClearCustomBackground = viewModel::clearCustomBackground,
                                onSetCustomBackgroundAlpha = viewModel::setCustomBackgroundAlpha,
                                onSetBackgroundTransform = viewModel::setBackgroundTransform,
                                onImportCustomPanelArt = viewModel::importCustomPanelArt,
                                onClearCustomPanelArt = viewModel::clearCustomPanelArt,
                                onSetPanelAlpha = viewModel::setPanelAlpha,
                                onSetPanelTransform = viewModel::setPanelTransform,
                                onImportCustomEyeMedia = viewModel::importCustomEyeMedia,
                                onClearCustomEyeMedia = viewModel::clearCustomEyeMedia,
                                onSetCustomEyeMediaAlpha = viewModel::setCustomEyeMediaAlpha,
                                onSetCustomEyeMediaFitMode = viewModel::setCustomEyeMediaFitMode,
                                onSelectColorTheme = viewModel::setColorTheme,
                                onSetCustomHudPrimaryHex = viewModel::setCustomHudPrimaryHex,
                                onSetCustomHudSecondaryHex = viewModel::setCustomHudSecondaryHex,
                                onSelectDialStyle = viewModel::setDialStyle,
                                onToggleBiolumEyes = viewModel::setBiolumEyesOverlayEnabled,
                                onSelectCanvasOverlayStyle = viewModel::setCanvasOverlayStyle,
                                onSelectIrisPupilStyle = viewModel::setIrisPupilStyle,
                                onSelectEyeColor = viewModel::setEyeColorPreset,
                                onSetCustomEyeColorHex = viewModel::setCustomEyeColorHex,
                                onSetEyeGlowIntensity = viewModel::setEyeGlowIntensity,
                                onSetEyeCanvasScale = viewModel::setEyeCanvasScale,
                                onSetEyeVerticalOffset = viewModel::setEyeVerticalOffset,
                                onSetEyeHorizontalOffset = viewModel::setEyeHorizontalOffset,
                                onToggleEyeWhiskers = viewModel::setEyeWhiskersEnabled,
                                onToggleEyeBrows = viewModel::setEyeBrowsEnabled,
                                onToggleEyeBlink = viewModel::setEyeBlinkEnabled,
                                onUpdateHudTextBoxes = viewModel::updateHudTextBoxes,
                                onToggleParticleStream = viewModel::setParticleStreamEnabled,
                                onToggleStarfield = viewModel::setStarfieldEnabled,
                                onToggleAuroraNebula = viewModel::setAuroraNebulaEnabled,
                                onToggleAnamorphicFlare = viewModel::setAnamorphicFlareEnabled,
                                onToggleGravityHorizon = viewModel::setGravityHorizonEnabled,
                                onToggleCinematicScanlines = viewModel::setCinematicScanlinesEnabled,
                                onTogglePerspectiveGrid3d = viewModel::setPerspectiveGrid3dEnabled,
                                onToggleHoloPrism3d = viewModel::setHoloPrism3dEnabled,
                                onToggleNeonPulseRings = viewModel::setNeonPulseRingsEnabled,
                                onToggleCyberMatrixRain = viewModel::setCyberMatrixRainEnabled,
                                onToggleAtmosphericFog = viewModel::setAtmosphericFogEnabled,
                                onSetFxIntensity = viewModel::setFxIntensity,
                                onApplyFxPreset = viewModel::applyFxPreset,
                                onAddCustomFxLayer = viewModel::addCustomFxLayer,
                                onToggleCustomFxLayer = viewModel::toggleCustomFxLayer,
                                onDeleteCustomFxLayer = viewModel::deleteCustomFxLayer,
                                onToggleBatteryOptimization = viewModel::setBatteryOptimizationEnabled,
                                onToggle24Hour = viewModel::setUse24Hour,
                                onToggleSeconds = viewModel::setShowSeconds,
                                onToggleBeatPulse = viewModel::setBeatPulseEnabled,
                                onToggleSeoulClock = viewModel::setShowSeoulClock,
                                modifier = innerModifier
                            )
                        }
                    }

                    // Telemetry Push Log floating banner on every activated function
                    TelemetryPushBanner(
                        latestLog = latestPushLog,
                        historyCount = pushLogHistory.size,
                        onDismiss = viewModel::dismissPushLog,
                        onOpenHistory = { showLogHistoryDialog = true },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )

                    AnimatedVisibility(
                        visible = isDeskMode,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        DeskModeOverlay(
                            nowMillis = nowMillis,
                            preferences = preferences,
                            onExit = { viewModel.setDeskMode(false) }
                        )
                    }

                    val activeAlarm = ringingAlarm
                    if (activeAlarm != null) {
                        AlarmRingingModal(
                            alarm = activeAlarm,
                            onSnooze = { viewModel.snoozeRingingAlarm(5) },
                            onDismiss = { viewModel.stopRingingAlarm() }
                        )
                    }

                    if (showLogHistoryDialog) {
                        PushLogHistoryDialog(
                            history = pushLogHistory,
                            onClear = viewModel::clearPushLogHistory,
                            onDismiss = { showLogHistoryDialog = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmRingingModal(
    alarm: AlarmItem,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalNekoPalette.current
    AlertDialog(
        onDismissRequest = {},
        containerColor = StageCardElevated,
        titleContentColor = InkCream,
        textContentColor = InkCream,
        icon = {
            Icon(
                imageVector = Icons.Filled.AlarmOn,
                contentDescription = null,
                tint = palette.primaryGlow,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = alarm.formattedTime(),
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 36.sp,
                    color = palette.primaryGlow
                )
                Text(
                    text = alarm.label.ifBlank { "Alarma programada" },
                    style = MaterialTheme.typography.titleLarge,
                    color = InkCream
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (alarm.description.isNotBlank()) {
                    Text(
                        text = alarm.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkCream
                    )
                }
                Text(
                    text = "Audio: ${alarm.audioDisplayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkMuted
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primaryGlow,
                    contentColor = StageVoid
                ),
                modifier = Modifier.testTag("ringing_alarm_dismiss")
            ) {
                Icon(Icons.Filled.NotificationsOff, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Desactivar")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onSnooze,
                border = BorderStroke(1.dp, palette.secondaryGlow),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.secondaryGlow),
                modifier = Modifier.testTag("ringing_alarm_snooze")
            ) {
                Icon(Icons.Filled.Snooze, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Posponer 5m")
            }
        }
    )
}

@Composable
private fun NekoBeatAppShell(
    preferences: ClockPreferences,
    activeTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val palette = LocalNekoPalette.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        SpaceBackgroundLayer(
            customBackgroundPath = preferences.customBackgroundPath,
            customBackgroundAlpha = preferences.customBackgroundAlpha,
            bgTransform = preferences.bgTransform,
            starfieldEnabled = preferences.starfieldEnabled,
            particleStreamEnabled = preferences.particleStreamEnabled,
            auroraNebulaEnabled = preferences.auroraNebulaEnabled,
            anamorphicFlareEnabled = preferences.anamorphicFlareEnabled,
            gravityHorizonEnabled = preferences.gravityHorizonEnabled,
            cinematicScanlinesEnabled = preferences.cinematicScanlinesEnabled,
            perspectiveGrid3dEnabled = preferences.perspectiveGrid3dEnabled,
            holoPrism3dEnabled = preferences.holoPrism3dEnabled,
            neonPulseRingsEnabled = preferences.neonPulseRingsEnabled,
            cyberMatrixRainEnabled = preferences.cyberMatrixRainEnabled,
            atmosphericFogEnabled = preferences.atmosphericFogEnabled,
            fxIntensity = preferences.fxIntensity,
            customFxLayers = preferences.customFxLayers,
            batteryOptimized = preferences.batteryOptimizationEnabled,
            reducedBackdropLoad = activeTab != AppTab.CLOCK
        )

        val useRail = maxWidth >= 600.dp

        if (useRail) {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavigationRail(
                        containerColor = StageSurface.copy(alpha = 0.84f),
                        contentColor = InkCream
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { onSelectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes), maxLines = 1, softWrap = false) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = StageVoid,
                                    selectedTextColor = palette.primaryGlow,
                                    indicatorColor = palette.primaryGlow,
                                    unselectedIconColor = InkMuted,
                                    unselectedTextColor = InkMuted
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        content(Modifier.fillMaxSize())
                    }
                }
            }
        } else {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    NavigationBar(
                        containerColor = StageSurface.copy(alpha = 0.88f),
                        contentColor = InkCream
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { onSelectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        maxLines = 1,
                                        softWrap = false,
                                        fontSize = 10.sp
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = StageVoid,
                                    selectedTextColor = palette.primaryGlow,
                                    indicatorColor = palette.primaryGlow,
                                    unselectedIconColor = InkMuted,
                                    unselectedTextColor = InkMuted
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                content(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}

private fun tabIcon(tab: AppTab, selected: Boolean) = when (tab) {
    AppTab.CLOCK -> if (selected) Icons.Filled.Schedule else Icons.Outlined.Schedule
    AppTab.TIME -> if (selected) Icons.Filled.AvTimer else Icons.Outlined.AvTimer
    AppTab.MEDIA -> if (selected) Icons.Filled.PlayCircle else Icons.Outlined.PlayCircle
    AppTab.CALENDAR -> if (selected) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth
    AppTab.AI -> if (selected) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome
    AppTab.STUDIO -> if (selected) Icons.Filled.Tune else Icons.Outlined.Tune
}
