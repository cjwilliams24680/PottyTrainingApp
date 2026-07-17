package com.cjwilliams.pottytraining

import app.cash.turbine.test
import com.cjwilliams.pottytraining.domain.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()
    private val viewModel = MainViewModel(authRepository)

    @Test
    fun `the session starts unknown so the login screen cannot flash before tokens load`() {
        assertEquals(SessionUiState.Unknown, viewModel.sessionUiState.value)
    }

    @Test
    fun `no stored session resolves to logged out`() = runTest {
        viewModel.sessionUiState.test {
            assertEquals(SessionUiState.Unknown, awaitItem())
            assertEquals(SessionUiState.LoggedOut, awaitItem())
        }
    }

    @Test
    fun `signing in swaps the session to logged in`() = runTest {
        viewModel.sessionUiState.test {
            assertEquals(SessionUiState.Unknown, awaitItem())
            assertEquals(SessionUiState.LoggedOut, awaitItem())

            authRepository.login("a@b.com", "hunter2")

            assertEquals(SessionUiState.LoggedIn, awaitItem())
        }
    }

    @Test
    fun `logging out swaps the session back so the user lands on login`() = runTest {
        authRepository.setLoggedIn(true)

        viewModel.sessionUiState.test {
            assertEquals(SessionUiState.Unknown, awaitItem())
            assertEquals(SessionUiState.LoggedIn, awaitItem())

            authRepository.logout()

            assertEquals(SessionUiState.LoggedOut, awaitItem())
        }
    }
}
