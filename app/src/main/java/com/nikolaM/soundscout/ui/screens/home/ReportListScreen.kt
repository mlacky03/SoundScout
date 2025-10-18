package com.nikolaM.soundscout.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.nikolaM.soundscout.ui.filter.DateFilterType
import com.nikolaM.soundscout.ui.filter.MapViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportListScreen(navController: NavController, vm: MapViewModel) {
    // Koristimo ISTI ViewModel i uzimamo ISTU listu izveštaja
    val noiseReports by vm.noiseReports.collectAsState()
    val filters by vm.filters.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lista Prijava") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            items(noiseReports) { report ->
                val dateField = if (filters.dateFilterType == DateFilterType.INTERACTION) {
                    report.lastInteractionTimestamp
                } else {
                    report.timestamp
                }

                ListItem(
                    headlineContent = { Text("Tip: ${report.noiseType} | Nivo: ${report.noiseLevel} | Prijavio: ${report.username} ") },
                    //supportingContent = { Text("Prijavio: ${report.username}") },
                    trailingContent = { Text(formatDateForList(dateField)) }
                )
                Divider()
            }
        }
    }
}

private fun formatDateForList(date: Date?): String {
    if (date == null) return "N/A"
    val format = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())
    return format.format(date)
}