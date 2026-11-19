package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CalendarEvent
import com.example.data.CalendarRepository
import com.example.data.EventCategory
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CalendarUiState {
    data object Loading : CalendarUiState
    data class Success(val events: List<CalendarEvent>) : CalendarUiState
    data class Error(val message: String) : CalendarUiState
}

class CalendarViewModel(
    private val repository: CalendarRepository,
    private val currentUserId: String
) : ViewModel() {

    val eventsState: StateFlow<CalendarUiState> = repository.observeEvents(currentUserId)
        .map<List<CalendarEvent>, CalendarUiState> { CalendarUiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing calendar events", rootError)
            emit(CalendarUiState.Error(rootError.localizedMessage ?: "Error al cargar agenda."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = CalendarUiState.Loading
        )

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun clearActionError() {
        _actionError.value = null
    }

    fun addEvent(
        title: String,
        dateKey: String,
        timeLabel: String,
        category: EventCategory,
        note: String,
        colorHex: String,
        onAdded: () -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            _actionError.value = null
            val result = repository.addEvent(
                title = title,
                dateKey = dateKey,
                timeLabel = timeLabel,
                category = category,
                note = note,
                colorHex = colorHex
            )
            _isSaving.value = false
            result.onSuccess {
                onAdded()
            }.onFailure { err ->
                _actionError.value = err.localizedMessage ?: "No se pudo agendar el evento."
            }
        }
    }

    fun toggleCompleted(eventId: String, completed: Boolean) {
        viewModelScope.launch {
            _actionError.value = null
            val result = repository.toggleCompleted(eventId, completed)
            result.onFailure { err ->
                _actionError.value = err.localizedMessage ?: "No se pudo actualizar el evento."
            }
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            _actionError.value = null
            val result = repository.deleteEvent(eventId)
            result.onFailure { err ->
                _actionError.value = err.localizedMessage ?: "No se pudo eliminar el evento."
            }
        }
    }

    private companion object {
        const val TAG = "CalendarViewModel"
    }
}
