package com.cjwilliams.pottytraining.ui.createlog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cjwilliams.pottytraining.R
import com.cjwilliams.pottytraining.domain.PottyType

@Composable
fun CreateLogScreen(
    viewModel: CreateLogViewModel = hiltViewModel()
) {
    val note by viewModel.note.collectAsStateWithLifecycle()
    val isAccident by viewModel.isAccident.collectAsStateWithLifecycle()
    val type by viewModel.type.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.potty_type_label),
            style = MaterialTheme.typography.titleMedium
        )
        PottyType.entries.forEach { pottyType ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(
                    selected = (type == pottyType),
                    onClick = { viewModel.onTypeChange(pottyType) }
                )
                val label = when (pottyType) {
                    PottyType.PEE -> stringResource(R.string.potty_type_pee)
                    PottyType.POO -> stringResource(R.string.potty_type_poo)
                    PottyType.BOTH -> stringResource(R.string.potty_type_both)
                }
                Text(text = label)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = isAccident,
                onCheckedChange = viewModel::onAccidentChange
            )
            Text(text = stringResource(R.string.is_accident_label))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = note,
            onValueChange = viewModel::onNoteChange,
            label = { Text(stringResource(R.string.note_hint)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = viewModel::saveLog,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.create_log_button))
        }
    }
}
