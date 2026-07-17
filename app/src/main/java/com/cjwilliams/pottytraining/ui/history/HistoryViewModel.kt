package com.cjwilliams.pottytraining.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cjwilliams.pottytraining.domain.AppError
import com.cjwilliams.pottytraining.domain.AppResult
import com.cjwilliams.pottytraining.domain.PottyLog
import com.cjwilliams.pottytraining.domain.PottyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

sealed interface HistoryUiState {
    /** The cache is empty and the first refresh hasn't landed yet, so "no logs" would be a lie. */
    data object Loading : HistoryUiState

    /** The cached logs, which may be none. An empty map here means there really are no logs. */
    data class Loaded(
        val groupedLogs: Map<String, List<PottyLog>>,
        val isRefreshing: Boolean = false
    ) : HistoryUiState

    /** Nothing cached to fall back on and the refresh failed. */
    data class BlockingError(val error: AppError) : HistoryUiState
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: PottyRepository
) : ViewModel() {

    private val dateFormatter = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())

    /**
     * [lastError] is only set when a refresh failed with nothing on screen to fall back on — that
     * is, before any refresh has ever landed. Once one has, the screen keeps whatever it shows
     * (logs, or a confirmed-empty history) and failures go out as [refreshErrorEvents] instead.
     */
    private data class RefreshStatus(
        val inFlight: Boolean = false,
        val hasSucceededOnce: Boolean = false,
        val lastError: AppError? = null
    )

    private val refreshStatus = MutableStateFlow(RefreshStatus())

    private val _refreshErrorEvents = MutableSharedFlow<AppError>()
    val refreshErrorEvents: SharedFlow<AppError> = _refreshErrorEvents.asSharedFlow()

    /** Fires when a delete is rejected because the server doesn't support it yet. */
    private val _deleteNotSupportedEvents = MutableSharedFlow<Unit>()
    val deleteNotSupportedEvents: SharedFlow<Unit> = _deleteNotSupportedEvents.asSharedFlow()

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.getLogs(),
        refreshStatus
    ) { logs, refresh ->
        when {
            logs.isNotEmpty() -> HistoryUiState.Loaded(
                groupedLogs = logs.groupBy { dateFormatter.format(Date.from(it.timestamp)) },
                isRefreshing = refresh.inFlight
            )
            refresh.hasSucceededOnce -> HistoryUiState.Loaded(emptyMap(), refresh.inFlight)
            refresh.lastError != null -> HistoryUiState.BlockingError(refresh.lastError)
            else -> HistoryUiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState.Loading
    )

    init {
        refresh()
    }

    fun refresh() {
        if (refreshStatus.value.inFlight) return
        refreshStatus.update { it.copy(inFlight = true, lastError = null) }
        viewModelScope.launch {
            when (val result = repository.refreshLogs()) {
                is AppResult.Success ->
                    refreshStatus.update { it.copy(inFlight = false, hasSucceededOnce = true) }

                is AppResult.Error -> {
                    refreshStatus.update { it.copy(inFlight = false) }
                    // Stale logs beat an error screen, and so does a confirmed-empty history:
                    // both are worth keeping on screen, so only block when there's nothing at all.
                    val screenHasContent = refreshStatus.value.hasSucceededOnce ||
                        repository.getLogs().first().isNotEmpty()
                    if (screenHasContent) {
                        _refreshErrorEvents.emit(result.error)
                    } else {
                        refreshStatus.update { it.copy(lastError = result.error) }
                    }
                }
            }
        }
    }

    fun deleteLog(log: PottyLog) {
        viewModelScope.launch {
            val result = repository.deleteLog(log)
            if (result is AppResult.Error && result.error == AppError.NotSupported) {
                _deleteNotSupportedEvents.emit(Unit)
            }
        }
    }
}
