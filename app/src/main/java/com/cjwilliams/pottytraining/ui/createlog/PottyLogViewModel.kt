package com.cjwilliams.pottytraining.ui.createlog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.cjwilliams.pottytraining.Route
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PottyLogViewModel @Inject constructor(
    private val repository: PottyRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val logId: Int? = try {
        savedStateHandle.toRoute<Route.EditLog>().logId
    } catch (e: Exception) {
        null
    }

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    private val _isAccident = MutableStateFlow(false)
    val isAccident: StateFlow<Boolean> = _isAccident.asStateFlow()

    private val _type = MutableStateFlow(PottyType.PEE)
    val type: StateFlow<PottyType> = _type.asStateFlow()

    private val _saveEvent = MutableSharedFlow<SaveResult>()
    val saveEvent: SharedFlow<SaveResult> = _saveEvent.asSharedFlow()

    private var originalLog: PottyLog? = null

    val isEditMode: Boolean get() = logId != null

    init {
        if (logId != null) {
            viewModelScope.launch {
                repository.getLogById(logId)?.let { log ->
                    originalLog = log
                    _note.value = log.note
                    _isAccident.value = log.isAccident
                    _type.value = log.type
                }
            }
        }
    }

    fun onNoteChange(newNote: String) {
        _note.value = newNote
    }

    fun onAccidentChange(isAccident: Boolean) {
        _isAccident.value = isAccident
    }

    fun onTypeChange(type: PottyType) {
        _type.value = type
    }

    fun save() {
        viewModelScope.launch {
            val isAccidentValue = _isAccident.value
            if (isEditMode && originalLog != null) {
                repository.updateLog(
                    originalLog!!.copy(
                        note = _note.value,
                        isAccident = isAccidentValue,
                        type = _type.value
                    )
                )
                _saveEvent.emit(SaveResult.Updated)
            } else {
                repository.addLog(
                    PottyLog(
                        timestamp = System.currentTimeMillis(),
                        note = _note.value,
                        isAccident = isAccidentValue,
                        type = _type.value
                    )
                )
                _saveEvent.emit(SaveResult.Created(isAccidentValue))
            }
        }
    }

    sealed interface SaveResult {
        data class Created(val isAccident: Boolean) : SaveResult
        data object Updated : SaveResult
    }
}
