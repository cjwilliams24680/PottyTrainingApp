package com.cjwilliams.pottytraining.ui.createlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cjwilliams.pottytraining.domain.AppResult
import com.cjwilliams.pottytraining.domain.PottyLog
import com.cjwilliams.pottytraining.domain.PottyRepository
import com.cjwilliams.pottytraining.domain.PottyType
import com.cjwilliams.pottytraining.ui.createlog.PottyLogUiState.Uninitialized
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

sealed interface PottyLogUiState {

    data object Uninitialized : PottyLogUiState
    data class Loading(val id: String?) : PottyLogUiState
    data class Loaded(
        val id: String? = null,
        val timestamp: Instant? = null,
        val note: String = "",
        val isAccident: Boolean = false,
        val type: PottyType = PottyType.PEE,
        val isEditMode: Boolean = false
    ) : PottyLogUiState
    data object BlockingError : PottyLogUiState
}

@HiltViewModel
class PottyLogViewModel @Inject constructor(
    private val repository: PottyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PottyLogUiState>(Uninitialized)
    val uiState: StateFlow<PottyLogUiState> = _uiState.asStateFlow()

    private val _saveEvent = MutableSharedFlow<SaveResult>()
    val saveEvent: SharedFlow<SaveResult> = _saveEvent.asSharedFlow()

    fun initialize(id: String?) {
        if (uiState.value != Uninitialized) return

        if (id != null) {
            _uiState.value = PottyLogUiState.Loading(id)
            viewModelScope.launch {
                repository.getLogById(id).firstOrNull()?.let { log ->
                    _uiState.value = PottyLogUiState.Loaded(
                        id = log.id,
                        note = log.note.orEmpty(),
                        isAccident = log.isAccident,
                        type = log.type,
                        isEditMode = true,
                        timestamp = log.timestamp,
                    )
                } ?: run {
                    _uiState.value = PottyLogUiState.BlockingError
                }
            }
        } else {
            _uiState.value = PottyLogUiState.Loaded(
                id = id,
                isEditMode = false,
            )
        }
    }

    fun onNoteChange(newNote: String) {
        updateLoadedState { it.copy(note = newNote) }
    }

    fun onAccidentChange(isAccident: Boolean) {
        updateLoadedState { it.copy(isAccident = isAccident) }
    }

    fun onTypeChange(type: PottyType) {
        updateLoadedState { it.copy(type = type) }
    }

    private fun updateLoadedState(update: (PottyLogUiState.Loaded) -> PottyLogUiState.Loaded) {
        _uiState.update { state ->
            if (state is PottyLogUiState.Loaded) update(state) else state
        }
    }

    fun save() {
        val currentState = _uiState.value
        if (currentState !is PottyLogUiState.Loaded) return

        viewModelScope.launch {
            val result = repository.saveLog(
                PottyLog(
                    id = currentState.id,
                    timestamp = currentState.timestamp ?: Instant.now(),
                    // An empty field means "no note", which the server stores as null.
                    note = currentState.note.takeIf { it.isNotBlank() },
                    isAccident = currentState.isAccident,
                    type = currentState.type
                )
            )

            // TODO: surface save failures in the UI phase; navigating away on an error
            // would tell the user the log was saved when it wasn't.
            if (result is AppResult.Success) {
                _saveEvent.emit(SaveResult(currentState.isAccident))
            } else {
                Timber.w("Save failed: %s", result)
            }
        }
    }

    data class SaveResult(val isAccident: Boolean)
}
