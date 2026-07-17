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

data class SignupUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isSubmitting: Boolean = false,
    val error: AppError? = null
) {
    /** Only true once the user has typed something to confirm, so it can't fire while typing. */
    val passwordsMismatch: Boolean
        get() = confirmPassword.isNotBlank() && password != confirmPassword

    val canSubmit: Boolean
        get() = email.isNotBlank() &&
            password.isNotBlank() &&
            confirmPassword.isNotBlank() &&
            !passwordsMismatch &&
            !isSubmitting
}

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) = _uiState.update { it.copy(email = email) }

    fun onPasswordChange(password: String) = _uiState.update { it.copy(password = password) }

    fun onConfirmPasswordChange(confirmPassword: String) =
        _uiState.update { it.copy(confirmPassword = confirmPassword) }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    /**
     * Signing up returns a session, so success needs no navigation here: the stored tokens
     * flip [AuthRepository.isLoggedIn] and the session root swaps in the main app.
     */
    fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return

        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.signup(state.email.trim(), state.password)
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    error = (result as? AppResult.Error)?.error
                )
            }
        }
    }
}
