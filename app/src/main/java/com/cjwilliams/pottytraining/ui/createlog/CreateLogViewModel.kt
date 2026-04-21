package com.cjwilliams.pottytraining.ui.createlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cjwilliams.pottytraining.domain.PottyLog
import com.cjwilliams.pottytraining.domain.PottyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateLogViewModel @Inject constructor(
    private val repository: PottyRepository
) : ViewModel() {

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    fun onNoteChange(newNote: String) {
        _note.value = newNote
    }

    fun saveLog() {
        viewModelScope.launch {
            repository.addLog(
                PottyLog(
                    timestamp = System.currentTimeMillis(),
                    note = _note.value
                )
            )
            _note.value = "" // Reset note after saving
        }
    }
}
