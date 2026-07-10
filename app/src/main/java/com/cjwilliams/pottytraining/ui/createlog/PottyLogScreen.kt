package com.cjwilliams.pottytraining.ui.createlog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cjwilliams.pottytraining.R

@Composable
fun PottyLogScreen(
    onSaveSuccess: (PottyLogViewModel.SaveResult) -> Unit,
    viewModel: PottyLogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.saveEvent.collect { result ->
            onSaveSuccess(result)
        }
    }

    PottyLogForm(
        uiState = uiState,
        onNoteChange = viewModel::onNoteChange,
        onAccidentChange = viewModel::onAccidentChange,
        onTypeChange = viewModel::onTypeChange,
        onSave = viewModel::save
    )
}
