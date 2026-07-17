package com.cjwilliams.pottytraining.ui.createlog

import app.cash.turbine.test
import com.cjwilliams.pottytraining.MainDispatcherRule
import com.cjwilliams.pottytraining.domain.AppError
import com.cjwilliams.pottytraining.domain.FakePottyRepository
import com.cjwilliams.pottytraining.domain.PottyLog
import com.cjwilliams.pottytraining.domain.PottyType
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class PottyLogViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakePottyRepository()

    private fun createViewModel() = PottyLogViewModel(repository)

    @Test
    fun `a successful create-mode save resets the form for the next log`() = runTest {
        val viewModel = createViewModel()
        viewModel.initialize(null)
        viewModel.onNoteChange("in the car")
        viewModel.onAccidentChange(true)
        viewModel.onTypeChange(PottyType.POO)

        viewModel.saveEvent.test {
            viewModel.save()
            advanceUntilIdle()

            // The event carries what was saved, while the state is ready for a fresh entry —
            // the CreateLog back stack entry survives navigating away, so a stale form would
            // resurface when the user returns to the tab.
            assertTrue(awaitItem().isAccident)
            assertEquals(PottyLogUiState.Loaded(isEditMode = false), viewModel.uiState.value)
        }
    }

    @Test
    fun `a successful edit-mode save keeps the form state`() = runTest {
        repository.setLogs(listOf(EXISTING_LOG))
        val viewModel = createViewModel()
        viewModel.initialize(EXISTING_LOG.id)
        advanceUntilIdle()
        viewModel.onNoteChange("updated note")

        viewModel.saveEvent.test {
            viewModel.save()
            advanceUntilIdle()

            awaitItem()
            val state = viewModel.uiState.value as PottyLogUiState.Loaded
            assertTrue(state.isEditMode)
            assertEquals("updated note", state.note)
        }
    }

    @Test
    fun `a failed save keeps the form and emits no event`() = runTest {
        repository.saveError = AppError.Network
        val viewModel = createViewModel()
        viewModel.initialize(null)
        viewModel.onNoteChange("in the car")

        viewModel.saveEvent.test {
            viewModel.save()
            advanceUntilIdle()

            expectNoEvents()
            val state = viewModel.uiState.value as PottyLogUiState.Loaded
            assertEquals("in the car", state.note)
        }
    }

    private companion object {
        val EXISTING_LOG = PottyLog(
            id = "log-1",
            timestamp = Instant.parse("2026-07-15T12:00:00Z"),
            note = "original note",
            isAccident = false,
            type = PottyType.PEE
        )
    }
}
