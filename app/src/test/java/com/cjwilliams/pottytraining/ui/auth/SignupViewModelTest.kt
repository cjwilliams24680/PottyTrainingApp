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

class SignupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()
    private val viewModel = SignupViewModel(authRepository)

    private fun fillForm(
        email: String = "a@b.com",
        password: String = "hunter2",
        confirm: String = "hunter2"
    ) {
        viewModel.onEmailChange(email)
        viewModel.onPasswordChange(password)
        viewModel.onConfirmPasswordChange(confirm)
    }

    @Test
    fun `mismatched passwords block submission and flag the field`() {
        fillForm(confirm = "hunter3")

        assertTrue(viewModel.uiState.value.passwordsMismatch)
        assertFalse(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `no mismatch is reported before the confirm field is touched`() {
        viewModel.onEmailChange("a@b.com")
        viewModel.onPasswordChange("hunter2")

        // Every keystroke in the password field would otherwise look like a mismatch.
        assertFalse(viewModel.uiState.value.passwordsMismatch)
    }

    @Test
    fun `matching passwords allow submission`() {
        fillForm()

        assertFalse(viewModel.uiState.value.passwordsMismatch)
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `submit is blocked until every field has content`() {
        fillForm(email = "")

        assertFalse(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `a successful signup hands the credentials to the repository`() = runTest {
        fillForm()

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(listOf("a@b.com" to "hunter2"), authRepository.signupCalls)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `a rejected signup surfaces the error`() = runTest {
        authRepository.result = AppResult.Error(AppError.Auth)
        fillForm()

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(AppError.Auth, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `submitting a mismatched form never reaches the server`() = runTest {
        fillForm(confirm = "hunter3")

        viewModel.submit()
        advanceUntilIdle()

        assertTrue(authRepository.signupCalls.isEmpty())
    }

    @Test
    fun `dismissing the dialog clears the error`() = runTest {
        authRepository.result = AppResult.Error(AppError.Network)
        fillForm()
        viewModel.submit()
        advanceUntilIdle()

        viewModel.dismissError()

        assertNull(viewModel.uiState.value.error)
    }
}
