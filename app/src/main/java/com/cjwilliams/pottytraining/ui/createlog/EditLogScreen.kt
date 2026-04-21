package com.cjwilliams.pottytraining.ui.createlog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun EditLogScreen(
    onLogUpdated: () -> Unit,
    viewModel: PottyLogViewModel = hiltViewModel()
) {
    val note by viewModel.note.collectAsStateWithLifecycle()
    val isAccident by viewModel.isAccident.collectAsStateWithLifecycle()
    val type by viewModel.type.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.saveEvent.collect { result ->
            if (result is PottyLogViewModel.SaveResult.Updated) {
                onLogUpdated()
            }
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
        buttonText = "Update Log"
    )
}
