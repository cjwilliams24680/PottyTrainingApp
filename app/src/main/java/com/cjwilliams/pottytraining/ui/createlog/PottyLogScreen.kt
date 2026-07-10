package com.cjwilliams.pottytraining.ui.createlog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cjwilliams.pottytraining.R

@Composable
fun PottyLogScreen(
    onSaveSuccess: (PottyLogViewModel.SaveResult) -> Unit,
    viewModel: PottyLogViewModel = hiltViewModel()
) {
    val note by viewModel.note.collectAsStateWithLifecycle()
    val isAccident by viewModel.isAccident.collectAsStateWithLifecycle()
    val type by viewModel.type.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.saveEvent.collect { result ->
            onSaveSuccess(result)
        }
    }

    PottyLogForm(
        note = note,
        onNoteChange = viewModel::onNoteChange,
        isAccident = isAccident,
        onAccidentChange = viewModel::onAccidentChange,
        type = type,
        onTypeChange = viewModel::onTypeChange,
        onSave = viewModel::save,
        buttonText = if (viewModel.isEditMode) stringResource(R.string.update_log_button) else stringResource(R.string.create_log_button)
    )
}
