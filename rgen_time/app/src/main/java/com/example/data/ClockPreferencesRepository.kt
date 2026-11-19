package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AiBackendMode
import com.example.model.CanvasOverlayStyle
import com.example.model.ClockPreferences
import com.example.model.CustomFxCodec
import com.example.model.CustomFxLayer
import com.example.model.DialStyle
import com.example.model.ExternalApiFormat
import com.example.model.EyeColorPreset
import com.example.model.HudColorTheme
import com.example.model.IrisPupilStyle
import com.example.model.LocalEdgeFormat
import com.example.model.MediaFitMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rgen_time_prefs")

class ClockPreferencesRepository(private val context: Context) {

    private object Keys {
        val COLOR_THEME = stringPreferencesKey("color_theme")
        val CUSTOM_HUD_PRIMARY_HEX = stringPreferencesKey("custom_hud_primary_hex")
        val CUSTOM_HUD_SECONDARY_HEX = stringPreferencesKey("custom_hud_secondary_hex")
        val DIAL_STYLE = stringPreferencesKey("dial_style")

        // Core optical module
        val BIOLUM_EYES = booleanPreferencesKey("biolum_eyes")
        val CANVAS_OVERLAY_STYLE = stringPreferencesKey("canvas_overlay_style")
        val IRIS_PUPIL_STYLE = stringPreferencesKey("iris_pupil_style")
        val EYE_COLOR = stringPreferencesKey("eye_color")
        val CUSTOM_EYE_COLOR_HEX = stringPreferencesKey("custom_eye_color_hex")
        val EYE_GLOW = floatPreferencesKey("eye_glow")
        val EYE_CANVAS_SCALE = floatPreferencesKey("eye_canvas_scale")
        val EYE_VERTICAL_OFFSET = floatPreferencesKey("eye_vertical_offset")
        val EYE_HORIZONTAL_OFFSET = floatPreferencesKey("eye_horizontal_offset")
        val EYE_WHISKERS = booleanPreferencesKey("eye_whiskers")
        val EYE_BROWS = booleanPreferencesKey("eye_brows")
        val EYE_BLINK = booleanPreferencesKey("eye_blink")
        val CUSTOM_EYE_MEDIA_PATH = stringPreferencesKey("custom_eye_media_path")
        val CUSTOM_EYE_MEDIA_ALPHA = floatPreferencesKey("custom_eye_media_alpha")
        val CUSTOM_EYE_MEDIA_FIT = stringPreferencesKey("custom_eye_media_fit")

        // Atmospheric, Particle, 3D & Neon EFX
        val PARTICLE_STREAM = booleanPreferencesKey("particle_stream")
        val STARFIELD = booleanPreferencesKey("starfield")
        val AURORA_NEBULA = booleanPreferencesKey("aurora_nebula")
        val ANAMORPHIC_FLARE = booleanPreferencesKey("anamorphic_flare")
        val GRAVITY_HORIZON = booleanPreferencesKey("gravity_horizon")
        val CINEMATIC_SCANLINES = booleanPreferencesKey("cinematic_scanlines")
        val PERSPECTIVE_GRID_3D = booleanPreferencesKey("perspective_grid_3d")
        val HOLO_PRISM_3D = booleanPreferencesKey("holo_prism_3d")
        val NEON_PULSE_RINGS = booleanPreferencesKey("neon_pulse_rings")
        val CYBER_MATRIX_RAIN = booleanPreferencesKey("cyber_matrix_rain")
        val ATMOSPHERIC_FOG = booleanPreferencesKey("atmospheric_fog")
        val FX_INTENSITY = floatPreferencesKey("fx_intensity")
        val CUSTOM_FX_LAYERS = stringPreferencesKey("custom_fx_layers")

        // Battery optimization
        val BATTERY_OPTIMIZATION = booleanPreferencesKey("battery_optimization")

        // Background + resize transforms
        val CUSTOM_BG_PATH = stringPreferencesKey("custom_bg_path")
        val CUSTOM_BG_ALPHA = floatPreferencesKey("custom_bg_alpha")
        val BG_SCALE = floatPreferencesKey("bg_scale")
        val BG_OFFSET_X = floatPreferencesKey("bg_offset_x")
        val BG_OFFSET_Y = floatPreferencesKey("bg_offset_y")
        val BG_FIT_MODE = stringPreferencesKey("bg_fit_mode")

        // Panel / Collage + resize transforms
        val CUSTOM_PANEL_PATH = stringPreferencesKey("custom_panel_path")
        val CUSTOM_PANEL_SLICES = stringPreferencesKey("custom_panel_slices")
        val PANEL_ALPHA = floatPreferencesKey("panel_alpha")
        val PANEL_SCALE = floatPreferencesKey("panel_scale")
        val PANEL_OFFSET_X = floatPreferencesKey("panel_offset_x")
        val PANEL_OFFSET_Y = floatPreferencesKey("panel_offset_y")
        val PANEL_FIT_MODE = stringPreferencesKey("panel_fit_mode")

        // Editable text boxes
        val HUD_BRAND_TAG = stringPreferencesKey("hud_brand_tag")
        val HUD_BANNER_PRIMARY = stringPreferencesKey("hud_banner_primary")
        val HUD_BANNER_SECONDARY = stringPreferencesKey("hud_banner_secondary")
        val PANEL_LABEL_DAY = stringPreferencesKey("panel_label_day")
        val PANEL_LABEL_HOURS = stringPreferencesKey("panel_label_hours")
        val PANEL_LABEL_MINUTES = stringPreferencesKey("panel_label_minutes")
        val PANEL_LABEL_SECONDS = stringPreferencesKey("panel_label_seconds")
        val PANEL_LABEL_DATE = stringPreferencesKey("panel_label_date")

        // Clock params
        val USE_24_HOUR = booleanPreferencesKey("use_24_hour")
        val SHOW_SECONDS = booleanPreferencesKey("show_seconds")
        val BEAT_PULSE = booleanPreferencesKey("beat_pulse")
        val SHOW_SEOUL = booleanPreferencesKey("show_seoul")
        val WORLD_CITY_ID = stringPreferencesKey("world_city_id")

        // AI Connector keys
        val AI_BACKEND_MODE = stringPreferencesKey("ai_backend_mode")
        val EDGE_BASE_URL = stringPreferencesKey("edge_base_url")
        val EDGE_MODEL = stringPreferencesKey("edge_model")
        val EDGE_FORMAT = stringPreferencesKey("edge_format")
        val EXTERNAL_BASE_URL = stringPreferencesKey("external_base_url")
        val EXTERNAL_MODEL = stringPreferencesKey("external_model")
        val EXTERNAL_API_KEY = stringPreferencesKey("external_api_key")
        val EXTERNAL_FORMAT = stringPreferencesKey("external_format")
        val FIREBASE_GEMINI_MODEL = stringPreferencesKey("firebase_gemini_model")
        val AI_SYSTEM_PROMPT = stringPreferencesKey("ai_system_prompt")
    }

    val preferencesFlow: Flow<ClockPreferences> = context.dataStore.data.map { prefs ->
        val rawSlices = prefs[Keys.CUSTOM_PANEL_SLICES].orEmpty()
        val parsedSlices = if (rawSlices.isBlank()) {
            emptyList()
        } else {
            rawSlices.split("|").filter { it.isNotBlank() }
        }

        ClockPreferences(
            colorTheme = HudColorTheme.fromId(prefs[Keys.COLOR_THEME]),
            customHudPrimaryHex = prefs[Keys.CUSTOM_HUD_PRIMARY_HEX]?.takeIf { it.isNotBlank() },
            customHudSecondaryHex = prefs[Keys.CUSTOM_HUD_SECONDARY_HEX]?.takeIf { it.isNotBlank() },
            dialStyle = DialStyle.fromId(prefs[Keys.DIAL_STYLE]),
            biolumEyesOverlayEnabled = prefs[Keys.BIOLUM_EYES] ?: true,
            canvasOverlayStyle = CanvasOverlayStyle.fromId(prefs[Keys.CANVAS_OVERLAY_STYLE]),
            irisPupilStyle = IrisPupilStyle.fromId(prefs[Keys.IRIS_PUPIL_STYLE]),
            eyeColorPreset = EyeColorPreset.fromId(prefs[Keys.EYE_COLOR]),
            customEyeColorHex = prefs[Keys.CUSTOM_EYE_COLOR_HEX]?.takeIf { it.isNotBlank() },
            eyeGlowIntensity = prefs[Keys.EYE_GLOW] ?: 0.85f,
            eyeCanvasScale = prefs[Keys.EYE_CANVAS_SCALE] ?: 1.0f,
            eyeVerticalOffset = prefs[Keys.EYE_VERTICAL_OFFSET] ?: 0.0f,
            eyeHorizontalOffset = prefs[Keys.EYE_HORIZONTAL_OFFSET] ?: 0.0f,
            eyeWhiskersEnabled = prefs[Keys.EYE_WHISKERS] ?: true,
            eyeBrowsEnabled = prefs[Keys.EYE_BROWS] ?: true,
            eyeBlinkEnabled = prefs[Keys.EYE_BLINK] ?: true,
            customEyeMediaPath = prefs[Keys.CUSTOM_EYE_MEDIA_PATH]?.takeIf { it.isNotBlank() },
            customEyeMediaAlpha = prefs[Keys.CUSTOM_EYE_MEDIA_ALPHA] ?: 0.92f,
            customEyeMediaFitMode = MediaFitMode.fromId(prefs[Keys.CUSTOM_EYE_MEDIA_FIT]),
            particleStreamEnabled = prefs[Keys.PARTICLE_STREAM] ?: true,
            starfieldEnabled = prefs[Keys.STARFIELD] ?: true,
            auroraNebulaEnabled = prefs[Keys.AURORA_NEBULA] ?: true,
            anamorphicFlareEnabled = prefs[Keys.ANAMORPHIC_FLARE] ?: true,
            gravityHorizonEnabled = prefs[Keys.GRAVITY_HORIZON] ?: false,
            cinematicScanlinesEnabled = prefs[Keys.CINEMATIC_SCANLINES] ?: false,
            perspectiveGrid3dEnabled = prefs[Keys.PERSPECTIVE_GRID_3D] ?: true,
            holoPrism3dEnabled = prefs[Keys.HOLO_PRISM_3D] ?: false,
            neonPulseRingsEnabled = prefs[Keys.NEON_PULSE_RINGS] ?: true,
            cyberMatrixRainEnabled = prefs[Keys.CYBER_MATRIX_RAIN] ?: false,
            atmosphericFogEnabled = prefs[Keys.ATMOSPHERIC_FOG] ?: true,
            fxIntensity = prefs[Keys.FX_INTENSITY] ?: 0.85f,
            customFxLayers = CustomFxCodec.decode(prefs[Keys.CUSTOM_FX_LAYERS]),
            batteryOptimizationEnabled = prefs[Keys.BATTERY_OPTIMIZATION] ?: true,
            customBackgroundPath = prefs[Keys.CUSTOM_BG_PATH]?.takeIf { it.isNotBlank() },
            customBackgroundAlpha = prefs[Keys.CUSTOM_BG_ALPHA] ?: 0.45f,
            bgScale = prefs[Keys.BG_SCALE] ?: 1.0f,
            bgOffsetX = prefs[Keys.BG_OFFSET_X] ?: 0.0f,
            bgOffsetY = prefs[Keys.BG_OFFSET_Y] ?: 0.0f,
            bgFitMode = MediaFitMode.fromId(prefs[Keys.BG_FIT_MODE]),
            customPanelArtPath = prefs[Keys.CUSTOM_PANEL_PATH]?.takeIf { it.isNotBlank() },
            customPanelSlices = parsedSlices,
            panelAlpha = prefs[Keys.PANEL_ALPHA] ?: 0.56f,
            panelScale = prefs[Keys.PANEL_SCALE] ?: 1.0f,
            panelOffsetX = prefs[Keys.PANEL_OFFSET_X] ?: 0.0f,
            panelOffsetY = prefs[Keys.PANEL_OFFSET_Y] ?: 0.0f,
            panelFitMode = MediaFitMode.fromId(prefs[Keys.PANEL_FIT_MODE]),
            hudBrandTag = prefs[Keys.HUD_BRAND_TAG] ?: "RGEN",
            hudBannerPrimary = prefs[Keys.HUD_BANNER_PRIMARY] ?: "RGEN_TIME // TELEMETRÍA",
            hudBannerSecondary = prefs[Keys.HUD_BANNER_SECONDARY] ?: "KRONOS · KYROS SUITE",
            panelLabelDay = prefs[Keys.PANEL_LABEL_DAY] ?: "DÍA",
            panelLabelHours = prefs[Keys.PANEL_LABEL_HOURS] ?: "HORAS",
            panelLabelMinutes = prefs[Keys.PANEL_LABEL_MINUTES] ?: "MINUTOS",
            panelLabelSeconds = prefs[Keys.PANEL_LABEL_SECONDS] ?: "SEGUNDOS",
            panelLabelDate = prefs[Keys.PANEL_LABEL_DATE] ?: "FECHA",
            use24Hour = prefs[Keys.USE_24_HOUR] ?: true,
            showSeconds = prefs[Keys.SHOW_SECONDS] ?: true,
            beatPulseEnabled = prefs[Keys.BEAT_PULSE] ?: true,
            showSeoulClock = prefs[Keys.SHOW_SEOUL] ?: true,
            selectedWorldCityId = prefs[Keys.WORLD_CITY_ID] ?: "tokyo",
            aiBackendMode = AiBackendMode.fromId(prefs[Keys.AI_BACKEND_MODE]),
            edgeBaseUrl = prefs[Keys.EDGE_BASE_URL] ?: "http://127.0.0.1:11434",
            edgeModel = prefs[Keys.EDGE_MODEL] ?: "llama3.2",
            edgeFormat = LocalEdgeFormat.fromId(prefs[Keys.EDGE_FORMAT]),
            externalBaseUrl = prefs[Keys.EXTERNAL_BASE_URL] ?: "https://api.openai.com/v1/chat/completions",
            externalModel = prefs[Keys.EXTERNAL_MODEL] ?: "gpt-4o-mini",
            externalApiKey = prefs[Keys.EXTERNAL_API_KEY] ?: "",
            externalFormat = ExternalApiFormat.fromId(prefs[Keys.EXTERNAL_FORMAT]),
            firebaseGeminiModel = prefs[Keys.FIREBASE_GEMINI_MODEL] ?: "gemini-3.8-flash",
            aiSystemPrompt = prefs[Keys.AI_SYSTEM_PROMPT]
                ?: "Eres el núcleo técnico IA de RGEN_time (suite Kronos/Kyros). Responde de forma seria, formal, precisa y orientada al sector tecnológico en español."
        )
    }

    suspend fun setColorTheme(theme: HudColorTheme) {
        context.dataStore.edit { it[Keys.COLOR_THEME] = theme.id }
    }

    suspend fun setCustomHudPrimaryHex(hex: String?) {
        context.dataStore.edit {
            if (hex.isNullOrBlank()) it.remove(Keys.CUSTOM_HUD_PRIMARY_HEX)
            else it[Keys.CUSTOM_HUD_PRIMARY_HEX] = hex
        }
    }

    suspend fun setCustomHudSecondaryHex(hex: String?) {
        context.dataStore.edit {
            if (hex.isNullOrBlank()) it.remove(Keys.CUSTOM_HUD_SECONDARY_HEX)
            else it[Keys.CUSTOM_HUD_SECONDARY_HEX] = hex
        }
    }

    suspend fun setDialStyle(style: DialStyle) {
        context.dataStore.edit { it[Keys.DIAL_STYLE] = style.id }
    }

    suspend fun setBiolumEyesOverlayEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOLUM_EYES] = enabled }
    }

    suspend fun setCanvasOverlayStyle(style: CanvasOverlayStyle) {
        context.dataStore.edit { it[Keys.CANVAS_OVERLAY_STYLE] = style.id }
    }

    suspend fun setIrisPupilStyle(style: IrisPupilStyle) {
        context.dataStore.edit { it[Keys.IRIS_PUPIL_STYLE] = style.id }
    }

    suspend fun setEyeColorPreset(preset: EyeColorPreset) {
        context.dataStore.edit {
            it[Keys.EYE_COLOR] = preset.id
            it.remove(Keys.CUSTOM_EYE_COLOR_HEX)
        }
    }

    suspend fun setCustomEyeColorHex(hex: String?) {
        context.dataStore.edit {
            if (hex.isNullOrBlank()) it.remove(Keys.CUSTOM_EYE_COLOR_HEX)
            else it[Keys.CUSTOM_EYE_COLOR_HEX] = hex
        }
    }

    suspend fun setEyeGlowIntensity(intensity: Float) {
        context.dataStore.edit { it[Keys.EYE_GLOW] = intensity.coerceIn(0.3f, 1.0f) }
    }

    suspend fun setEyeCanvasScale(scale: Float) {
        context.dataStore.edit { it[Keys.EYE_CANVAS_SCALE] = scale.coerceIn(0.4f, 2.2f) }
    }

    suspend fun setEyeVerticalOffset(offset: Float) {
        context.dataStore.edit { it[Keys.EYE_VERTICAL_OFFSET] = offset.coerceIn(-0.45f, 0.45f) }
    }

    suspend fun setEyeHorizontalOffset(offset: Float) {
        context.dataStore.edit { it[Keys.EYE_HORIZONTAL_OFFSET] = offset.coerceIn(-0.45f, 0.45f) }
    }

    suspend fun setEyeWhiskersEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.EYE_WHISKERS] = enabled }
    }

    suspend fun setEyeBrowsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.EYE_BROWS] = enabled }
    }

    suspend fun setEyeBlinkEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.EYE_BLINK] = enabled }
    }

    suspend fun setCustomEyeMediaPath(path: String?) {
        context.dataStore.edit {
            if (path.isNullOrBlank()) it.remove(Keys.CUSTOM_EYE_MEDIA_PATH)
            else it[Keys.CUSTOM_EYE_MEDIA_PATH] = path
        }
    }

    suspend fun setCustomEyeMediaAlpha(alpha: Float) {
        context.dataStore.edit { it[Keys.CUSTOM_EYE_MEDIA_ALPHA] = alpha.coerceIn(0.15f, 1.0f) }
    }

    suspend fun setCustomEyeMediaFitMode(mode: MediaFitMode) {
        context.dataStore.edit { it[Keys.CUSTOM_EYE_MEDIA_FIT] = mode.id }
    }

    suspend fun setParticleStreamEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PARTICLE_STREAM] = enabled }
    }

    suspend fun setStarfieldEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.STARFIELD] = enabled }
    }

    suspend fun setAuroraNebulaEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AURORA_NEBULA] = enabled }
    }

    suspend fun setAnamorphicFlareEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ANAMORPHIC_FLARE] = enabled }
    }

    suspend fun setGravityHorizonEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GRAVITY_HORIZON] = enabled }
    }

    suspend fun setCinematicScanlinesEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CINEMATIC_SCANLINES] = enabled }
    }

    suspend fun setPerspectiveGrid3dEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PERSPECTIVE_GRID_3D] = enabled }
    }

    suspend fun setHoloPrism3dEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HOLO_PRISM_3D] = enabled }
    }

    suspend fun setNeonPulseRingsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NEON_PULSE_RINGS] = enabled }
    }

    suspend fun setCyberMatrixRainEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CYBER_MATRIX_RAIN] = enabled }
    }

    suspend fun setAtmosphericFogEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ATMOSPHERIC_FOG] = enabled }
    }

    suspend fun setFxIntensity(intensity: Float) {
        context.dataStore.edit { it[Keys.FX_INTENSITY] = intensity.coerceIn(0.2f, 1.5f) }
    }

    suspend fun setCustomFxLayers(layers: List<CustomFxLayer>) {
        context.dataStore.edit {
            it[Keys.CUSTOM_FX_LAYERS] = CustomFxCodec.encode(layers)
        }
    }

    suspend fun setBatteryOptimizationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BATTERY_OPTIMIZATION] = enabled }
    }

    suspend fun setCustomBackgroundPath(path: String?) {
        context.dataStore.edit {
            if (path.isNullOrBlank()) it.remove(Keys.CUSTOM_BG_PATH)
            else it[Keys.CUSTOM_BG_PATH] = path
        }
    }

    suspend fun setCustomBackgroundAlpha(alpha: Float) {
        context.dataStore.edit { it[Keys.CUSTOM_BG_ALPHA] = alpha.coerceIn(0.10f, 1.0f) }
    }

    suspend fun setBackgroundTransform(scale: Float, offsetX: Float, offsetY: Float, fitMode: MediaFitMode) {
        context.dataStore.edit {
            it[Keys.BG_SCALE] = scale.coerceIn(0.4f, 2.2f)
            it[Keys.BG_OFFSET_X] = offsetX.coerceIn(-0.45f, 0.45f)
            it[Keys.BG_OFFSET_Y] = offsetY.coerceIn(-0.45f, 0.45f)
            it[Keys.BG_FIT_MODE] = fitMode.id
        }
    }

    suspend fun setCustomPanelArtAndSlices(masterPath: String?, slicePaths: List<String>) {
        context.dataStore.edit {
            if (masterPath.isNullOrBlank()) {
                it.remove(Keys.CUSTOM_PANEL_PATH)
                it.remove(Keys.CUSTOM_PANEL_SLICES)
            } else {
                it[Keys.CUSTOM_PANEL_PATH] = masterPath
                if (slicePaths.size == 5) {
                    it[Keys.CUSTOM_PANEL_SLICES] = slicePaths.joinToString("|")
                } else {
                    it.remove(Keys.CUSTOM_PANEL_SLICES)
                }
            }
        }
    }

    suspend fun setPanelAlpha(alpha: Float) {
        context.dataStore.edit { it[Keys.PANEL_ALPHA] = alpha.coerceIn(0.15f, 1.0f) }
    }

    suspend fun setPanelTransform(scale: Float, offsetX: Float, offsetY: Float, fitMode: MediaFitMode) {
        context.dataStore.edit {
            it[Keys.PANEL_SCALE] = scale.coerceIn(0.4f, 2.2f)
            it[Keys.PANEL_OFFSET_X] = offsetX.coerceIn(-0.45f, 0.45f)
            it[Keys.PANEL_OFFSET_Y] = offsetY.coerceIn(-0.45f, 0.45f)
            it[Keys.PANEL_FIT_MODE] = fitMode.id
        }
    }

    suspend fun updateHudTextBoxes(
        brandTag: String,
        bannerPrimary: String,
        bannerSecondary: String,
        labelDay: String,
        labelHours: String,
        labelMinutes: String,
        labelSeconds: String,
        labelDate: String
    ) {
        context.dataStore.edit {
            it[Keys.HUD_BRAND_TAG] = brandTag.take(18)
            it[Keys.HUD_BANNER_PRIMARY] = bannerPrimary.take(48)
            it[Keys.HUD_BANNER_SECONDARY] = bannerSecondary.take(48)
            it[Keys.PANEL_LABEL_DAY] = labelDay.take(16)
            it[Keys.PANEL_LABEL_HOURS] = labelHours.take(16)
            it[Keys.PANEL_LABEL_MINUTES] = labelMinutes.take(16)
            it[Keys.PANEL_LABEL_SECONDS] = labelSeconds.take(16)
            it[Keys.PANEL_LABEL_DATE] = labelDate.take(16)
        }
    }

    suspend fun setUse24Hour(use24Hour: Boolean) {
        context.dataStore.edit { it[Keys.USE_24_HOUR] = use24Hour }
    }

    suspend fun setShowSeconds(showSeconds: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_SECONDS] = showSeconds }
    }

    suspend fun setBeatPulseEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BEAT_PULSE] = enabled }
    }

    suspend fun setShowSeoulClock(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_SEOUL] = show }
    }

    suspend fun setSelectedWorldCityId(cityId: String) {
        context.dataStore.edit { it[Keys.WORLD_CITY_ID] = cityId }
    }

    suspend fun setAiBackendMode(mode: AiBackendMode) {
        context.dataStore.edit { it[Keys.AI_BACKEND_MODE] = mode.id }
    }

    suspend fun updateEdgeConfig(baseUrl: String, model: String, format: LocalEdgeFormat) {
        context.dataStore.edit {
            it[Keys.EDGE_BASE_URL] = baseUrl.trim()
            it[Keys.EDGE_MODEL] = model.trim()
            it[Keys.EDGE_FORMAT] = format.id
        }
    }

    suspend fun updateExternalApiConfig(
        baseUrl: String,
        model: String,
        apiKey: String,
        format: ExternalApiFormat
    ) {
        context.dataStore.edit {
            it[Keys.EXTERNAL_BASE_URL] = baseUrl.trim()
            it[Keys.EXTERNAL_MODEL] = model.trim()
            it[Keys.EXTERNAL_API_KEY] = apiKey.trim()
            it[Keys.EXTERNAL_FORMAT] = format.id
        }
    }

    suspend fun setFirebaseGeminiModel(model: String) {
        context.dataStore.edit { it[Keys.FIREBASE_GEMINI_MODEL] = model.trim() }
    }

    suspend fun setAiSystemPrompt(prompt: String) {
        context.dataStore.edit { it[Keys.AI_SYSTEM_PROMPT] = prompt.take(600) }
    }

    suspend fun applyAiThemeProfile(
        primaryHex: String?,
        secondaryHex: String?,
        eyeHex: String?,
        eyeMediaPath: String?,
        starfield: Boolean,
        particleStream: Boolean,
        auroraNebula: Boolean,
        anamorphicFlare: Boolean,
        gravityHorizon: Boolean,
        cinematicScanlines: Boolean
    ) {
        context.dataStore.edit {
            if (!primaryHex.isNullOrBlank()) it[Keys.CUSTOM_HUD_PRIMARY_HEX] = primaryHex
            if (!secondaryHex.isNullOrBlank()) it[Keys.CUSTOM_HUD_SECONDARY_HEX] = secondaryHex
            if (!eyeHex.isNullOrBlank()) it[Keys.CUSTOM_EYE_COLOR_HEX] = eyeHex
            if (!eyeMediaPath.isNullOrBlank()) {
                it[Keys.CUSTOM_EYE_MEDIA_PATH] = eyeMediaPath
                it[Keys.BIOLUM_EYES] = true
            }
            it[Keys.STARFIELD] = starfield
            it[Keys.PARTICLE_STREAM] = particleStream
            it[Keys.AURORA_NEBULA] = auroraNebula
            it[Keys.ANAMORPHIC_FLARE] = anamorphicFlare
            it[Keys.GRAVITY_HORIZON] = gravityHorizon
            it[Keys.CINEMATIC_SCANLINES] = cinematicScanlines
        }
    }
}
