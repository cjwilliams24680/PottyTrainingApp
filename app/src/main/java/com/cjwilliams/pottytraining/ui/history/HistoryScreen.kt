package com.cjwilliams.pottytraining.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cjwilliams.pottytraining.domain.PottyLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onEditLog: (Int) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    var selectedLog by remember { mutableStateOf<PottyLog?>(null) }
    val sheetState = rememberModalBottomSheetState()

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(logs) { log ->
            PottyLogItem(
                log = log,
                onClick = { selectedLog = log }
            )
        }
    }

    if (selectedLog != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedLog = null },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Log Options",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                ListItem(
                    headlineContent = { Text("Edit Log") },
                    leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                    modifier = Modifier.clickable {
                        selectedLog?.let { onEditLog(it.id) }
                        selectedLog = null
                    }
                )
                ListItem(
                    headlineContent = { Text("Delete Log") },
                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable {
                        selectedLog?.let { viewModel.deleteLog(it) }
                        selectedLog = null
                    }
                )
            }
        }
    }
}

@Composable
fun PottyLogItem(
    log: PottyLog,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() },
        colors = if (log.isAccident) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = log.type.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (log.isAccident) {
                    Text(
                        text = "(Accident)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(log.timestamp)),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Text(
                text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(log.timestamp)),
                style = MaterialTheme.typography.bodySmall
            )
            if (log.note.isNotBlank()) {
                Spacer(modifier = Modifier.padding(top = 8.dp))
                Text(
                    text = log.note,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
