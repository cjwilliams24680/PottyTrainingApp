package com.cjwilliams.pottytraining.ui.createlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cjwilliams.pottytraining.domain.PottyLog
import com.cjwilliams.pottytraining.domain.PottyRepository
import com.cjwilliams.pottytraining.domain.PottyType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateLogViewModel @Inject constructor(
    private val repository: PottyRepository
) : ViewModel() {

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    private val _isAccident = MutableStateFlow(false)
    val isAccident: StateFlow<Boolean> = _isAccident.asStateFlow()

    private val _type = MutableStateFlow(PottyType.PEE)
    val type: StateFlow<PottyType> = _type.asStateFlow()

    private val _logSavedEvent = MutableSharedFlow<Boolean>()
    val logSavedEvent: SharedFlow<Boolean> = _logSavedEvent.asSharedFlow()

    fun onNoteChange(newNote: String) {
        _note.value = newNote
    }

    fun onAccidentChange(isAccident: Boolean) {
        _isAccident.value = isAccident
    }

    fun onTypeChange(type: PottyType) {
        _type.value = type
    }

    fun saveLog() {
        viewModelScope.launch {
            val isAccidentValue = _isAccident.value
            repository.addLog(
                PottyLog(
                    timestamp = System.currentTimeMillis(),
                    note = _note.value,
                    isAccident = isAccidentValue,
                    type = _type.value
                )
            )
            _note.value = ""
            _isAccident.value = false
            _type.value = PottyType.PEE
            _logSavedEvent.emit(isAccidentValue)
        }
    }
}
