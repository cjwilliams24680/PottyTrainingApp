package com.cjwilliams.pottytraining.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cjwilliams.pottytraining.domain.PottyLog
import com.cjwilliams.pottytraining.domain.PottyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: PottyRepository
) : ViewModel() {

    private val dateFormatter = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())

    val groupedLogs: StateFlow<Map<String, List<PottyLog>>> = repository.getLogs()
        .map { logs ->
            logs.groupBy { log ->
                dateFormatter.format(Date(log.timestamp))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    fun deleteLog(log: PottyLog) {
        viewModelScope.launch {
            repository.deleteLog(log)
        }
    }
}
