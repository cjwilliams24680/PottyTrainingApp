package com.cjwilliams.pottytraining.ui.history

import app.cash.turbine.test
import com.cjwilliams.pottytraining.MainDispatcherRule
import com.cjwilliams.pottytraining.domain.AppError
import com.cjwilliams.pottytraining.domain.AppResult
import com.cjwilliams.pottytraining.domain.FakePottyRepository
import com.cjwilliams.pottytraining.domain.PottyLog
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakePottyRepository()

    /**
     * Built inside each test rather than in a field: the view model refreshes from its `init`
     * block, which needs the main dispatcher the rule installs after construction would run.
     */
    private fun createViewModel() = HistoryViewModel(repository)

    @Test
    fun `the first refresh with an empty cache shows loading, not the empty state`() = runTest {
        repository.refreshGate = CompletableDeferred()
        val viewModel = createViewModel()

        viewModel.uiState.test {
            advanceUntilIdle()
            assertEquals(HistoryUiState.Loading, expectMostRecentItem())
        }
    }

    @Test
    fun `cached logs render grouped by day while the first refresh is still in flight`() = runTest {
        repository.setLogs(listOf(log("1", DAY_ONE), log("2", DAY_ONE), log("3", DAY_TWO)))
        repository.refreshGate = CompletableDeferred()
        val viewModel = createViewModel()

        viewModel.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem() as HistoryUiState.Loaded

            assertTrue(state.isRefreshing)
            assertEquals(2, state.groupedLogs.size)
            assertEquals(listOf(2, 1), state.groupedLogs.values.map { it.size })
        }
    }

    @Test
    fun `a successful refresh with no logs shows the empty state`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            advanceUntilIdle()
            assertEquals(HistoryUiState.Loaded(emptyMap(), isRefreshing = false), expectMostRecentItem())
        }
    }

    @Test
    fun `a refresh failure with nothing cached blocks the screen`() = runTest {
        repository.refreshResult = AppResult.Error(AppError.Network)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            advanceUntilIdle()
            assertEquals(HistoryUiState.BlockingError(AppError.Network), expectMostRecentItem())
        }
    }

    @Test
    fun `a refresh failure with cached logs keeps the list and reports the error once`() = runTest {
        repository.setLogs(listOf(log("1", DAY_ONE)))
        repository.refreshResult = AppResult.Error(AppError.Network)
        // Hold the refresh open so the event can't fire before this test subscribes to it.
        val gate = CompletableDeferred<Unit>()
        repository.refreshGate = gate
        val viewModel = createViewModel()

        viewModel.refreshErrorEvents.test {
            advanceUntilIdle()
            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(AppError.Network, awaitItem())
            expectNoEvents()
        }

        viewModel.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem() as HistoryUiState.Loaded

            assertEquals(1, state.groupedLogs.size)
            assertFalse(state.isRefreshing)
        }
    }

    @Test
    fun `a refresh failure on an empty but already-loaded history reports the error`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // The first refresh succeeded with no logs, so the empty state is on screen. A later
        // failure has to speak up rather than leave the pull gesture looking like a no-op.
        repository.refreshResult = AppResult.Error(AppError.Network)

        viewModel.refreshErrorEvents.test {
            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(AppError.Network, awaitItem())
        }

        viewModel.uiState.test {
            advanceUntilIdle()
            assertEquals(HistoryUiState.Loaded(emptyMap(), isRefreshing = false), expectMostRecentItem())
        }
    }

    @Test
    fun `refreshing while a refresh is already in flight is a no-op`() = runTest {
        repository.refreshGate = CompletableDeferred()
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(1, repository.refreshCallCount)
    }

    @Test
    fun `retrying after a blocking error recovers`() = runTest {
        repository.refreshResult = AppResult.Error(AppError.Network)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            advanceUntilIdle()
            assertEquals(HistoryUiState.BlockingError(AppError.Network), expectMostRecentItem())

            repository.refreshResult = AppResult.Success(Unit)
            repository.setLogs(listOf(log("1", DAY_ONE)))
            viewModel.refresh()
            advanceUntilIdle()

            val state = expectMostRecentItem() as HistoryUiState.Loaded
            assertEquals(1, state.groupedLogs.size)
            assertFalse(state.isRefreshing)
        }
    }

    @Test
    fun `a delete the server doesn't support yet reports the coming-soon event`() = runTest {
        repository.deleteResult = AppResult.Error(AppError.NotSupported)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.deleteNotSupportedEvents.test {
            viewModel.deleteLog(log("1", DAY_ONE))
            advanceUntilIdle()

            awaitItem()
            assertEquals(listOf("1"), repository.deletedLogs.map { it.id })
        }
    }

    @Test
    fun `a successful delete stays quiet`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.deleteNotSupportedEvents.test {
            viewModel.deleteLog(log("1", DAY_ONE))
            advanceUntilIdle()

            expectNoEvents()
        }
    }

    private fun log(id: String, timestamp: Instant) = PottyLog(id = id, timestamp = timestamp)

    private companion object {
        // Midday so the grouping lands on distinct days regardless of the test machine's zone.
        val DAY_ONE: Instant = Instant.parse("2026-07-15T12:00:00Z")
        val DAY_TWO: Instant = Instant.parse("2026-07-16T12:00:00Z")
    }
}
