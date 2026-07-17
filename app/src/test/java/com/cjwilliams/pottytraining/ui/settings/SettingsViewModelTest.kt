package com.cjwilliams.pottytraining.ui.settings

import com.cjwilliams.pottytraining.MainDispatcherRule
import com.cjwilliams.pottytraining.domain.FakeAuthRepository
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()
    private val viewModel = SettingsViewModel(authRepository)

    @Test
    fun `logout clears the session`() = runTest {
        authRepository.setLoggedIn(true)

        viewModel.logout()
        advanceUntilIdle()

        assertEquals(1, authRepository.logoutCount)
        assertFalse(viewModel.isLoggingOut.value)
    }

    @Test
    fun `the button is disabled while logging out`() = runTest {
        viewModel.logout()

        assertTrue(viewModel.isLoggingOut.value)

        advanceUntilIdle()
        assertFalse(viewModel.isLoggingOut.value)
    }

    @Test
    fun `tapping logout twice only logs out once`() = runTest {
        viewModel.logout()
        viewModel.logout()
        advanceUntilIdle()

        assertEquals(1, authRepository.logoutCount)
    }
}
