package com.cjwilliams.pottytraining.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(logs) { log ->
            PottyLogItem(log)
        }
    }
}

@Composable
fun PottyLogItem(log: PottyLog) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
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
