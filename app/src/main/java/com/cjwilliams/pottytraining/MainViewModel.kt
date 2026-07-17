package com.cjwilliams.pottytraining

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cjwilliams.pottytraining.domain.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface SessionUiState {
    /** Tokens are read from disk asynchronously; rendering Login first would flash the wrong UI. */
    data object Unknown : SessionUiState
    data object LoggedIn : SessionUiState
    data object LoggedOut : SessionUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    authRepository: AuthRepository
) : ViewModel() {

    val sessionUiState: StateFlow<SessionUiState> = authRepository.isLoggedIn
        .map { if (it) SessionUiState.LoggedIn else SessionUiState.LoggedOut }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SessionUiState.Unknown
        )
}
