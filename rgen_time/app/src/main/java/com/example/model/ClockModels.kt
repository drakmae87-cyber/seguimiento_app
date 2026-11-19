package com.example.model

import androidx.annotation.StringRes
import com.example.R

enum class AppTab(
    @StringRes val labelRes: Int
) {
    CLOCK(R.string.nav_clock),
    TIME(R.string.nav_time),
    MEDIA(R.string.nav_media),
    CALENDAR(R.string.nav_calendar),
    AI(R.string.nav_ai),
    STUDIO(R.string.nav_studio)
}

enum class MediaFitMode(val id: String, val label: String) {
    CROP("crop", "Rellenar"),
    FIT("fit", "Ajustar"),
    FILL("fill", "Estirar"),
    INSIDE("inside", "Centrar");

    companion object {
        fun fromId(id: String?): MediaFitMode =
            entries.firstOrNull { it.id == id } ?: CROP
    }
}

data class MediaTransform(
    val scale: Float = 1.0f,
    val offsetX: Float = 0.0f,
    val offsetY: Float = 0.0f,
    val fitMode: MediaFitMode = MediaFitMode.CROP
)

enum class HudColorTheme(
    val id: String,
    @StringRes val nameRes: Int,
    val hudCode: String
) {
    INTERSTELLAR(
        id = "interstellar",
        nameRes = R.string.theme_interstellar_name,
        hudCode = "SYS // ATMOS"
    ),
    PLASMA_NEON(
        id = "plasma_neon",
        nameRes = R.string.theme_plasma_name,
        hudCode = "SYS // SPECTRUM"
    ),
    COBALT_STREAM(
        id = "cobalt_stream",
        nameRes = R.string.theme_cobalt_name,
        hudCode = "SYS // COBALT 3D"
    ),
    SOLAR_HORIZON(
        id = "solar_horizon",
        nameRes = R.string.theme_solar_name,
        hudCode = "SYS // HORIZON"
    );

    companion object {
        fun fromId(id: String?): HudColorTheme =
            entries.firstOrNull { it.id == id } ?: INTERSTELLAR
    }
}

enum class EyeColorPreset(
    val id: String,
    @StringRes val labelRes: Int,
    val hex: String
) {
    EMERALD("emerald", R.string.eye_color_emerald, "#00FFA3"),
    CYAN("cyan", R.string.eye_color_cyan, "#00E5FF"),
    MAGENTA("magenta", R.string.eye_color_magenta, "#FF2E93"),
    AMBER("amber", R.string.eye_color_amber, "#FFB84D");

    companion object {
        fun fromId(id: String?): EyeColorPreset =
            entries.firstOrNull { it.id == id } ?: EMERALD
    }
}

data class CodexColorToken(
    val code: String,
    val label: String,
    val hex: String
)

object HudColorCodex {
    val tokens = listOf(
        CodexColorToken("BIO-01", "Bio Esmeralda", "#00FFA3"),
        CodexColorToken("HUD-02", "Cian Orbital", "#00E5FF"),
        CodexColorToken("COB-03", "Cobalto 3D", "#3A86FF"),
        CodexColorToken("PLS-04", "Violeta Espectro", "#9D4EDD"),
        CodexColorToken("NEO-05", "Magenta Técnico", "#FF2E93"),
        CodexColorToken("STR-06", "Ámbar Señal", "#FFB84D"),
        CodexColorToken("HOR-07", "Coral Alerta", "#FF6B4A"),
        CodexColorToken("CRM-08", "Carmesí Pulso", "#FF3B5C"),
        CodexColorToken("MTX-09", "Lima Matriz", "#A8FF00"),
        CodexColorToken("CRY-10", "Blanco Crio", "#E6F4FF")
    )
}

/**
 * Geometric HUD core overlays — strictly without cat eye or robotic visor styles.
 */
enum class CanvasOverlayStyle(
    val id: String,
    @StringRes val labelRes: Int
) {
    CELESTIAL_CORE("celestial_core", R.string.canvas_style_celestial),
    HUD_RETICLE("hud_reticle", R.string.canvas_style_neko_hud),
    TENSOR_PRISM("tensor_prism", R.string.canvas_style_tensor);

    companion object {
        fun fromId(id: String?): CanvasOverlayStyle = when (id) {
            "celestial_core" -> CELESTIAL_CORE
            "hud_reticle", "neko_hud", "optical_eye", "feline_eyes", "cyber_visor" -> HUD_RETICLE
            "tensor_prism" -> TENSOR_PRISM
            else -> CELESTIAL_CORE
        }
    }
}

enum class IrisPupilStyle(
    val id: String,
    @StringRes val labelRes: Int
) {
    SLIT("slit", R.string.pupil_slit),
    ROUND("round", R.string.pupil_round),
    DIAMOND("diamond", R.string.pupil_diamond),
    CROSSHAIR("crosshair", R.string.pupil_crosshair);

    companion object {
        fun fromId(id: String?): IrisPupilStyle =
            entries.firstOrNull { it.id == id } ?: CROSSHAIR
    }
}

enum class DialStyle(
    val id: String,
    @StringRes val labelRes: Int
) {
    COLLAGER4("collager4", R.string.dial_collager4),
    PLASMA("plasma", R.string.dial_plasma),
    HUD_4("hud4", R.string.dial_hud4);

    companion object {
        fun fromId(id: String?): DialStyle =
            entries.firstOrNull { it.id == id } ?: COLLAGER4
    }
}

enum class AiBackendMode(val id: String) {
    LOCAL_EDGE("local_edge"),
    EXTERNAL_API("external_api"),
    FIREBASE_GEMINI("firebase_gemini");

    companion object {
        fun fromId(id: String?): AiBackendMode =
            entries.firstOrNull { it.id == id } ?: LOCAL_EDGE
    }
}

enum class LocalEdgeFormat(val id: String, val label: String) {
    OPENAI_COMPAT("openai_compat", "OpenAI /v1/chat/completions"),
    OLLAMA_CHAT("ollama_chat", "Ollama /api/chat");

    companion object {
        fun fromId(id: String?): LocalEdgeFormat =
            entries.firstOrNull { it.id == id } ?: OPENAI_COMPAT
    }
}

enum class ExternalApiFormat(val id: String, val label: String) {
    OPENAI_COMPAT("openai_compat", "OpenAI / OpenRouter / Groq"),
    GEMINI_REST("gemini_rest", "Gemini REST API");

    companion object {
        fun fromId(id: String?): ExternalApiFormat =
            entries.firstOrNull { it.id == id } ?: OPENAI_COMPAT
    }
}

data class WorldCity(
    val id: String,
    val name: String,
    val zoneId: String,
    val tag: String
)

object WorldCities {
    val all = listOf(
        WorldCity("seoul", "Seúl", "Asia/Seoul", "KST"),
        WorldCity("tokyo", "Tokio", "Asia/Tokyo", "JST"),
        WorldCity("madrid", "Madrid", "Europe/Madrid", "CET"),
        WorldCity("cdmx", "CDMX", "America/Mexico_City", "CST"),
        WorldCity("nyc", "Nueva York", "America/New_York", "EST"),
        WorldCity("london", "Londres", "Europe/London", "GMT")
    )

    fun byId(id: String?): WorldCity =
        all.firstOrNull { it.id == id } ?: all.first()
}

data class ClockPreferences(
    val colorTheme: HudColorTheme = HudColorTheme.INTERSTELLAR,
    val customHudPrimaryHex: String? = null,
    val customHudSecondaryHex: String? = null,
    val dialStyle: DialStyle = DialStyle.COLLAGER4,

    // Core Telemetry Overlay (Orbital Core / HUD Reticle / Tensor Prism OR custom animation .gif/.json/.lottie/.riv)
    val biolumEyesOverlayEnabled: Boolean = true,
    val canvasOverlayStyle: CanvasOverlayStyle = CanvasOverlayStyle.CELESTIAL_CORE,
    val irisPupilStyle: IrisPupilStyle = IrisPupilStyle.CROSSHAIR,
    val eyeColorPreset: EyeColorPreset = EyeColorPreset.EMERALD,
    val customEyeColorHex: String? = null,
    val eyeGlowIntensity: Float = 0.85f,
    val eyeCanvasScale: Float = 1.0f,
    val eyeVerticalOffset: Float = 0.0f,
    val eyeHorizontalOffset: Float = 0.0f,
    val eyeWhiskersEnabled: Boolean = true,
    val eyeBrowsEnabled: Boolean = true,
    val eyeBlinkEnabled: Boolean = true,
    val customEyeMediaPath: String? = null,
    val customEyeMediaAlpha: Float = 0.92f,
    val customEyeMediaFitMode: MediaFitMode = MediaFitMode.FIT,

    // Atmospheric, Particle, 3D & Neon EFX visual layers
    val particleStreamEnabled: Boolean = true,
    val starfieldEnabled: Boolean = true,
    val auroraNebulaEnabled: Boolean = true,
    val anamorphicFlareEnabled: Boolean = true,
    val gravityHorizonEnabled: Boolean = false,
    val cinematicScanlinesEnabled: Boolean = false,
    val perspectiveGrid3dEnabled: Boolean = true,
    val holoPrism3dEnabled: Boolean = false,
    val neonPulseRingsEnabled: Boolean = true,
    val cyberMatrixRainEnabled: Boolean = false,
    val atmosphericFogEnabled: Boolean = true,
    val fxIntensity: Float = 0.85f,
    val customFxLayers: List<CustomFxLayer> = emptyList(),

    // Performance & Battery Optimization
    val batteryOptimizationEnabled: Boolean = true,

    // Background media + resize transform
    val customBackgroundPath: String? = null,
    val customBackgroundAlpha: Float = 0.45f,
    val bgScale: Float = 1.0f,
    val bgOffsetX: Float = 0.0f,
    val bgOffsetY: Float = 0.0f,
    val bgFitMode: MediaFitMode = MediaFitMode.CROP,

    // Panel / Collage media + resize transform
    val customPanelArtPath: String? = null,
    val customPanelSlices: List<String> = emptyList(),
    val panelAlpha: Float = 0.56f,
    val panelScale: Float = 1.0f,
    val panelOffsetX: Float = 0.0f,
    val panelOffsetY: Float = 0.0f,
    val panelFitMode: MediaFitMode = MediaFitMode.CROP,

    // Editable corporate HUD text boxes
    val hudBrandTag: String = "RGEN",
    val hudBannerPrimary: String = "RGEN_TIME // TELEMETRÍA",
    val hudBannerSecondary: String = "KRONOS · KYROS SUITE",
    val panelLabelDay: String = "DÍA",
    val panelLabelHours: String = "HORAS",
    val panelLabelMinutes: String = "MINUTOS",
    val panelLabelSeconds: String = "SEGUNDOS",
    val panelLabelDate: String = "FECHA",

    // Clock parameters
    val use24Hour: Boolean = true,
    val showSeconds: Boolean = true,
    val beatPulseEnabled: Boolean = true,
    val showSeoulClock: Boolean = true,
    val selectedWorldCityId: String = "tokyo",

    // AI Connector preferences
    val aiBackendMode: AiBackendMode = AiBackendMode.LOCAL_EDGE,
    val edgeBaseUrl: String = "http://127.0.0.1:11434",
    val edgeModel: String = "llama3.2",
    val edgeFormat: LocalEdgeFormat = LocalEdgeFormat.OLLAMA_CHAT,
    val externalBaseUrl: String = "https://api.openai.com/v1/chat/completions",
    val externalModel: String = "gpt-4o-mini",
    val externalApiKey: String = "",
    val externalFormat: ExternalApiFormat = ExternalApiFormat.OPENAI_COMPAT,
    val firebaseGeminiModel: String = "gemini-3.8-flash",
    val aiSystemPrompt: String = "Eres el núcleo técnico IA de RGEN_time (suite Kronos/Kyros). Responde de forma seria, formal, precisa y orientada al sector tecnológico en español."
) {
    val bgTransform: MediaTransform
        get() = MediaTransform(bgScale, bgOffsetX, bgOffsetY, bgFitMode)

    val panelTransform: MediaTransform
        get() = MediaTransform(panelScale, panelOffsetX, panelOffsetY, panelFitMode)

    val eyeMediaTransform: MediaTransform
        get() = MediaTransform(eyeCanvasScale, eyeHorizontalOffset, eyeVerticalOffset, customEyeMediaFitMode)
}

data class StopwatchLap(
    val number: Int,
    val splitMillis: Long,
    val totalMillis: Long
)
