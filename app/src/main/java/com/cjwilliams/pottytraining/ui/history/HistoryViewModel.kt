package com.cjwilliams.pottytraining.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cjwilliams.pottytraining.domain.PottyLog
import com.cjwilliams.pottytraining.domain.PottyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    repository: PottyRepository
) : ViewModel() {

    val logs: StateFlow<List<PottyLog>> = repository.getLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
