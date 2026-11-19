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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.R
import com.example.data.AiChatMessage
import com.example.data.AiRequestConfig
import com.example.model.AiBackendMode
import com.example.model.ClockPreferences
import com.example.model.ExternalApiFormat
import com.example.model.LocalEdgeFormat
import com.example.model.TelemetryPushCategory
import com.example.ui.components.ChamferedHudShape
import com.example.ui.components.CurvedSciFiHudBanner
import com.example.ui.theme.InkCream
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalNekoPalette
import com.example.ui.theme.StageBorder
import com.example.ui.theme.StageCard
import com.example.ui.theme.StageVoid
import com.example.ui.theme.parseHexColorOrNull
import com.example.viewmodel.AiConnectorViewModel
import com.example.viewmodel.AiProposalState
import com.example.viewmodel.AiUiState

@Composable
fun AiConnectorScreen(
    preferences: ClockPreferences,
    onSelectBackendMode: (AiBackendMode) -> Unit,
    onSaveEdgeConfig: (String, String, LocalEdgeFormat) -> Unit,
    onSaveExternalApiConfig: (String, String, String, ExternalApiFormat) -> Unit,
    onSaveFirebaseModel: (String) -> Unit,
    onSaveAiSystemPrompt: (String) -> Unit,
    onApplyAiThemeProfile: (
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
    ) -> Unit,
    onLogAction: (TelemetryPushCategory, String, String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
    aiViewModel: AiConnectorViewModel = viewModel()
) {
    val palette = LocalNekoPalette.current
    val context = LocalContext.current
    val uiState by aiViewModel.uiState.collectAsStateWithLifecycle()
    val proposalState by aiViewModel.proposalState.collectAsStateWithLifecycle()
    val messages by aiViewModel.messages.collectAsStateWithLifecycle()
    val connectionStatus by aiViewModel.connectionStatus.collectAsStateWithLifecycle()
    val isTesting by aiViewModel.isTestingConnection.collectAsStateWithLifecycle()

    var configExpanded by remember { mutableStateOf(false) }
    var generatorExpanded by remember { mutableStateOf(true) }
    var promptInput by remember { mutableStateOf("") }
    var directiveInput by remember { mutableStateOf("") }
    var appliedConfirmation by remember { mutableStateOf(false) }

    var edgeUrl by remember(preferences.edgeBaseUrl) { mutableStateOf(preferences.edgeBaseUrl) }
    var edgeModel by remember(preferences.edgeModel) { mutableStateOf(preferences.edgeModel) }
    var edgeFormat by remember(preferences.edgeFormat) { mutableStateOf(preferences.edgeFormat) }

    var extUrl by remember(preferences.externalBaseUrl) { mutableStateOf(preferences.externalBaseUrl) }
    var extModel by remember(preferences.externalModel) { mutableStateOf(preferences.externalModel) }
    var extKey by remember(preferences.externalApiKey) { mutableStateOf(preferences.externalApiKey) }
    var extFormat by remember(preferences.externalFormat) { mutableStateOf(preferences.externalFormat) }

    var fbModel by remember(preferences.firebaseGeminiModel) { mutableStateOf(preferences.firebaseGeminiModel) }
    var systemPrompt by remember(preferences.aiSystemPrompt) { mutableStateOf(preferences.aiSystemPrompt) }

    val currentConfig = remember(
        preferences.aiBackendMode,
        edgeUrl,
        edgeModel,
        edgeFormat,
        extUrl,
        extModel,
        extKey,
        extFormat,
        fbModel,
        systemPrompt
    ) {
        AiRequestConfig(
            mode = preferences.aiBackendMode,
            edgeBaseUrl = edgeUrl,
            edgeModel = edgeModel,
            edgeFormat = edgeFormat,
            externalBaseUrl = extUrl,
            externalModel = extModel,
            externalApiKey = extKey,
            externalFormat = extFormat,
            firebaseGeminiModel = fbModel,
            systemPrompt = systemPrompt
        )
    }

    val hudShape = remember { ChamferedHudShape(14.dp) }
    val chipShape = remember { ChamferedHudShape(8.dp) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, uiState) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
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
                    text = stringResource(R.string.ai_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = InkCream
                )
                Row {
                    IconButton(onClick = { aiViewModel.clearChat() }) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = "Limpiar historial",
                            tint = InkMuted
                        )
                    }
                    IconButton(onClick = { configExpanded = !configExpanded }) {
                        Icon(
                            imageVector = if (configExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = "Configuración de enlace IA",
                            tint = palette.primaryGlow
                        )
                    }
                }
            }

            CurvedSciFiHudBanner(
                primaryText = when (preferences.aiBackendMode) {
                    AiBackendMode.LOCAL_EDGE -> "MOTOR EDGE LOCAL // $edgeModel"
                    AiBackendMode.EXTERNAL_API -> "CONECTOR API // $extModel"
                    AiBackendMode.FIREBASE_GEMINI -> "GEMINI CLOUD // $fbModel"
                },
                secondaryText = "KRONOS IA · SÍNTESIS DE ANIMACIONES LOTTIE & TEMA"
            )

            Spacer(modifier = Modifier.height(8.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val modes = listOf(
                    AiBackendMode.LOCAL_EDGE to R.string.ai_mode_edge,
                    AiBackendMode.EXTERNAL_API to R.string.ai_mode_api,
                    AiBackendMode.FIREBASE_GEMINI to R.string.ai_mode_cloud
                )
                modes.forEachIndexed { idx, (mode, labelRes) ->
                    SegmentedButton(
                        selected = preferences.aiBackendMode == mode,
                        onClick = { onSelectBackendMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(idx, modes.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = palette.primaryGlow,
                            activeContentColor = StageVoid,
                            inactiveContainerColor = StageCard.copy(alpha = 0.85f),
                            inactiveContentColor = InkCream,
                            activeBorderColor = palette.primaryGlow,
                            inactiveBorderColor = StageBorder
                        ),
                        modifier = Modifier.testTag("ai_mode_${mode.id}")
                    ) {
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    }
                }
            }

            if (configExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = hudShape,
                    colors = CardDefaults.cardColors(containerColor = StageCard.copy(alpha = 0.86f)),
                    border = BorderStroke(1.dp, palette.secondaryGlow.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (preferences.aiBackendMode) {
                            AiBackendMode.LOCAL_EDGE -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Memory,
                                        contentDescription = null,
                                        tint = palette.secondaryGlow,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Servidor Edge Local (Ollama / LM Studio / llama.cpp)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = palette.secondaryGlow
                                    )
                                }

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    item {
                                        FilterChip(
                                            selected = edgeFormat == LocalEdgeFormat.OLLAMA_CHAT,
                                            onClick = {
                                                edgeFormat = LocalEdgeFormat.OLLAMA_CHAT
                                                if (edgeUrl.contains("1234")) edgeUrl = "http://127.0.0.1:11434"
                                                onSaveEdgeConfig(edgeUrl, edgeModel, edgeFormat)
                                            },
                                            label = { Text("Ollama :11434", fontSize = 11.sp) }
                                        )
                                    }
                                    item {
                                        FilterChip(
                                            selected = edgeFormat == LocalEdgeFormat.OPENAI_COMPAT,
                                            onClick = {
                                                edgeFormat = LocalEdgeFormat.OPENAI_COMPAT
                                                if (edgeUrl.contains("11434")) edgeUrl = "http://10.0.2.2:1234"
                                                onSaveEdgeConfig(edgeUrl, edgeModel, edgeFormat)
                                            },
                                            label = { Text("OpenAI Compat :1234", fontSize = 11.sp) }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = edgeUrl,
                                    onValueChange = {
                                        edgeUrl = it
                                        onSaveEdgeConfig(edgeUrl, edgeModel, edgeFormat)
                                    },
                                    label = { Text("URL del servidor local / LAN") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = palette.primaryGlow,
                                        unfocusedBorderColor = StageBorder,
                                        focusedTextColor = InkCream,
                                        unfocusedTextColor = InkCream
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("ai_edge_url_input")
                                )

                                OutlinedTextField(
                                    value = edgeModel,
                                    onValueChange = {
                                        edgeModel = it
                                        onSaveEdgeConfig(edgeUrl, edgeModel, edgeFormat)
                                    },
                                    label = { Text("Modelo local (ej. llama3.2, qwen2.5:3b)") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = palette.primaryGlow,
                                        unfocusedBorderColor = StageBorder,
                                        focusedTextColor = InkCream,
                                        unfocusedTextColor = InkCream
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("ai_edge_model_input")
                                )
                            }

                            AiBackendMode.EXTERNAL_API -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Http,
                                        contentDescription = null,
                                        tint = palette.primaryGlow,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Endpoint API Corporativa (OpenAI / Groq / Gemini REST)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = palette.primaryGlow
                                    )
                                }

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    item {
                                        FilterChip(
                                            selected = extFormat == ExternalApiFormat.OPENAI_COMPAT,
                                            onClick = {
                                                extFormat = ExternalApiFormat.OPENAI_COMPAT
                                                extUrl = "https://api.openai.com/v1/chat/completions"
                                                extModel = "gpt-4o-mini"
                                                onSaveExternalApiConfig(extUrl, extModel, extKey, extFormat)
                                            },
                                            label = { Text("OpenAI Compat", fontSize = 11.sp) }
                                        )
                                    }
                                    item {
                                        FilterChip(
                                            selected = extFormat == ExternalApiFormat.GEMINI_REST,
                                            onClick = {
                                                extFormat = ExternalApiFormat.GEMINI_REST
                                                extUrl = "https://generativelanguage.googleapis.com"
                                                extModel = "gemini-2.5-flash"
                                                onSaveExternalApiConfig(extUrl, extModel, extKey, extFormat)
                                            },
                                            label = { Text("Gemini REST", fontSize = 11.sp) }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = extUrl,
                                    onValueChange = {
                                        extUrl = it
                                        onSaveExternalApiConfig(extUrl, extModel, extKey, extFormat)
                                    },
                                    label = { Text("Endpoint URL") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = palette.primaryGlow,
                                        unfocusedBorderColor = StageBorder,
                                        focusedTextColor = InkCream,
                                        unfocusedTextColor = InkCream
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = extModel,
                                        onValueChange = {
                                            extModel = it
                                            onSaveExternalApiConfig(extUrl, extModel, extKey, extFormat)
                                        },
                                        label = { Text("Modelo") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = palette.primaryGlow,
                                            unfocusedBorderColor = StageBorder,
                                            focusedTextColor = InkCream,
                                            unfocusedTextColor = InkCream
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = extKey,
                                        onValueChange = {
                                            extKey = it
                                            onSaveExternalApiConfig(extUrl, extModel, extKey, extFormat)
                                        },
                                        label = { Text("API Key") },
                                        singleLine = true,
                                        visualTransformation = PasswordVisualTransformation(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = palette.primaryGlow,
                                            unfocusedBorderColor = StageBorder,
                                            focusedTextColor = InkCream,
                                            unfocusedTextColor = InkCream
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("ai_external_key_input")
                                    )
                                }
                            }

                            AiBackendMode.FIREBASE_GEMINI -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Cloud,
                                        contentDescription = null,
                                        tint = palette.secondaryGlow,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Firebase AI Logic · Análisis multimodal de fondo incluido",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = palette.secondaryGlow
                                    )
                                }
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(listOf("gemini-3.8-flash", "gemini-3.5-flash-lite")) { modelId ->
                                        FilterChip(
                                            selected = fbModel == modelId,
                                            onClick = {
                                                fbModel = modelId
                                                onSaveFirebaseModel(modelId)
                                            },
                                            label = { Text(modelId, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }

                        // Editable system prompt text box
                        OutlinedTextField(
                            value = systemPrompt,
                            onValueChange = {
                                systemPrompt = it
                                onSaveAiSystemPrompt(it)
                            },
                            label = { Text("Directriz base de sistema (editable)") },
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.primaryGlow,
                                unfocusedBorderColor = StageBorder,
                                focusedTextColor = InkCream,
                                unfocusedTextColor = InkCream
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_system_prompt_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { aiViewModel.testConnection(currentConfig) },
                                enabled = !isTesting,
                                shape = chipShape,
                                border = BorderStroke(1.dp, palette.secondaryGlow),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.secondaryGlow),
                                modifier = Modifier.testTag("ai_test_connection_button")
                            ) {
                                if (isTesting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = palette.secondaryGlow,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.WifiTethering,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.ai_test_connection), fontSize = 12.sp)
                            }
                        }

                        if (!connectionStatus.isNullOrBlank()) {
                            Text(
                                text = connectionStatus!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (connectionStatus!!.startsWith("Error:")) Color(0xFFFF6B4A) else palette.secondaryGlow
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Context-Aware Animation & Theme Synthesis Card
            Card(
                shape = hudShape,
                colors = CardDefaults.cardColors(containerColor = StageCard.copy(alpha = 0.88f)),
                border = BorderStroke(1.dp, palette.primaryGlow.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoFixHigh,
                                contentDescription = null,
                                tint = palette.primaryGlow,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Síntesis IA según Fondo & Preferencias",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.primaryGlow
                            )
                        }
                        TextButton(onClick = { generatorExpanded = !generatorExpanded }) {
                            Text(if (generatorExpanded) "Ocultar" else "Expandir", fontSize = 11.sp)
                        }
                    }

                    if (generatorExpanded) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = directiveInput,
                                onValueChange = { directiveInput = it },
                                placeholder = { Text("Directriz visual opcional (ej. telemetría azul)…", fontSize = 12.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = palette.primaryGlow,
                                    unfocusedBorderColor = StageBorder,
                                    focusedTextColor = InkCream,
                                    unfocusedTextColor = InkCream
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            Button(
                                onClick = {
                                    appliedConfirmation = false
                                    aiViewModel.generateThemeAndAnimation(
                                        context = context,
                                        config = currentConfig,
                                        preferences = preferences,
                                        directive = directiveInput
                                    )
                                    onLogAction(
                                        TelemetryPushCategory.AI,
                                        "Síntesis IA iniciada",
                                        "Analizando fondo y preferencias para generar perfil Lottie"
                                    )
                                },
                                enabled = proposalState !is AiProposalState.Generating,
                                shape = chipShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.primaryGlow,
                                    contentColor = StageVoid
                                ),
                                modifier = Modifier.testTag("ai_generate_theme_button")
                            ) {
                                if (proposalState is AiProposalState.Generating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = StageVoid,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Generar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        when (val ps = proposalState) {
                            is AiProposalState.Error -> {
                                Text(
                                    text = ps.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFF6B4A)
                                )
                            }

                            is AiProposalState.Ready -> {
                                val prop = ps.proposal
                                val composition by rememberLottieComposition(
                                    LottieCompositionSpec.JsonString(prop.lottieJson)
                                )
                                val progress by animateLottieCompositionAsState(
                                    composition = composition,
                                    iterations = LottieConstants.IterateForever
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(chipShape)
                                        .background(StageVoid.copy(alpha = 0.78f))
                                        .border(1.dp, StageBorder, chipShape)
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(chipShape)
                                            .background(StageVoid),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        LottieAnimation(
                                            composition = composition,
                                            progress = { progress },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = prop.themeName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = InkCream
                                        )
                                        Text(
                                            text = prop.analysis,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = InkMuted,
                                            maxLines = 2
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf(prop.primaryHex, prop.secondaryHex, prop.eyeHex).forEach { hex ->
                                                val dotColor = parseHexColorOrNull(hex) ?: palette.primaryGlow
                                                Box(
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .clip(CircleShape)
                                                        .background(dotColor)
                                                )
                                            }
                                            Text(
                                                text = "Lottie .JSON · ${prop.animationPattern}",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 10.sp,
                                                color = palette.secondaryGlow
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { aiViewModel.dismissProposal() }) {
                                        Text("Descartar", color = InkMuted, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = {
                                            onApplyAiThemeProfile(
                                                prop.primaryHex,
                                                prop.secondaryHex,
                                                prop.eyeHex,
                                                prop.lottieJson,
                                                prop.starfield,
                                                prop.particleStream,
                                                prop.auroraNebula,
                                                prop.anamorphicFlare,
                                                prop.gravityHorizon,
                                                prop.cinematicScanlines
                                            )
                                            appliedConfirmation = true
                                        },
                                        shape = chipShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = palette.secondaryGlow,
                                            contentColor = StageVoid
                                        ),
                                        modifier = Modifier.testTag("ai_apply_proposal_button")
                                    ) {
                                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (appliedConfirmation) "Aplicado al HUD y Eye" else "Aplicar al HUD y Eye",
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            else -> Unit
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Formal Tech Quick Prompts
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(
                    listOf(
                        "Estructurar protocolo operativo de enfoque 50/10",
                        "Sugerir paleta HEX corporativa de alto contraste",
                        "Optimizar bloques de agenda ejecutiva diaria"
                    )
                ) { quickPrompt ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            promptInput = quickPrompt
                            aiViewModel.sendMessage(quickPrompt, currentConfig)
                        },
                        shape = chipShape,
                        label = { Text(quickPrompt, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = StageCard.copy(alpha = 0.78f),
                            labelColor = InkCream
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Chat Feed
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubbleCard(msg = msg)
                }

                when (val s = uiState) {
                    AiUiState.Loading -> {
                        item {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = palette.primaryGlow,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Procesando inferencia técnica…", color = InkMuted, fontSize = 12.sp)
                            }
                        }
                    }

                    is AiUiState.Streaming -> {
                        item {
                            ChatBubbleCard(
                                msg = AiChatMessage(
                                    id = "streaming",
                                    isUser = false,
                                    text = s.partialText,
                                    backendTag = "STREAMING"
                                )
                            )
                        }
                    }

                    is AiUiState.Error -> {
                        item {
                            Card(
                                shape = hudShape,
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B1018)),
                                border = BorderStroke(1.dp, Color(0xFFFF6B4A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = s.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFFB4A2),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    AiUiState.Idle -> Unit
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Composer Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    placeholder = { Text(stringResource(R.string.ai_input_hint), fontSize = 13.sp) },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.primaryGlow,
                        unfocusedBorderColor = StageBorder,
                        focusedTextColor = InkCream,
                        unfocusedTextColor = InkCream
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_prompt_input")
                )

                Button(
                    onClick = {
                        val toSend = promptInput
                        promptInput = ""
                        aiViewModel.sendMessage(toSend, currentConfig)
                        onLogAction(TelemetryPushCategory.AI, "Consulta IA enviada", toSend.take(64))
                    },
                    enabled = promptInput.isNotBlank() && uiState !is AiUiState.Loading && uiState !is AiUiState.Streaming,
                    shape = chipShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primaryGlow,
                        contentColor = StageVoid
                    ),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("ai_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.ai_send),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleCard(msg: AiChatMessage) {
    val palette = LocalNekoPalette.current
    val accent = if (msg.isUser) palette.primaryGlow else palette.secondaryGlow
    val shape = remember { ChamferedHudShape(12.dp) }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            shape = shape,
            color = if (msg.isUser) palette.primaryGlow.copy(alpha = 0.16f) else StageCard.copy(alpha = 0.88f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = msg.backendTag,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = accent
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = msg.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkCream
                )
            }
        }
    }
}
