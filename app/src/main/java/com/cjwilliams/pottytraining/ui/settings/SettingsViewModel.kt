package com.cjwilliams.pottytraining.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cjwilliams.pottytraining.domain.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _isLoggingOut = MutableStateFlow(false)
    val isLoggingOut: StateFlow<Boolean> = _isLoggingOut.asStateFlow()

    /**
     * No result handling on purpose: [AuthRepository.logout] clears the local session even
     * when the server call fails, and that is what swaps the UI back to the login screen.
     */
    fun logout() {
        if (_isLoggingOut.value) return

        _isLoggingOut.value = true
        viewModelScope.launch {
            authRepository.logout()
            _isLoggingOut.value = false
        }
    }
}
