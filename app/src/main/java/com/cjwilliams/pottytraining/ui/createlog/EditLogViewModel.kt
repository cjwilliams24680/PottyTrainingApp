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
class EditLogViewModel @Inject constructor(
    private val repository: PottyRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val editLogRoute = savedStateHandle.toRoute<Route.EditLog>()
    private val logId = editLogRoute.logId

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    private val _isAccident = MutableStateFlow(false)
    val isAccident: StateFlow<Boolean> = _isAccident.asStateFlow()

    private val _type = MutableStateFlow(PottyType.PEE)
    val type: StateFlow<PottyType> = _type.asStateFlow()

    private val _logUpdatedEvent = MutableSharedFlow<Unit>()
    val logUpdatedEvent: SharedFlow<Unit> = _logUpdatedEvent.asSharedFlow()

    private var originalLog: PottyLog? = null

    init {
        viewModelScope.launch {
            repository.getLogById(logId)?.let { log ->
                originalLog = log
                _note.value = log.note
                _isAccident.value = log.isAccident
                _type.value = log.type
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

    fun updateLog() {
        viewModelScope.launch {
            originalLog?.let { log ->
                repository.updateLog(
                    log.copy(
                        note = _note.value,
                        isAccident = _isAccident.value,
                        type = _type.value
                    )
                )
                _logUpdatedEvent.emit(Unit)
            }
        }
    }
}
