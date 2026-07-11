package com.cjwilliams.pottytraining.ui.createlog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cjwilliams.pottytraining.R

@Composable
fun PottyLogScreen(
    logId: Int? = null,
    onSaveSuccess: (PottyLogViewModel.SaveResult) -> Unit,
    viewModel: PottyLogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(logId) {
        viewModel.initialize(logId)
        viewModel.saveEvent.collect { result ->
            onSaveSuccess(result)
        }
    }

    when (val state = uiState) {
        is PottyLogUiState.Loading, is PottyLogUiState.Uninitialized -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is PottyLogUiState.Loaded -> {
            PottyLogForm(
                uiState = state,
                onNoteChange = viewModel::onNoteChange,
                onAccidentChange = viewModel::onAccidentChange,
                onTypeChange = viewModel::onTypeChange,
                onSave = viewModel::save
            )
        }
        is PottyLogUiState.BlockingError -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.error_loading_log))
            }
        }
    }
}
