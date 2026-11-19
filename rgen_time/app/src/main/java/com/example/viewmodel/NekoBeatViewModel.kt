package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmPlaybackManager
import com.example.alarm.AlarmScheduler
import com.example.alarm.TelemetryNotificationHelper
import com.example.data.AlarmRepository
import com.example.data.ClockPreferencesRepository
import com.example.data.CollageImageSlicer
import com.example.data.CustomMediaHelper
import com.example.model.AiBackendMode
import com.example.model.AlarmItem
import com.example.model.AppTab
import com.example.model.CanvasOverlayStyle
import com.example.model.ClockPreferences
import com.example.model.CustomFxLayer
import com.example.model.DialStyle
import com.example.model.ExternalApiFormat
import com.example.model.EyeColorPreset
import com.example.model.FxCategory
import com.example.model.HudColorTheme
import com.example.model.IrisPupilStyle
import com.example.model.LocalEdgeFormat
import com.example.model.MediaFitMode
import com.example.model.StopwatchLap
import com.example.model.TelemetryPushCategory
import com.example.model.TelemetryPushMessage
import com.example.model.TelemetryTimeFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class TimerUiState(
    val totalMillis: Long = 25 * 60_000L,
    val remainingMillis: Long = 25 * 60_000L,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val selectedPresetMinutes: Int = 25
) {
    val progress: Float
        get() = if (totalMillis > 0L) (remainingMillis.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f) else 0f
}

data class StopwatchUiState(
    val elapsedMillis: Long = 0L,
    val isRunning: Boolean = false,
    val laps: List<StopwatchLap> = emptyList()
)

class NekoBeatViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val repository = ClockPreferencesRepository(appContext)
    private val alarmRepository = AlarmRepository(appContext)

    val preferences: StateFlow<ClockPreferences> = repository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ClockPreferences()
        )

    val alarms: StateFlow<List<AlarmItem>> = alarmRepository.alarmsFlow
    val ringingAlarm: StateFlow<AlarmItem?> = AlarmPlaybackManager.ringingAlarm
    val previewingAlarmId: StateFlow<String?> = AlarmPlaybackManager.previewingAlarmId

    private val _nowMillis = MutableStateFlow(System.currentTimeMillis())
    val nowMillis: StateFlow<Long> = _nowMillis.asStateFlow()

    private val _activeTab = MutableStateFlow(AppTab.CLOCK)
    val activeTab: StateFlow<AppTab> = _activeTab.asStateFlow()

    private val _isDeskMode = MutableStateFlow(false)
    val isDeskMode: StateFlow<Boolean> = _isDeskMode.asStateFlow()

    private val _isAppForeground = MutableStateFlow(true)
    val isAppForeground: StateFlow<Boolean> = _isAppForeground.asStateFlow()

    private val _timerState = MutableStateFlow(TimerUiState())
    val timerState: StateFlow<TimerUiState> = _timerState.asStateFlow()

    private val _stopwatchState = MutableStateFlow(StopwatchUiState())
    val stopwatchState: StateFlow<StopwatchUiState> = _stopwatchState.asStateFlow()

    private val _isSlicingCollage = MutableStateFlow(false)
    val isSlicingCollage: StateFlow<Boolean> = _isSlicingCollage.asStateFlow()

    private val _latestPushLog = MutableStateFlow<TelemetryPushMessage?>(null)
    val latestPushLog: StateFlow<TelemetryPushMessage?> = _latestPushLog.asStateFlow()

    private val _pushLogHistory = MutableStateFlow<List<TelemetryPushMessage>>(emptyList())
    val pushLogHistory: StateFlow<List<TelemetryPushMessage>> = _pushLogHistory.asStateFlow()

    private var timerJob: Job? = null
    private var stopwatchJob: Job? = null
    private var pushDismissJob: Job? = null
    private var lastLapTotalMillis: Long = 0L

    init {
        alarmRepository.refreshSchedules()
        TelemetryNotificationHelper.ensureChannel(appContext)

        // Adaptive, lifecycle-aware & battery-optimized clock ticker
        viewModelScope.launch {
            combine(
                _isAppForeground,
                _activeTab,
                _isDeskMode,
                preferences
            ) { foreground, tab, desk, prefs ->
                TickMode(
                    foreground = foreground,
                    onClockSurface = desk || tab == AppTab.CLOCK,
                    batteryOpt = prefs.batteryOptimizationEnabled,
                    showSeconds = prefs.showSeconds,
                    beatPulse = prefs.beatPulseEnabled
                )
            }.collectLatest { mode ->
                if (!mode.foreground) return@collectLatest
                while (isActive) {
                    val now = System.currentTimeMillis()
                    _nowMillis.value = now
                    val sleepMs = when {
                        !mode.onClockSurface -> if (mode.batteryOpt) 1500L else 1000L
                        mode.batteryOpt && !mode.showSeconds && !mode.beatPulse ->
                            (60_000L - (now % 60_000L) + 20L).coerceIn(250L, 60_050L)
                        mode.batteryOpt && !mode.beatPulse ->
                            (1000L - (now % 1000L) + 15L).coerceIn(100L, 1020L)
                        mode.batteryOpt -> 200L
                        else -> 100L
                    }
                    delay(sleepMs)
                }
            }
        }
    }

    private data class TickMode(
        val foreground: Boolean,
        val onClockSurface: Boolean,
        val batteryOpt: Boolean,
        val showSeconds: Boolean,
        val beatPulse: Boolean
    )

    fun setAppInForeground(foreground: Boolean) {
        _isAppForeground.value = foreground
        if (foreground) {
            _nowMillis.value = System.currentTimeMillis()
        }
    }

    fun emitPushLog(
        category: TelemetryPushCategory,
        title: String,
        message: String,
        countdownHhMmSs: String? = null,
        sendSystemNotification: Boolean = false
    ) {
        val entry = TelemetryPushMessage(
            category = category,
            title = title,
            message = message,
            countdownHhMmSs = countdownHhMmSs
        )
        publishPushLog(entry, sendSystemNotification = sendSystemNotification, isAlarmCountdown = false)
    }

    private fun publishPushLog(
        entry: TelemetryPushMessage,
        sendSystemNotification: Boolean,
        isAlarmCountdown: Boolean
    ) {
        _latestPushLog.value = entry
        _pushLogHistory.update { current -> (listOf(entry) + current).take(60) }
        pushDismissJob?.cancel()
        pushDismissJob = viewModelScope.launch {
            delay(if (isAlarmCountdown) 8_500L else 5_500L)
            if (_latestPushLog.value?.id == entry.id) {
                _latestPushLog.value = null
            }
        }
        if (sendSystemNotification) {
            TelemetryNotificationHelper.postPushLogNotification(
                context = appContext,
                log = entry,
                isAlarmCountdown = isAlarmCountdown
            )
        }
    }

    fun dismissPushLog() {
        pushDismissJob?.cancel()
        _latestPushLog.value = null
    }

    fun clearPushLogHistory() {
        _pushLogHistory.value = emptyList()
    }

    fun selectTab(tab: AppTab) {
        _activeTab.value = tab
    }

    fun setDeskMode(enabled: Boolean) {
        _isDeskMode.value = enabled
        emitPushLog(
            TelemetryPushCategory.CLOCK,
            if (enabled) "Modo Consola activado" else "Modo Consola finalizado",
            if (enabled) "Visualización de telemetría a pantalla completa" else "Retorno a interfaz principal"
        )
    }

    fun cycleDialStyle() {
        val current = preferences.value.dialStyle
        val next = when (current) {
            DialStyle.COLLAGER4 -> DialStyle.PLASMA
            DialStyle.PLASMA -> DialStyle.HUD_4
            DialStyle.HUD_4 -> DialStyle.COLLAGER4
        }
        setDialStyle(next)
    }

    fun cycleColorTheme() {
        val entries = HudColorTheme.entries
        val currentIdx = entries.indexOf(preferences.value.colorTheme)
        val next = entries[(currentIdx + 1) % entries.size]
        setColorTheme(next)
    }

    fun setColorTheme(theme: HudColorTheme) {
        viewModelScope.launch {
            repository.setColorTheme(theme)
            repository.setCustomHudPrimaryHex(null)
            repository.setCustomHudSecondaryHex(null)
            emitPushLog(
                TelemetryPushCategory.SYSTEM,
                "Esquema cromático aplicado",
                "Perfil activo: ${theme.hudCode}"
            )
        }
    }

    fun setCustomHudPrimaryHex(hex: String?) {
        viewModelScope.launch {
            repository.setCustomHudPrimaryHex(hex)
            emitPushLog(
                TelemetryPushCategory.SYSTEM,
                "Color primario HUD actualizado",
                hex ?: "Restaurado al perfil base"
            )
        }
    }

    fun setCustomHudSecondaryHex(hex: String?) {
        viewModelScope.launch {
            repository.setCustomHudSecondaryHex(hex)
            emitPushLog(
                TelemetryPushCategory.SYSTEM,
                "Color secundario HUD actualizado",
                hex ?: "Restaurado al perfil base"
            )
        }
    }

    fun setDialStyle(style: DialStyle) {
        viewModelScope.launch {
            repository.setDialStyle(style)
            emitPushLog(
                TelemetryPushCategory.CLOCK,
                "Arquitectura de reloj activa",
                "Módulo seleccionado: ${style.id.uppercase(Locale.US)}"
            )
        }
    }

    fun setBiolumEyesOverlayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setBiolumEyesOverlayEnabled(enabled)
            emitPushLog(
                TelemetryPushCategory.CLOCK,
                if (enabled) "Núcleo central activado" else "Núcleo central desactivado",
                if (enabled) "Renderizado de núcleo orbital/retícula habilitado" else "Capa de núcleo central oculta"
            )
        }
    }

    fun setCanvasOverlayStyle(style: CanvasOverlayStyle) {
        viewModelScope.launch {
            repository.setCanvasOverlayStyle(style)
            emitPushLog(
                TelemetryPushCategory.CLOCK,
                "Geometría de núcleo actualizada",
                "Estilo activo: ${style.id.uppercase(Locale.US)}"
            )
        }
    }

    fun setIrisPupilStyle(style: IrisPupilStyle) {
        viewModelScope.launch { repository.setIrisPupilStyle(style) }
    }

    fun setEyeColorPreset(preset: EyeColorPreset) {
        viewModelScope.launch { repository.setEyeColorPreset(preset) }
    }

    fun setCustomEyeColorHex(hex: String?) {
        viewModelScope.launch { repository.setCustomEyeColorHex(hex) }
    }

    fun setEyeGlowIntensity(intensity: Float) {
        viewModelScope.launch { repository.setEyeGlowIntensity(intensity) }
    }

    fun setEyeCanvasScale(scale: Float) {
        viewModelScope.launch { repository.setEyeCanvasScale(scale) }
    }

    fun setEyeVerticalOffset(offset: Float) {
        viewModelScope.launch { repository.setEyeVerticalOffset(offset) }
    }

    fun setEyeHorizontalOffset(offset: Float) {
        viewModelScope.launch { repository.setEyeHorizontalOffset(offset) }
    }

    fun setEyeWhiskersEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setEyeWhiskersEnabled(enabled) }
    }

    fun setEyeBrowsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setEyeBrowsEnabled(enabled) }
    }

    fun setEyeBlinkEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setEyeBlinkEnabled(enabled) }
    }

    fun importCustomEyeMedia(uri: Uri) {
        viewModelScope.launch {
            val savedPath = CustomMediaHelper.importUriToInternalStorage(appContext, uri, "eye")
            if (savedPath != null) {
                repository.setCustomEyeMediaPath(savedPath)
                repository.setBiolumEyesOverlayEnabled(true)
                emitPushLog(
                    TelemetryPushCategory.CLOCK,
                    "Animación/imagen de núcleo cargada",
                    "Recurso vinculado correctamente al módulo central"
                )
            }
        }
    }

    fun clearCustomEyeMedia() {
        viewModelScope.launch {
            CustomMediaHelper.deleteSlotFiles(appContext, "eye")
            repository.setCustomEyeMediaPath(null)
            emitPushLog(TelemetryPushCategory.CLOCK, "Núcleo restaurado", "Modo vectorial procedural restablecido")
        }
    }

    fun setCustomEyeMediaAlpha(alpha: Float) {
        viewModelScope.launch { repository.setCustomEyeMediaAlpha(alpha) }
    }

    fun setCustomEyeMediaFitMode(mode: MediaFitMode) {
        viewModelScope.launch { repository.setCustomEyeMediaFitMode(mode) }
    }

    // --- FX Controls & Custom FX Studio ---
    fun setParticleStreamEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setParticleStreamEnabled(enabled)
            emitPushLog(TelemetryPushCategory.FX, "Flujo de partículas 3D", if (enabled) "Activado" else "Desactivado")
        }
    }

    fun setStarfieldEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setStarfieldEnabled(enabled)
            emitPushLog(TelemetryPushCategory.FX, "Campo estelar", if (enabled) "Activado" else "Desactivado")
        }
    }

    fun setAuroraNebulaEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAuroraNebulaEnabled(enabled)
            emitPushLog(TelemetryPushCategory.FX, "Aurora atmosférica", if (enabled) "Activada" else "Desactivada")
        }
    }

    fun setAnamorphicFlareEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAnamorphicFlareEnabled(enabled)
            emitPushLog(TelemetryPushCategory.FX, "Haz anamórfico neón", if (enabled) "Activado" else "Desactivado")
        }
    }

    fun setGravityHorizonEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setGravityHorizonEnabled(enabled)
            emitPushLog(TelemetryPushCategory.FX, "Anillos de horizonte 3D", if (enabled) "Activados" else "Desactivados")
        }
    }

    fun setCinematicScanlinesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setCinematicScanlinesEnabled(enabled)
            emitPushLog(TelemetryPushCategory.FX, "Escáner láser HUD", if (enabled) "Activado" else "Desactivado")
        }
    }

    fun setPerspectiveGrid3dEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setPerspectiveGrid3dEnabled(enabled)
            emitPushLog(TelemetryPushCategory.FX, "Malla 3D de perspectiva", if (enabled) "Activada" else "Desactivada")
        }
    }

    fun setHoloPrism3dEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setHoloPrism3dEnabled(enabled)
            emitPushLog(TelemetryPushCategory.FX, "Poliedro 3D holográfico", if (enabled) "Activado" else "Desactivado")
        }
    }

    fun setNeonPulseRingsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setNeonPulseRingsEnabled(enabled)
            emitPushLog(TelemetryPushCategory.FX, "Ondas Neón EFX", if (enabled) "Activadas" else "Desactivadas")
        }
    }

    fun setCyberMatrixRainEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setCyberMatrixRainEnabled(enabled)
            emitPushLog(TelemetryPushCategory.FX, "Flujo fotónico vertical", if (enabled) "Activado" else "Desactivado")
        }
    }

    fun setAtmosphericFogEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAtmosphericFogEnabled(enabled)
            emitPushLog(TelemetryPushCategory.FX, "Bruma atmosférica", if (enabled) "Activada" else "Desactivada")
        }
    }

    fun setFxIntensity(intensity: Float) {
        viewModelScope.launch { repository.setFxIntensity(intensity) }
    }

    fun applyFxPreset(presetId: String) {
        viewModelScope.launch {
            when (presetId) {
                "telemetry_3d" -> {
                    repository.setParticleStreamEnabled(true)
                    repository.setPerspectiveGrid3dEnabled(true)
                    repository.setHoloPrism3dEnabled(true)
                    repository.setNeonPulseRingsEnabled(true)
                    repository.setAtmosphericFogEnabled(true)
                    repository.setStarfieldEnabled(true)
                    repository.setAuroraNebulaEnabled(true)
                    repository.setCyberMatrixRainEnabled(false)
                    repository.setFxIntensity(1.0f)
                }
                "neon_storm" -> {
                    repository.setParticleStreamEnabled(true)
                    repository.setCyberMatrixRainEnabled(true)
                    repository.setNeonPulseRingsEnabled(true)
                    repository.setAnamorphicFlareEnabled(true)
                    repository.setCinematicScanlinesEnabled(true)
                    repository.setHoloPrism3dEnabled(true)
                    repository.setPerspectiveGrid3dEnabled(true)
                    repository.setFxIntensity(1.25f)
                }
                "deep_atmos" -> {
                    repository.setStarfieldEnabled(true)
                    repository.setAuroraNebulaEnabled(true)
                    repository.setAtmosphericFogEnabled(true)
                    repository.setGravityHorizonEnabled(true)
                    repository.setParticleStreamEnabled(true)
                    repository.setHoloPrism3dEnabled(false)
                    repository.setCyberMatrixRainEnabled(false)
                    repository.setFxIntensity(0.9f)
                }
                "eco_battery" -> {
                    repository.setBatteryOptimizationEnabled(true)
                    repository.setParticleStreamEnabled(false)
                    repository.setCyberMatrixRainEnabled(false)
                    repository.setHoloPrism3dEnabled(false)
                    repository.setPerspectiveGrid3dEnabled(false)
                    repository.setGravityHorizonEnabled(false)
                    repository.setCinematicScanlinesEnabled(false)
                    repository.setStarfieldEnabled(true)
                    repository.setAuroraNebulaEnabled(true)
                    repository.setFxIntensity(0.55f)
                }
            }
            emitPushLog(
                TelemetryPushCategory.FX,
                "Perfil FX aplicado",
                "Configuración de efectos '$presetId' sincronizada"
            )
        }
    }

    fun addCustomFxLayer(
        name: String,
        category: FxCategory,
        colorHex: String,
        speed: Float,
        density: Float
    ) {
        viewModelScope.launch {
            val cleanName = name.trim().ifBlank { "Capa ${category.label}" }
            val newLayer = CustomFxLayer(
                name = cleanName,
                category = category,
                colorHex = colorHex,
                speed = speed.coerceIn(0.3f, 2.5f),
                density = density.coerceIn(0.2f, 1.5f),
                enabled = true
            )
            val updated = listOf(newLayer) + preferences.value.customFxLayers
            repository.setCustomFxLayers(updated)
            emitPushLog(
                TelemetryPushCategory.FX,
                "Capa FX agregada: $cleanName",
                "Tipo ${category.label} · Color $colorHex activa en escena"
            )
        }
    }

    fun toggleCustomFxLayer(layerId: String, enabled: Boolean) {
        viewModelScope.launch {
            val updated = preferences.value.customFxLayers.map {
                if (it.id == layerId) it.copy(enabled = enabled) else it
            }
            repository.setCustomFxLayers(updated)
            val target = updated.firstOrNull { it.id == layerId }
            emitPushLog(
                TelemetryPushCategory.FX,
                if (enabled) "FX personalizado activado" else "FX personalizado pausado",
                target?.name ?: "Capa FX"
            )
        }
    }

    fun deleteCustomFxLayer(layerId: String) {
        viewModelScope.launch {
            val removed = preferences.value.customFxLayers.firstOrNull { it.id == layerId }
            val updated = preferences.value.customFxLayers.filterNot { it.id == layerId }
            repository.setCustomFxLayers(updated)
            emitPushLog(
                TelemetryPushCategory.FX,
                "Capa FX eliminada",
                removed?.name ?: "Elemento removido"
            )
        }
    }

    fun setBatteryOptimizationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setBatteryOptimizationEnabled(enabled)
            emitPushLog(
                TelemetryPushCategory.SYSTEM,
                if (enabled) "Optimización de batería activada" else "Modo alto rendimiento activado",
                if (enabled) {
                    "Cadencia adaptativa por ciclo de vida y reducción de partículas en segundo plano"
                } else {
                    "Frecuencia de refresco máxima habilitada en pantalla"
                }
            )
        }
    }

    fun importCustomBackground(uri: Uri) {
        viewModelScope.launch {
            val savedPath = CustomMediaHelper.importUriToInternalStorage(appContext, uri, "bg")
            if (savedPath != null) {
                repository.setCustomBackgroundPath(savedPath)
                emitPushLog(TelemetryPushCategory.SYSTEM, "Fondo actualizado", "Recurso visual aplicado al entorno")
            }
        }
    }

    fun clearCustomBackground() {
        viewModelScope.launch {
            CustomMediaHelper.deleteSlotFiles(appContext, "bg")
            repository.setCustomBackgroundPath(null)
            emitPushLog(TelemetryPushCategory.SYSTEM, "Fondo restablecido", "Entorno espacial procedural activo")
        }
    }

    fun setCustomBackgroundAlpha(alpha: Float) {
        viewModelScope.launch { repository.setCustomBackgroundAlpha(alpha) }
    }

    fun setBackgroundTransform(scale: Float, offsetX: Float, offsetY: Float, fitMode: MediaFitMode) {
        viewModelScope.launch { repository.setBackgroundTransform(scale, offsetX, offsetY, fitMode) }
    }

    fun importCustomPanelArt(uri: Uri) {
        viewModelScope.launch {
            _isSlicingCollage.value = true
            try {
                val savedPath = CustomMediaHelper.importUriToInternalStorage(appContext, uri, "panel")
                if (savedPath != null) {
                    val slices = CollageImageSlicer.sliceImageForCollager4(appContext, savedPath)
                    repository.setCustomPanelArtAndSlices(savedPath, slices)
                    emitPushLog(
                        TelemetryPushCategory.CLOCK,
                        "Collage 5 paneles segmentado",
                        "Imagen dividida proporcionalmente para COLLAGER4 / HUD-4"
                    )
                }
            } finally {
                _isSlicingCollage.value = false
            }
        }
    }

    fun clearCustomPanelArt() {
        viewModelScope.launch {
            CustomMediaHelper.deleteSlotFiles(appContext, "panel")
            CollageImageSlicer.deleteOldSliceFiles(appContext)
            repository.setCustomPanelArtAndSlices(null, emptyList())
            emitPushLog(TelemetryPushCategory.CLOCK, "Paneles restablecidos", "Textura de collage removida")
        }
    }

    fun setPanelAlpha(alpha: Float) {
        viewModelScope.launch { repository.setPanelAlpha(alpha) }
    }

    fun setPanelTransform(scale: Float, offsetX: Float, offsetY: Float, fitMode: MediaFitMode) {
        viewModelScope.launch { repository.setPanelTransform(scale, offsetX, offsetY, fitMode) }
    }

    fun updateHudTextBoxes(
        brandTag: String,
        bannerPrimary: String,
        bannerSecondary: String,
        labelDay: String,
        labelHours: String,
        labelMinutes: String,
        labelSeconds: String,
        labelDate: String
    ) {
        viewModelScope.launch {
            repository.updateHudTextBoxes(
                brandTag = brandTag,
                bannerPrimary = bannerPrimary,
                bannerSecondary = bannerSecondary,
                labelDay = labelDay,
                labelHours = labelHours,
                labelMinutes = labelMinutes,
                labelSeconds = labelSeconds,
                labelDate = labelDate
            )
        }
    }

    fun setUse24Hour(use24Hour: Boolean) {
        viewModelScope.launch {
            repository.setUse24Hour(use24Hour)
            emitPushLog(
                TelemetryPushCategory.CLOCK,
                "Formato horario cambiado",
                if (use24Hour) "Modo 24 horas activado" else "Modo 12 horas AM/PM activado"
            )
        }
    }

    fun setShowSeconds(showSeconds: Boolean) {
        viewModelScope.launch {
            repository.setShowSeconds(showSeconds)
            emitPushLog(
                TelemetryPushCategory.CLOCK,
                "Indicador de segundos",
                if (showSeconds) "Segundos visibles en tiempo real" else "Ocultado para ahorro energético"
            )
        }
    }

    fun setBeatPulseEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setBeatPulseEnabled(enabled)
            emitPushLog(
                TelemetryPushCategory.CLOCK,
                "Pulso de sincronización",
                if (enabled) "Modulación luminosa habilitada" else "Modulación estática (bajo consumo)"
            )
        }
    }

    fun setShowSeoulClock(show: Boolean) {
        viewModelScope.launch { repository.setShowSeoulClock(show) }
    }

    fun setSelectedWorldCity(cityId: String) {
        viewModelScope.launch {
            repository.setSelectedWorldCityId(cityId)
            emitPushLog(TelemetryPushCategory.CLOCK, "Zona horaria sincronizada", "Ciudad activa: $cityId")
        }
    }

    // --- AI Connector Config ---
    fun setAiBackendMode(mode: AiBackendMode) {
        viewModelScope.launch {
            repository.setAiBackendMode(mode)
            emitPushLog(TelemetryPushCategory.AI, "Motor IA seleccionado", "Modo activo: ${mode.id}")
        }
    }

    fun updateEdgeConfig(baseUrl: String, model: String, format: LocalEdgeFormat) {
        viewModelScope.launch { repository.updateEdgeConfig(baseUrl, model, format) }
    }

    fun updateExternalApiConfig(
        baseUrl: String,
        model: String,
        apiKey: String,
        format: ExternalApiFormat
    ) {
        viewModelScope.launch { repository.updateExternalApiConfig(baseUrl, model, apiKey, format) }
    }

    fun setFirebaseGeminiModel(model: String) {
        viewModelScope.launch { repository.setFirebaseGeminiModel(model) }
    }

    fun setAiSystemPrompt(prompt: String) {
        viewModelScope.launch { repository.setAiSystemPrompt(prompt) }
    }

    fun applyAiThemeProfile(
        primaryHex: String?,
        secondaryHex: String?,
        eyeHex: String?,
        lottieJson: String?,
        starfield: Boolean,
        particleStream: Boolean,
        auroraNebula: Boolean,
        anamorphicFlare: Boolean,
        gravityHorizon: Boolean,
        cinematicScanlines: Boolean
    ) {
        viewModelScope.launch {
            val savedEyePath = if (!lottieJson.isNullOrBlank()) {
                CustomMediaHelper.saveGeneratedLottieJson(appContext, lottieJson, "eye")
            } else {
                null
            }
            repository.applyAiThemeProfile(
                primaryHex = primaryHex,
                secondaryHex = secondaryHex,
                eyeHex = eyeHex,
                eyeMediaPath = savedEyePath,
                starfield = starfield,
                particleStream = particleStream,
                auroraNebula = auroraNebula,
                anamorphicFlare = anamorphicFlare,
                gravityHorizon = gravityHorizon,
                cinematicScanlines = cinematicScanlines
            )
            emitPushLog(
                TelemetryPushCategory.AI,
                "Síntesis IA aplicada",
                "Paleta adaptativa, capas FX y animación Lottie sincronizadas"
            )
        }
    }

    // --- Alarms with Push Countdown Log (hh:mm:ss) ---
    fun saveAlarm(alarm: AlarmItem) {
        alarmRepository.saveAlarm(alarm)
        if (alarm.enabled) {
            val now = System.currentTimeMillis()
            val nextTrigger = AlarmScheduler.computeNextTriggerAtMillis(alarm, now)
            val pushMsg = TelemetryTimeFormatter.buildAlarmActivationLog(alarm, nextTrigger, now)
            publishPushLog(pushMsg, sendSystemNotification = true, isAlarmCountdown = true)
        } else {
            emitPushLog(
                TelemetryPushCategory.ALARM,
                "Alarma guardada (desactivada)",
                "${alarm.formattedTime()} · ${alarm.label.ifBlank { "Sin título" }}"
            )
        }
    }

    fun toggleAlarmEnabled(alarmId: String, enabled: Boolean) {
        alarmRepository.toggleAlarmEnabled(alarmId, enabled)
        val alarm = alarms.value.firstOrNull { it.id == alarmId }
        if (enabled && alarm != null) {
            val now = System.currentTimeMillis()
            val nextTrigger = AlarmScheduler.computeNextTriggerAtMillis(alarm, now)
            val pushMsg = TelemetryTimeFormatter.buildAlarmActivationLog(alarm, nextTrigger, now)
            publishPushLog(pushMsg, sendSystemNotification = true, isAlarmCountdown = true)
        } else {
            emitPushLog(
                TelemetryPushCategory.ALARM,
                "Alarma desactivada",
                alarm?.let { "${it.formattedTime()} · ${it.label}" } ?: "Alarma pausada"
            )
        }
    }

    fun deleteAlarm(alarmId: String) {
        val target = alarms.value.firstOrNull { it.id == alarmId }
        if (previewingAlarmId.value == alarmId) {
            AlarmPlaybackManager.stopPreview()
        }
        alarmRepository.deleteAlarm(alarmId)
        emitPushLog(
            TelemetryPushCategory.ALARM,
            "Alarma eliminada",
            target?.let { "${it.formattedTime()} · ${it.label}" } ?: "Registro removido"
        )
    }

    fun importAlarmAudio(
        uri: Uri,
        alarmId: String,
        onImported: (path: String, displayName: String) -> Unit
    ) {
        viewModelScope.launch {
            val result = CustomMediaHelper.importAudioToInternalStorage(appContext, uri, alarmId)
            if (result != null) {
                onImported(result.first, result.second)
                emitPushLog(
                    TelemetryPushCategory.ALARM,
                    "Tono de alarma importado",
                    result.second
                )
            }
        }
    }

    fun toggleAlarmPreview(alarmId: String, audioPath: String?) {
        AlarmPlaybackManager.togglePreview(appContext, alarmId, audioPath)
    }

    fun stopAlarmPreview() {
        AlarmPlaybackManager.stopPreview()
    }

    fun stopRingingAlarm() {
        val active = ringingAlarm.value
        AlarmPlaybackManager.stopRinging(appContext)
        emitPushLog(
            TelemetryPushCategory.ALARM,
            "Alarma detenida",
            active?.let { "${it.formattedTime()} · ${it.label}" } ?: "Señal desactivada"
        )
    }

    fun snoozeRingingAlarm(minutes: Int = 5) {
        val active = ringingAlarm.value ?: return
        AlarmPlaybackManager.stopRinging(appContext)
        val now = System.currentTimeMillis()
        val snoozeAt = AlarmScheduler.computeNextSnoozeMillis(minutes, now)
        AlarmScheduler.scheduleAlarm(appContext, active, snoozeAt)
        val remaining = TelemetryTimeFormatter.formatRemainingHhMmSs(snoozeAt, now)
        val msg = TelemetryPushMessage(
            category = TelemetryPushCategory.ALARM,
            title = "Alarma pospuesta $minutes min",
            message = "Nueva activación en $remaining (${active.label})",
            countdownHhMmSs = remaining,
            timestampMillis = now
        )
        publishPushLog(msg, sendSystemNotification = true, isAlarmCountdown = true)
    }

    // --- Timer Controls ---
    fun selectTimerPreset(minutes: Int) {
        timerJob?.cancel()
        val millis = minutes * 60_000L
        _timerState.value = TimerUiState(
            totalMillis = millis,
            remainingMillis = millis,
            isRunning = false,
            isFinished = false,
            selectedPresetMinutes = minutes
        )
        emitPushLog(
            TelemetryPushCategory.TIMER,
            "Intervalo configurado: ${minutes}m",
            "Duración lista: ${TelemetryTimeFormatter.formatRemainingHhMmSs(millis, 0L)}",
            countdownHhMmSs = TelemetryTimeFormatter.formatRemainingHhMmSs(millis, 0L)
        )
    }

    fun addOneMinuteToTimer() {
        _timerState.update { state ->
            val added = 60_000L
            state.copy(
                totalMillis = state.totalMillis + added,
                remainingMillis = state.remainingMillis + added,
                isFinished = false
            )
        }
        emitPushLog(TelemetryPushCategory.TIMER, "Temporizador +01:00", "Tiempo restante incrementado en 1 minuto")
    }

    fun toggleTimer() {
        if (_timerState.value.isRunning) pauseTimer() else startTimer()
    }

    private fun startTimer() {
        val initialRemaining = _timerState.value.remainingMillis
        if (initialRemaining <= 0L) {
            resetTimer()
            return
        }
        timerJob?.cancel()
        _timerState.update { it.copy(isRunning = true, isFinished = false) }

        val endTime = System.currentTimeMillis() + initialRemaining
        val remainingStr = TelemetryTimeFormatter.formatRemainingHhMmSs(initialRemaining, 0L)
        emitPushLog(
            TelemetryPushCategory.TIMER,
            "Temporizador iniciado",
            "Tiempo restante programado: $remainingStr",
            countdownHhMmSs = remainingStr
        )

        timerJob = viewModelScope.launch {
            while (isActive) {
                val left = (endTime - System.currentTimeMillis()).coerceAtLeast(0L)
                if (left == 0L) {
                    _timerState.update {
                        it.copy(remainingMillis = 0L, isRunning = false, isFinished = true)
                    }
                    emitPushLog(
                        TelemetryPushCategory.TIMER,
                        "Temporizador completado",
                        "Ciclo operativo finalizado (00:00:00)",
                        countdownHhMmSs = "00:00:00",
                        sendSystemNotification = true
                    )
                    break
                } else {
                    _timerState.update { it.copy(remainingMillis = left) }
                }
                delay(if (preferences.value.batteryOptimizationEnabled) 200L else 60L)
            }
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        _timerState.update { it.copy(isRunning = false) }
        val rem = TelemetryTimeFormatter.formatRemainingHhMmSs(_timerState.value.remainingMillis, 0L)
        emitPushLog(TelemetryPushCategory.TIMER, "Temporizador pausado", "Restante: $rem", countdownHhMmSs = rem)
    }

    fun resetTimer() {
        timerJob?.cancel()
        _timerState.update { state ->
            state.copy(remainingMillis = state.totalMillis, isRunning = false, isFinished = false)
        }
        emitPushLog(TelemetryPushCategory.TIMER, "Temporizador reiniciado", "Contador restaurado a duración base")
    }

    fun dismissTimerFinished() {
        _timerState.update { state ->
            state.copy(remainingMillis = state.totalMillis, isRunning = false, isFinished = false)
        }
    }

    // --- Stopwatch Controls ---
    fun toggleStopwatch() {
        if (_stopwatchState.value.isRunning) pauseStopwatch() else startStopwatch()
    }

    private fun startStopwatch() {
        val baseElapsed = _stopwatchState.value.elapsedMillis
        val startedAt = System.currentTimeMillis() - baseElapsed
        stopwatchJob?.cancel()
        _stopwatchState.update { it.copy(isRunning = true) }
        emitPushLog(TelemetryPushCategory.TIMER, "Cronómetro activo", "Medición de precisión en marcha")

        stopwatchJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startedAt
                _stopwatchState.update { it.copy(elapsedMillis = elapsed) }
                delay(if (preferences.value.batteryOptimizationEnabled) 33L else 25L)
            }
        }
    }

    private fun pauseStopwatch() {
        stopwatchJob?.cancel()
        _stopwatchState.update { it.copy(isRunning = false) }
        emitPushLog(
            TelemetryPushCategory.TIMER,
            "Cronómetro pausado",
            "Marca actual: ${ClockFormatters.formatStopwatch(_stopwatchState.value.elapsedMillis)}"
        )
    }

    fun recordLap() {
        val state = _stopwatchState.value
        if (!state.isRunning && state.elapsedMillis == 0L) return
        val total = state.elapsedMillis
        val split = (total - lastLapTotalMillis).coerceAtLeast(0L)
        lastLapTotalMillis = total
        val nextNumber = state.laps.size + 1
        val lap = StopwatchLap(number = nextNumber, splitMillis = split, totalMillis = total)
        _stopwatchState.update { it.copy(laps = listOf(lap) + it.laps) }
        emitPushLog(
            TelemetryPushCategory.TIMER,
            "Parcial #$nextNumber registrado",
            "Split ${ClockFormatters.formatStopwatch(split)} · Total ${ClockFormatters.formatStopwatch(total)}"
        )
    }

    fun resetStopwatch() {
        stopwatchJob?.cancel()
        lastLapTotalMillis = 0L
        _stopwatchState.value = StopwatchUiState()
        emitPushLog(TelemetryPushCategory.TIMER, "Cronómetro a cero", "Marcas limpiadas")
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        stopwatchJob?.cancel()
        pushDismissJob?.cancel()
    }
}

object ClockFormatters {

    fun formatMainTime(
        millis: Long,
        use24Hour: Boolean,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val pattern = if (use24Hour) "HH:mm" else "hh:mm"
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        sdf.timeZone = timeZone
        return sdf.format(Date(millis))
    }

    fun formatHourTwoDigits(
        millis: Long,
        use24Hour: Boolean,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val pattern = if (use24Hour) "HH" else "hh"
        val sdf = SimpleDateFormat(pattern, Locale.US)
        sdf.timeZone = timeZone
        return sdf.format(Date(millis))
    }

    fun formatMinuteTwoDigits(
        millis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val sdf = SimpleDateFormat("mm", Locale.US)
        sdf.timeZone = timeZone
        return sdf.format(Date(millis))
    }

    fun formatSeconds(
        millis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val sdf = SimpleDateFormat("ss", Locale.US)
        sdf.timeZone = timeZone
        return sdf.format(Date(millis))
    }

    fun formatDayShortEs(
        millis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val sdf = SimpleDateFormat("EEE", Locale("es", "ES"))
        sdf.timeZone = timeZone
        return sdf.format(Date(millis)).replace(".", "").uppercase(Locale("es", "ES"))
    }

    fun formatDayNumber(
        millis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val sdf = SimpleDateFormat("dd", Locale.US)
        sdf.timeZone = timeZone
        return sdf.format(Date(millis))
    }

    fun formatMonthShortEs(
        millis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val sdf = SimpleDateFormat("MMM", Locale("es", "ES"))
        sdf.timeZone = timeZone
        return sdf.format(Date(millis)).replace(".", "").uppercase(Locale("es", "ES"))
    }

    fun formatYearShort(
        millis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val sdf = SimpleDateFormat("yy", Locale.US)
        sdf.timeZone = timeZone
        return sdf.format(Date(millis))
    }

    fun formatAmPm(
        millis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val sdf = SimpleDateFormat("a", Locale.US)
        sdf.timeZone = timeZone
        return sdf.format(Date(millis))
    }

    fun formatDateEs(
        millis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val sdf = SimpleDateFormat("EEE, d MMM yyyy", Locale("es", "ES"))
        sdf.timeZone = timeZone
        return sdf.format(Date(millis)).uppercase(Locale("es", "ES"))
    }

    fun formatDateKey(year: Int, monthZeroBased: Int, dayOfMonth: Int): String {
        return String.format(Locale.US, "%04d-%02d-%02d", year, monthZeroBased + 1, dayOfMonth)
    }

    fun todayDateKey(): String {
        val cal = Calendar.getInstance()
        return formatDateKey(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun isDaytime(millis: Long, timeZone: TimeZone): Boolean {
        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = millis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return hour in 6..18
    }

    fun formatOffsetFromLocal(millis: Long, targetZone: TimeZone): String {
        val localOffset = TimeZone.getDefault().getOffset(millis)
        val targetOffset = targetZone.getOffset(millis)
        val diffHours = (targetOffset - localOffset) / 3_600_000.0
        return when {
            diffHours == 0.0 -> "Misma hora"
            diffHours % 1.0 == 0.0 -> {
                val h = diffHours.toInt()
                if (h > 0) "+${h}h" else "${h}h"
            }
            else -> {
                if (diffHours > 0) String.format(Locale.US, "+%.1fh", diffHours)
                else String.format(Locale.US, "%.1fh", diffHours)
            }
        }
    }

    fun formatTimerCountdown(millis: Long): String {
        val totalSeconds = ((millis + 999L) / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatStopwatch(millis: Long): String {
        val totalCentis = (millis / 10L).coerceAtLeast(0L)
        val centis = totalCentis % 100L
        val totalSeconds = totalCentis / 100L
        val seconds = totalSeconds % 60L
        val minutes = totalSeconds / 60L
        return String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, centis)
    }
}
