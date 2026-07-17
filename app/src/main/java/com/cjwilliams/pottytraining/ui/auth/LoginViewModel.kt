package com.cjwilliams.pottytraining.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cjwilliams.pottytraining.domain.AppError
import com.cjwilliams.pottytraining.domain.AppResult
import com.cjwilliams.pottytraining.domain.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val error: AppError? = null
) {
    val canSubmit: Boolean
        get() = email.isNotBlank() && password.isNotBlank() && !isSubmitting
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) = _uiState.update { it.copy(email = email) }

    fun onPasswordChange(password: String) = _uiState.update { it.copy(password = password) }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    /**
     * A successful sign in deliberately updates no state: storing the tokens flips
     * [AuthRepository.isLoggedIn], and the session root swaps this screen out.
     */
    fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return

        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.login(state.email.trim(), state.password)
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    error = (result as? AppResult.Error)?.error
                )
            }
        }
    }
}
