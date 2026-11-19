package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.model.AiBackendMode
import com.example.model.ClockPreferences
import com.example.model.ExternalApiFormat
import com.example.model.LocalEdgeFormat
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val AI_TAG = "AiConnectorRepo"

data class AiRequestConfig(
    val mode: AiBackendMode,
    val edgeBaseUrl: String,
    val edgeModel: String,
    val edgeFormat: LocalEdgeFormat,
    val externalBaseUrl: String,
    val externalModel: String,
    val externalApiKey: String,
    val externalFormat: ExternalApiFormat,
    val firebaseGeminiModel: String,
    val systemPrompt: String = ""
)

data class AiChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val backendTag: String
)

data class AiThemeAnimationProposal(
    val themeName: String,
    val analysis: String,
    val animationPattern: String,
    val primaryHex: String,
    val secondaryHex: String,
    val eyeHex: String,
    val starfield: Boolean,
    val particleStream: Boolean,
    val auroraNebula: Boolean,
    val anamorphicFlare: Boolean,
    val gravityHorizon: Boolean,
    val cinematicScanlines: Boolean,
    val lottieJson: String
)

class AiConnectorRepository {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val defaultGeminiModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private fun getGeminiModel(modelName: String) =
        if (modelName.isBlank() || modelName == "gemini-3.8-flash") {
            defaultGeminiModel
        } else {
            Firebase.ai.generativeModel(modelName)
        }

    suspend fun testConnection(config: AiRequestConfig): Result<String> = withContext(Dispatchers.IO) {
        try {
            when (config.mode) {
                AiBackendMode.FIREBASE_GEMINI -> {
                    val model = getGeminiModel(config.firebaseGeminiModel)
                    val response = withTimeout(30_000L) {
                        model.generateContent("Responde únicamente con: OK RGEN_TIME")
                    }
                    val txt = response.text?.trim().orEmpty()
                    if (txt.isNotEmpty()) {
                        Result.success("Firebase AI Logic conectado (${config.firebaseGeminiModel}): $txt")
                    } else {
                        Result.failure(IllegalStateException("El modelo respondió vacío."))
                    }
                }

                AiBackendMode.LOCAL_EDGE -> {
                    val base = config.edgeBaseUrl.trim().trimEnd('/')
                    if (!base.startsWith("http://") && !base.startsWith("https://")) {
                        return@withContext Result.failure(
                            IllegalArgumentException("La URL debe empezar con http:// o https://")
                        )
                    }
                    val probeUrl = when (config.edgeFormat) {
                        LocalEdgeFormat.OLLAMA_CHAT -> {
                            val root = base.removeSuffix("/api/chat").removeSuffix("/api/generate")
                            "$root/api/tags"
                        }
                        LocalEdgeFormat.OPENAI_COMPAT -> {
                            val root = base.removeSuffix("/v1/chat/completions").removeSuffix("/chat/completions")
                            if (root.endsWith("/v1")) "$root/models" else "$root/v1/models"
                        }
                    }
                    val req = Request.Builder().url(probeUrl).get().build()
                    client.newCall(req).execute().use { resp ->
                        val body = resp.body?.string().orEmpty()
                        if (!resp.isSuccessful) {
                            return@withContext Result.failure(
                                IllegalStateException("HTTP ${resp.code}: ${body.take(180)}")
                            )
                        }
                        val modelsCount = runCatching {
                            val obj = JSONObject(body)
                            when {
                                obj.has("models") -> obj.getJSONArray("models").length()
                                obj.has("data") -> obj.getJSONArray("data").length()
                                else -> 0
                            }
                        }.getOrDefault(0)
                        Result.success("Servidor Edge Local activo ($modelsCount modelos detectados en $probeUrl)")
                    }
                }

                AiBackendMode.EXTERNAL_API -> {
                    if (config.externalApiKey.isBlank()) {
                        return@withContext Result.failure(
                            IllegalArgumentException("Ingrese su API Key para probar la API externa.")
                        )
                    }
                    val reply = sendExternalApiPrompt(
                        config = config,
                        prompt = "Responda únicamente con: OK"
                    )
                    Result.success("API externa conectada: ${reply.take(80)}")
                }
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("Tiempo de espera agotado al conectar con el modelo."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_TAG, "Connection test failed", e)
            Result.failure(e)
        }
    }

    fun generateReplyStream(
        config: AiRequestConfig,
        prompt: String,
        history: List<AiChatMessage>
    ): Flow<String> = flow {
        val systemInstruction = config.systemPrompt.ifBlank {
            "Eres el núcleo técnico IA de RGEN_time (suite corporativa Kronos/Kyros). Responde de forma seria, formal y técnica en español."
        }

        when (config.mode) {
            AiBackendMode.FIREBASE_GEMINI -> {
                val model = getGeminiModel(config.firebaseGeminiModel)
                val fullPrompt = buildString {
                    appendLine(systemInstruction)
                    history.takeLast(6).forEach { msg ->
                        appendLine(if (msg.isUser) "Operador: ${msg.text}" else "Sistema IA: ${msg.text}")
                    }
                    append("Operador: $prompt")
                }
                val accumulated = StringBuilder()
                withTimeout(60_000L) {
                    model.generateContentStream(fullPrompt).collect { chunk ->
                        val piece = chunk.text.orEmpty()
                        if (piece.isNotEmpty()) {
                            accumulated.append(piece)
                            emit(accumulated.toString())
                        }
                    }
                }
                if (accumulated.isBlank()) {
                    throw IllegalStateException("El modelo Gemini devolvió una respuesta vacía.")
                }
            }

            AiBackendMode.LOCAL_EDGE -> {
                val reply = withTimeout(60_000L) {
                    sendLocalEdgePrompt(config, systemInstruction, prompt, history)
                }
                emit(reply)
            }

            AiBackendMode.EXTERNAL_API -> {
                val reply = withTimeout(60_000L) {
                    sendExternalApiPrompt(config, prompt, systemInstruction, history)
                }
                emit(reply)
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun generateThemeAndAnimation(
        context: Context,
        config: AiRequestConfig,
        preferences: ClockPreferences,
        directive: String = ""
    ): Result<AiThemeAnimationProposal> = withContext(Dispatchers.IO) {
        try {
            val bgBitmap = CustomMediaHelper.decodeBackgroundSnapshotForAi(
                context = context,
                path = preferences.customBackgroundPath
            )
            val contextSummary = buildString {
                appendLine("Estado actual del sistema RGEN_time:")
                appendLine("- Arquitectura activa: ${preferences.dialStyle.id}")
                appendLine("- Tema base: ${preferences.colorTheme.id} | Primario=${preferences.customHudPrimaryHex ?: "preset"} | Secundario=${preferences.customHudSecondaryHex ?: "preset"}")
                appendLine("- Fondo personalizado: ${if (preferences.customBackgroundPath != null) "ACTIVO (${MediaFormatKind.fromPath(preferences.customBackgroundPath)})" else "Procedural nativo"}")
                appendLine("- Panel/Collage personalizado: ${if (preferences.customPanelArtPath != null) "ACTIVO" else "Procedural"}")
                appendLine("- Banner HUD: '${preferences.hudBannerPrimary}' / '${preferences.hudBannerSecondary}'")
                appendLine("- Capas activas: starfield=${preferences.starfieldEnabled}, particles=${preferences.particleStreamEnabled}, aurora=${preferences.auroraNebulaEnabled}, flare=${preferences.anamorphicFlareEnabled}, horizon=${preferences.gravityHorizonEnabled}, scanlines=${preferences.cinematicScanlinesEnabled}")
                if (directive.isNotBlank()) {
                    appendLine("- Directriz adicional del operador: $directive")
                }
            }

            val jsonInstruction = """
                Analiza la imagen de fondo adjunta (si existe) y las preferencias actuales de RGEN_time.
                Diseña una configuración cromática técnica en formato HEX (#RRGGBB), activa las capas visuales adecuadas y selecciona un patrón de animación vectorial para el módulo Eye ('orbital_pulse', 'scanner_reticle', 'horizon_wave' o 'data_core').
                Opcionalmente incluye un JSON Lottie v5 mínimo en 'lottieJson' o déjalo vacío para generar la composición vectorial sincronizada con la paleta HEX.
                Responde ESTRICTAMENTE en JSON válido con las claves:
                themeName, analysis, animationPattern, primaryHex, secondaryHex, eyeHex, starfield, particleStream, auroraNebula, anamorphicFlare, gravityHorizon, cinematicScanlines, lottieJson.
                Tono formal, serio y corporativo del sector tecnológico en español.
            """.trimIndent()

            val rawJson = when (config.mode) {
                AiBackendMode.FIREBASE_GEMINI -> {
                    val modelName = config.firebaseGeminiModel.ifBlank { "gemini-3.8-flash" }
                    val schema = Schema.obj(
                        mapOf(
                            "themeName" to Schema.string(),
                            "analysis" to Schema.string(),
                            "animationPattern" to Schema.string(),
                            "primaryHex" to Schema.string(),
                            "secondaryHex" to Schema.string(),
                            "eyeHex" to Schema.string(),
                            "starfield" to Schema.boolean(),
                            "particleStream" to Schema.boolean(),
                            "auroraNebula" to Schema.boolean(),
                            "anamorphicFlare" to Schema.boolean(),
                            "gravityHorizon" to Schema.boolean(),
                            "cinematicScanlines" to Schema.boolean(),
                            "lottieJson" to Schema.string()
                        )
                    )
                    val structuredModel = Firebase.ai.generativeModel(
                        modelName = modelName,
                        generationConfig = generationConfig {
                            responseMimeType = "application/json"
                            responseSchema = schema
                        }
                    )
                    val response = withTimeout(45_000L) {
                        structuredModel.generateContent(
                            content {
                                if (bgBitmap != null) image(bgBitmap)
                                text("$jsonInstruction\n\n$contextSummary")
                            }
                        )
                    }
                    response.text.orEmpty()
                }

                AiBackendMode.LOCAL_EDGE -> {
                    withTimeout(45_000L) {
                        sendLocalEdgePrompt(
                            config = config,
                            systemPrompt = config.systemPrompt.ifBlank { "Responde únicamente con JSON válido sin bloques markdown." },
                            prompt = "$jsonInstruction\n\n$contextSummary",
                            history = emptyList()
                        )
                    }
                }

                AiBackendMode.EXTERNAL_API -> {
                    withTimeout(45_000L) {
                        if (config.externalFormat == ExternalApiFormat.GEMINI_REST && bgBitmap != null) {
                            sendGeminiRestMultimodalPrompt(config, "$jsonInstruction\n\n$contextSummary", bgBitmap)
                        } else {
                            sendExternalApiPrompt(
                                config = config,
                                prompt = "$jsonInstruction\n\n$contextSummary",
                                systemPrompt = config.systemPrompt.ifBlank { "Responde únicamente con JSON válido sin bloques markdown." }
                            )
                        }
                    }
                }
            }

            val proposal = parseAndNormalizeProposal(rawJson, preferences)
            Result.success(proposal)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_TAG, "Failed to generate theme proposal", e)
            Result.failure(e)
        }
    }

    private fun parseAndNormalizeProposal(
        rawText: String,
        prefs: ClockPreferences
    ): AiThemeAnimationProposal {
        val cleaned = rawText.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val jsonStart = cleaned.indexOf('{')
        val jsonEnd = cleaned.lastIndexOf('}')
        val jsonSlice = if (jsonStart >= 0 && jsonEnd > jsonStart) {
            cleaned.substring(jsonStart, jsonEnd + 1)
        } else {
            "{}"
        }

        val obj = runCatching { JSONObject(jsonSlice) }.getOrElse { JSONObject() }
        val primaryHex = normalizeHex(obj.optString("primaryHex"), prefs.customHudPrimaryHex ?: "#00E5FF")
        val secondaryHex = normalizeHex(obj.optString("secondaryHex"), prefs.customHudSecondaryHex ?: "#00FFA3")
        val eyeHex = normalizeHex(obj.optString("eyeHex"), prefs.customEyeColorHex ?: primaryHex)
        val pattern = obj.optString("animationPattern", "scanner_reticle").ifBlank { "scanner_reticle" }

        val candidateLottie = obj.optString("lottieJson", "").trim()
        val validatedLottie = if (isValidLottieJson(candidateLottie)) {
            candidateLottie
        } else {
            buildProceduralTelemetryLottieJson(
                primaryHex = primaryHex,
                secondaryHex = secondaryHex,
                eyeHex = eyeHex,
                pattern = pattern
            )
        }

        return AiThemeAnimationProposal(
            themeName = obj.optString("themeName", "Perfil Técnico RGEN").ifBlank { "Perfil Técnico RGEN" },
            analysis = obj.optString(
                "analysis",
                "Perfil sincronizado con la capa de fondo y los parámetros operativos de RGEN_time."
            ).ifBlank { "Perfil sincronizado con la capa de fondo y los parámetros operativos de RGEN_time." },
            animationPattern = pattern,
            primaryHex = primaryHex,
            secondaryHex = secondaryHex,
            eyeHex = eyeHex,
            starfield = obj.optBoolean("starfield", prefs.starfieldEnabled),
            particleStream = obj.optBoolean("particleStream", prefs.particleStreamEnabled),
            auroraNebula = obj.optBoolean("auroraNebula", prefs.auroraNebulaEnabled),
            anamorphicFlare = obj.optBoolean("anamorphicFlare", prefs.anamorphicFlareEnabled),
            gravityHorizon = obj.optBoolean("gravityHorizon", true),
            cinematicScanlines = obj.optBoolean("cinematicScanlines", true),
            lottieJson = validatedLottie
        )
    }

    private fun normalizeHex(raw: String, fallback: String): String {
        val clean = raw.trim().uppercase()
        val candidate = if (clean.startsWith("#")) clean else "#$clean"
        return if (Regex("^#[0-9A-F]{6}$").matches(candidate)) candidate else fallback
    }

    private fun isValidLottieJson(raw: String): Boolean {
        if (raw.length < 40 || !raw.startsWith("{")) return false
        return runCatching {
            val obj = JSONObject(raw)
            obj.has("v") && obj.has("fr") && obj.has("op") &&
                obj.has("layers") && obj.getJSONArray("layers").length() > 0
        }.getOrDefault(false)
    }

    private fun hexToRgbNormalized(hex: String): Triple<Double, Double, Double> {
        val clean = hex.removePrefix("#")
        val r = clean.substring(0, 2).toInt(16) / 255.0
        val g = clean.substring(2, 4).toInt(16) / 255.0
        val b = clean.substring(4, 6).toInt(16) / 255.0
        return Triple(r, g, b)
    }

    /**
     * Generates a genuine animated Lottie v5 JSON vector telemetry animation parameterized
     * by the AI-selected HEX palette and motion pattern so it always renders smoothly in Lottie Compose.
     */
    fun buildProceduralTelemetryLottieJson(
        primaryHex: String,
        secondaryHex: String,
        eyeHex: String,
        pattern: String
    ): String {
        val (pr, pg, pb) = hexToRgbNormalized(primaryHex)
        val (sr, sg, sb) = hexToRgbNormalized(secondaryHex)
        val (er, eg, eb) = hexToRgbNormalized(eyeHex)

        val outerDurationEnd = if (pattern == "orbital_pulse") 360.0 else -360.0
        val innerScalePeak = if (pattern == "horizon_wave") 112 else 95
        val safePattern = pattern.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")

        fun fmt(d: Double): String = String.format(Locale.US, "%.4f", d)
        fun rgbArrayJson(r: Double, g: Double, b: Double): String =
            "[${fmt(r)},${fmt(g)},${fmt(b)},1.0]"

        fun staticProp(valueJson: String): String = """{"a":0,"k":$valueJson}"""

        fun animatedProp(keyframes: List<Pair<Int, String>>): String = buildString {
            append("""{"a":1,"k":[""")
            keyframes.forEachIndexed { idx, (time, valJson) ->
                if (idx > 0) append(',')
                append("""{"t":$time,"s":$valJson""")
                if (idx < keyframes.lastIndex) {
                    append(""","i":{"x":[0.42],"y":[1.0]},"o":{"x":[0.58],"y":[0.0]}""")
                }
                append('}')
            }
            append("]}")
        }

        fun ringLayerJson(
            ind: Int,
            name: String,
            diameter: Double,
            strokeW: Double,
            rgb: Triple<Double, Double, Double>,
            rotStart: Double,
            rotEnd: Double,
            dashLen: Double,
            gapLen: Double
        ): String {
            val dashField = if (dashLen > 0) {
                ""","d":[{"n":"d","nm":"dash","v":${staticProp(fmt(dashLen))}},{"n":"g","nm":"gap","v":${staticProp(fmt(gapLen))}}]"""
            } else {
                ""
            }
            val strokeShape = """{"ty":"st","c":${staticProp(rgbArrayJson(rgb.first, rgb.second, rgb.third))},"o":${staticProp("90")},"w":${staticProp(fmt(strokeW))},"lc":2,"lj":2$dashField}"""
            val ellipseShape = """{"ty":"el","p":${staticProp("[0,0]")},"s":${staticProp("[${fmt(diameter)},${fmt(diameter)}]")}}"""
            val transformShape = """{"ty":"tr","p":${staticProp("[0,0]")},"a":${staticProp("[0,0]")},"s":${staticProp("[100,100]")},"r":${staticProp("0")},"o":${staticProp("100")}}"""
            val group = """{"ty":"gr","nm":"${name}_grp","it":[$ellipseShape,$strokeShape,$transformShape]}"""
            val rotAnim = animatedProp(listOf(0 to "[${fmt(rotStart)}]", 120 to "[${fmt(rotEnd)}]"))
            val ks = """{"o":${staticProp("100")},"r":$rotAnim,"p":${staticProp("[150,150,0]")},"a":${staticProp("[0,0,0]")},"s":${staticProp("[100,100,100]")}}"""

            return """{"ddd":0,"ind":$ind,"ty":4,"nm":"$name","sr":1,"ks":$ks,"shapes":[$group],"ip":0,"op":120,"st":0}"""
        }

        fun corePulseLayerJson(): String {
            val fill = """{"ty":"fl","c":${staticProp(rgbArrayJson(er, eg, eb))},"o":${staticProp("85")}}"""
            val ellipse = """{"ty":"el","p":${staticProp("[0,0]")},"s":${staticProp("[44,44]")}}"""
            val transformShape = """{"ty":"tr","p":${staticProp("[0,0]")},"a":${staticProp("[0,0]")},"s":${staticProp("[100,100]")},"r":${staticProp("0")},"o":${staticProp("100")}}"""
            val group = """{"ty":"gr","nm":"core_grp","it":[$ellipse,$fill,$transformShape]}"""
            val opacityAnim = animatedProp(listOf(0 to "[45]", 60 to "[100]", 120 to "[45]"))
            val scaleAnim = animatedProp(
                listOf(
                    0 to "[75,75,100]",
                    60 to "[$innerScalePeak,$innerScalePeak,100]",
                    120 to "[75,75,100]"
                )
            )
            val ks = """{"o":$opacityAnim,"r":${staticProp("0")},"p":${staticProp("[150,150,0]")},"a":${staticProp("[0,0,0]")},"s":$scaleAnim}"""

            return """{"ddd":0,"ind":3,"ty":4,"nm":"core_pulse","sr":1,"ks":$ks,"shapes":[$group],"ip":0,"op":120,"st":0}"""
        }

        val layer1 = ringLayerJson(1, "outer_reticle", 220.0, 4.0, Triple(pr, pg, pb), 0.0, outerDurationEnd, 28.0, 14.0)
        val layer2 = ringLayerJson(2, "inner_reticle", 142.0, 3.0, Triple(sr, sg, sb), 0.0, -outerDurationEnd, 16.0, 12.0)
        val layer3 = corePulseLayerJson()

        return """{"v":"5.9.0","fr":30,"ip":0,"op":120,"w":300,"h":300,"nm":"rgen_ai_telemetry_$safePattern","ddd":0,"layers":[$layer1,$layer2,$layer3]}"""
    }

    private fun sendGeminiRestMultimodalPrompt(
        config: AiRequestConfig,
        prompt: String,
        bitmap: Bitmap
    ): String {
        val model = config.externalModel.trim().ifBlank { "gemini-2.5-flash" }
        val base = config.externalBaseUrl.trim()
        val endpoint = if (base.contains(":generateContent")) {
            base
        } else {
            "${base.trimEnd('/')}/v1beta/models/$model:generateContent"
        }
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 82, baos)
        val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

        val parts = JSONArray()
            .put(
                JSONObject().put(
                    "inlineData",
                    JSONObject().put("mimeType", "image/jpeg").put("data", b64)
                )
            )
            .put(JSONObject().put("text", prompt))

        val contents = JSONArray().put(JSONObject().put("parts", parts))
        val body = JSONObject().put("contents", contents)
        val req = Request.Builder()
            .url(endpoint)
            .addHeader("x-goog-api-key", config.externalApiKey.trim())
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(req).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException("Gemini REST HTTP ${resp.code}: ${raw.take(240)}")
            }
            val json = JSONObject(raw)
            return json.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
                .orEmpty()
                .trim()
        }
    }

    private fun sendLocalEdgePrompt(
        config: AiRequestConfig,
        systemPrompt: String,
        prompt: String,
        history: List<AiChatMessage>
    ): String {
        val base = config.edgeBaseUrl.trim().trimEnd('/')
        require(base.startsWith("http://") || base.startsWith("https://")) {
            "URL local inválida: debe comenzar por http:// o https://"
        }
        val model = config.edgeModel.trim().ifBlank { "llama3.2" }

        val messagesJson = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", systemPrompt))
            history.takeLast(6).forEach { msg ->
                put(
                    JSONObject()
                        .put("role", if (msg.isUser) "user" else "assistant")
                        .put("content", msg.text)
                )
            }
            put(JSONObject().put("role", "user").put("content", prompt))
        }

        val (endpointUrl, payloadJson) = when (config.edgeFormat) {
            LocalEdgeFormat.OLLAMA_CHAT -> {
                val url = if (base.endsWith("/api/chat")) base else "$base/api/chat"
                val json = JSONObject()
                    .put("model", model)
                    .put("messages", messagesJson)
                    .put("stream", false)
                url to json
            }

            LocalEdgeFormat.OPENAI_COMPAT -> {
                val url = when {
                    base.endsWith("/chat/completions") -> base
                    base.endsWith("/v1") -> "$base/chat/completions"
                    else -> "$base/v1/chat/completions"
                }
                val json = JSONObject()
                    .put("model", model)
                    .put("messages", messagesJson)
                    .put("stream", false)
                url to json
            }
        }

        val request = Request.Builder()
            .url(endpointUrl)
            .post(payloadJson.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("Edge HTTP ${response.code}: ${raw.take(240)}")
            }
            val parsed = JSONObject(raw)
            val text = when (config.edgeFormat) {
                LocalEdgeFormat.OLLAMA_CHAT ->
                    parsed.optJSONObject("message")?.optString("content").orEmpty()
                LocalEdgeFormat.OPENAI_COMPAT ->
                    parsed.optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("message")
                        ?.optString("content")
                        .orEmpty()
            }
            if (text.isBlank()) {
                throw IllegalStateException("El servidor local devolvió una respuesta vacía.")
            }
            return text.trim()
        }
    }

    private fun sendExternalApiPrompt(
        config: AiRequestConfig,
        prompt: String,
        systemPrompt: String = "Responda de forma seria, técnica y concisa en español.",
        history: List<AiChatMessage> = emptyList()
    ): String {
        val base = config.externalBaseUrl.trim()
        val apiKey = config.externalApiKey.trim()
        require(base.startsWith("http://") || base.startsWith("https://")) {
            "URL externa inválida."
        }
        require(apiKey.isNotEmpty()) {
            "Falta la API Key para el modo API Externa."
        }
        val model = config.externalModel.trim().ifBlank { "gpt-4o-mini" }

        return when (config.externalFormat) {
            ExternalApiFormat.OPENAI_COMPAT -> {
                val messagesJson = JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", systemPrompt))
                    history.takeLast(6).forEach { msg ->
                        put(
                            JSONObject()
                                .put("role", if (msg.isUser) "user" else "assistant")
                                .put("content", msg.text)
                        )
                    }
                    put(JSONObject().put("role", "user").put("content", prompt))
                }
                val body = JSONObject()
                    .put("model", model)
                    .put("messages", messagesJson)
                    .put("stream", false)

                val req = Request.Builder()
                    .url(base)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .post(body.toString().toRequestBody(jsonMediaType))
                    .build()

                client.newCall(req).execute().use { resp ->
                    val raw = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        throw IllegalStateException("API HTTP ${resp.code}: ${raw.take(240)}")
                    }
                    val json = JSONObject(raw)
                    val content = json.optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("message")
                        ?.optString("content")
                        .orEmpty()
                    if (content.isBlank()) throw IllegalStateException("Respuesta vacía de la API.")
                    content.trim()
                }
            }

            ExternalApiFormat.GEMINI_REST -> {
                val endpoint = if (base.contains(":generateContent")) {
                    base
                } else {
                    val trimmed = base.trimEnd('/')
                    "$trimmed/v1beta/models/$model:generateContent"
                }
                val contents = JSONArray().apply {
                    put(
                        JSONObject().put(
                            "parts",
                            JSONArray().put(JSONObject().put("text", "$systemPrompt\n\n$prompt"))
                        )
                    )
                }
                val body = JSONObject().put("contents", contents)
                val req = Request.Builder()
                    .url(endpoint)
                    .addHeader("x-goog-api-key", apiKey)
                    .post(body.toString().toRequestBody(jsonMediaType))
                    .build()

                client.newCall(req).execute().use { resp ->
                    val raw = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        throw IllegalStateException("Gemini REST HTTP ${resp.code}: ${raw.take(240)}")
                    }
                    val json = JSONObject(raw)
                    val content = json.optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")
                        .orEmpty()
                    if (content.isBlank()) throw IllegalStateException("Respuesta vacía de Gemini REST.")
                    content.trim()
                }
            }
        }
    }
}
