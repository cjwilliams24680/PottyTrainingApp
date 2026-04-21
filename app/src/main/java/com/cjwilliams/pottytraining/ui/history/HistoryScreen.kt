package com.cjwilliams.pottytraining.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault()).format(Date(log.timestamp)),
                style = MaterialTheme.typography.titleMedium
            )
            if (log.note.isNotBlank()) {
                Text(
                    text = log.note,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
