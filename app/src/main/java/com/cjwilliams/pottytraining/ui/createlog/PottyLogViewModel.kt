package com.cjwilliams.pottytraining.ui.createlog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cjwilliams.pottytraining.domain.PottyLog
import com.cjwilliams.pottytraining.domain.PottyRepository
import com.cjwilliams.pottytraining.domain.PottyType
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
import javax.inject.Inject

data class PottyLogUiState(
    val note: String = "",
    val isAccident: Boolean = false,
    val type: PottyType = PottyType.PEE,
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class PottyLogViewModel @Inject constructor(
    private val repository: PottyRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val logId: Int? = savedStateHandle.get<Int>("logId")

    private val _uiState = MutableStateFlow(PottyLogUiState(isEditMode = logId != null))
    val uiState: StateFlow<PottyLogUiState> = _uiState.asStateFlow()

    private val _saveEvent = MutableSharedFlow<SaveResult>()
    val saveEvent: SharedFlow<SaveResult> = _saveEvent.asSharedFlow()

    private var originalLog: PottyLog? = null

    init {
        logId?.let { id ->
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                repository.getLogById(id).firstOrNull()?.let { log ->
                    originalLog = log
                    _uiState.update { 
                        it.copy(
                            note = log.note,
                            isAccident = log.isAccident,
                            type = log.type,
                            isLoading = false
                        )
                    }
                } ?: _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onNoteChange(newNote: String) {
        _uiState.update { it.copy(note = newNote) }
    }

    fun onAccidentChange(isAccident: Boolean) {
        _uiState.update { it.copy(isAccident = isAccident) }
    }

    fun onTypeChange(type: PottyType) {
        _uiState.update { it.copy(type = type) }
    }

    fun save() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val logToSave = if (currentState.isEditMode && originalLog != null) {
                originalLog!!.copy(
                    note = currentState.note,
                    isAccident = currentState.isAccident,
                    type = currentState.type
                )
            } else {
                PottyLog(
                    timestamp = System.currentTimeMillis(),
                    note = currentState.note,
                    isAccident = currentState.isAccident,
                    type = currentState.type
                )
            }
            repository.upsertLog(logToSave)
            _saveEvent.emit(SaveResult(currentState.isAccident))

            if (!currentState.isEditMode) {
                _uiState.update { 
                    it.copy(
                        note = "",
                        isAccident = false,
                        type = PottyType.PEE
                    )
                }
            }
        }
    }

    data class SaveResult(val isAccident: Boolean)
}
