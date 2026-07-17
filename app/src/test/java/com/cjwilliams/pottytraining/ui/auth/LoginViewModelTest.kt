package com.cjwilliams.pottytraining.ui.auth

import com.cjwilliams.pottytraining.MainDispatcherRule
import com.cjwilliams.pottytraining.domain.AppError
import com.cjwilliams.pottytraining.domain.AppResult
import com.cjwilliams.pottytraining.domain.FakeAuthRepository
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()
    private val viewModel = LoginViewModel(authRepository)

    private fun fillForm(email: String = "a@b.com", password: String = "hunter2") {
        viewModel.onEmailChange(email)
        viewModel.onPasswordChange(password)
    }

    @Test
    fun `submit is blocked until both fields have content`() {
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.onEmailChange("a@b.com")
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.onPasswordChange("hunter2")
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `a successful sign in leaves no error for the session swap to handle`() = runTest {
        fillForm()

        viewModel.submit()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertEquals(listOf("a@b.com" to "hunter2"), authRepository.loginCalls)
    }

    @Test
    fun `the email is trimmed before it reaches the server`() = runTest {
        fillForm(email = "  a@b.com  ")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals("a@b.com", authRepository.loginCalls.single().first)
    }

    @Test
    fun `rejected credentials surface as an auth error`() = runTest {
        authRepository.result = AppResult.Error(AppError.Auth)
        fillForm()

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(AppError.Auth, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `being offline surfaces as a network error`() = runTest {
        authRepository.result = AppResult.Error(AppError.Network)
        fillForm()

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(AppError.Network, viewModel.uiState.value.error)
    }

    @Test
    fun `the form is locked while the request is in flight`() = runTest {
        fillForm()

        viewModel.submit()

        // Still queued: the dispatcher hasn't run the coroutine yet.
        assertTrue(viewModel.uiState.value.isSubmitting)
        assertFalse(viewModel.uiState.value.canSubmit)

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `submitting twice only calls the server once`() = runTest {
        fillForm()

        viewModel.submit()
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(1, authRepository.loginCalls.size)
    }

    @Test
    fun `retrying clears the previous error`() = runTest {
        authRepository.result = AppResult.Error(AppError.Auth)
        fillForm()
        viewModel.submit()
        advanceUntilIdle()

        authRepository.result = AppResult.Success(Unit)
        viewModel.submit()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `dismissing the dialog clears the error`() = runTest {
        authRepository.result = AppResult.Error(AppError.Auth)
        fillForm()
        viewModel.submit()
        advanceUntilIdle()

        viewModel.dismissError()

        assertNull(viewModel.uiState.value.error)
    }
}
