package com.example.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiChatMessage
import com.example.data.AiConnectorRepository
import com.example.data.AiRequestConfig
import com.example.data.AiThemeAnimationProposal
import com.example.model.AiBackendMode
import com.example.model.ClockPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Streaming(val partialText: String) : AiUiState
    data class Error(val message: String) : AiUiState
}

sealed interface AiProposalState {
    data object Idle : AiProposalState
    data object Generating : AiProposalState
    data class Ready(val proposal: AiThemeAnimationProposal) : AiProposalState
    data class Error(val message: String) : AiProposalState
}

class AiConnectorViewModel(
    private val repository: AiConnectorRepository = AiConnectorRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    private val _proposalState = MutableStateFlow<AiProposalState>(AiProposalState.Idle)
    val proposalState: StateFlow<AiProposalState> = _proposalState.asStateFlow()

    private val _messages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val messages: StateFlow<List<AiChatMessage>> = _messages.asStateFlow()

    private val _connectionStatus = MutableStateFlow<String?>(null)
    val connectionStatus: StateFlow<String?> = _connectionStatus.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    fun testConnection(config: AiRequestConfig) {
        viewModelScope.launch {
            _isTestingConnection.value = true
            _connectionStatus.value = null
            val res = repository.testConnection(config)
            _isTestingConnection.value = false
            res.onSuccess { msg ->
                _connectionStatus.value = msg
            }.onFailure { err ->
                _connectionStatus.value = "Error: ${err.localizedMessage ?: "No se pudo conectar"}"
            }
        }
    }

    fun generateThemeAndAnimation(
        context: Context,
        config: AiRequestConfig,
        preferences: ClockPreferences,
        directive: String = ""
    ) {
        if (_proposalState.value is AiProposalState.Generating) return
        viewModelScope.launch {
            _proposalState.value = AiProposalState.Generating
            val result = repository.generateThemeAndAnimation(
                context = context,
                config = config,
                preferences = preferences,
                directive = directive
            )
            result.onSuccess { proposal ->
                _proposalState.value = AiProposalState.Ready(proposal)
            }.onFailure { err ->
                _proposalState.value = AiProposalState.Error(
                    err.localizedMessage ?: "No se pudo generar la animación según el contexto actual."
                )
            }
        }
    }

    fun dismissProposal() {
        _proposalState.value = AiProposalState.Idle
    }

    fun sendMessage(prompt: String, config: AiRequestConfig) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isEmpty() || _uiState.value is AiUiState.Loading || _uiState.value is AiUiState.Streaming) {
            return
        }

        val backendLabel = when (config.mode) {
            AiBackendMode.LOCAL_EDGE -> "EDGE · ${config.edgeModel}"
            AiBackendMode.EXTERNAL_API -> "API · ${config.externalModel}"
            AiBackendMode.FIREBASE_GEMINI -> "CLOUD · ${config.firebaseGeminiModel}"
        }

        val userMsg = AiChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = cleanPrompt,
            backendTag = "OPERADOR"
        )
        val historySnapshot = _messages.value
        _messages.update { it + userMsg }

        viewModelScope.launch {
            _uiState.value = AiUiState.Loading
            var finalReply = ""
            try {
                repository.generateReplyStream(config, cleanPrompt, historySnapshot).collect { accumulated ->
                    finalReply = accumulated
                    _uiState.value = AiUiState.Streaming(accumulated)
                }
                if (finalReply.isNotBlank()) {
                    val aiMsg = AiChatMessage(
                        id = UUID.randomUUID().toString(),
                        isUser = false,
                        text = finalReply,
                        backendTag = backendLabel
                    )
                    _messages.update { it + aiMsg }
                    _uiState.value = AiUiState.Idle
                } else {
                    _uiState.value = AiUiState.Error("El modelo no devolvió contenido.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI request timed out", e)
                _uiState.value = AiUiState.Error("Tiempo de espera agotado (60s). Verifique la conexión o el modelo.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI request failed", e)
                _uiState.value = AiUiState.Error(e.localizedMessage ?: "Fallo al consultar el modelo de IA.")
            }
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
        _uiState.value = AiUiState.Idle
    }

    private companion object {
        const val TAG = "AiConnectorVM"
    }
}
