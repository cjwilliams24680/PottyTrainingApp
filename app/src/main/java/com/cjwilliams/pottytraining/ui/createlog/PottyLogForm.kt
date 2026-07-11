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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cjwilliams.pottytraining.R
import com.cjwilliams.pottytraining.domain.PottyType

@Composable
fun PottyLogForm(
    uiState: PottyLogUiState.Loaded,
    onNoteChange: (String) -> Unit,
    onAccidentChange: (Boolean) -> Unit,
    onTypeChange: (PottyType) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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
                    selected = (uiState.type == pottyType),
                    onClick = { onTypeChange(pottyType) }
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
                checked = uiState.isAccident,
                onCheckedChange = onAccidentChange
            )
            Text(text = stringResource(R.string.is_accident_label))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.note,
            onValueChange = onNoteChange,
            label = { Text(stringResource(R.string.note_hint)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth()
        ) {
            val buttonText = if (uiState.isEditMode) {
                stringResource(R.string.update_log_button)
            } else {
                stringResource(R.string.create_log_button)
            }
            Text(buttonText)
        }
    }
}
