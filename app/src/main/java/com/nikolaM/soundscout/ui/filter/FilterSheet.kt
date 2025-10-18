package com.nikolaM.soundscout.ui.filter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nikolaM.soundscout.data.model.NoiseLevel
import com.nikolaM.soundscout.data.model.NoiseType
import com.nikolaM.soundscout.data.repository.AuthRepository
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    vm: MapViewModel,
    onApplyFilters: () -> Unit // Funkcija koja će biti pozvana da zatvori prozor
) {
    val currentFilters by vm.filters.collectAsState()

    // Stanja za prikazivanje kalendara
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // LazyColumn koristimo da bi sadržaj mogao da se skroluje ako ne staje na ekran
    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // Ako je filter po autoru već uključen, isključujemo ga. U suprotnom, uključujemo.
                        if (currentFilters.authorId == AuthRepository.currentUid()) {
                            vm.setAuthorFilter(null) // Poništi filter
                        } else {
                            vm.setAuthorFilter(AuthRepository.currentUid()) // Postavi filter na ID trenutnog korisnika
                        }
                    }
            ) {
                // Switch prikazuje da li je filter aktivan
                Switch(
                    checked = (currentFilters.authorId == AuthRepository.currentUid()),
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            vm.setAuthorFilter(AuthRepository.currentUid())
                        } else {
                            vm.setAuthorFilter(null)
                        }
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Prikaži samo moje prijave")
            }
        }

        // --- Filter za TIP BUKE (Multi-select sa Checkbox-ovima) ---
        item {
            Text("Tip Buke (može više opcija)", style = MaterialTheme.typography.titleMedium)
            NoiseType.values().forEach { type ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { vm.toggleNoiseTypeFilter(type) }
                ) {
                    Checkbox(
                        checked = currentFilters.noiseTypes.contains(type),
                        onCheckedChange = { vm.toggleNoiseTypeFilter(type) }
                    )
                    Text(text = type.name)
                }
            }
        }

        // --- Filter za JAČINU BUKE (Single-select sa RadioButton-ima) ---
        item {
            Text("Jačina Buke (samo jedna opcija)", style = MaterialTheme.typography.titleMedium)
            NoiseLevel.values().forEach { level ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { vm.setNoiseLevelFilter(level) }
                ) {
                    RadioButton(
                        selected = (currentFilters.noiseLevel == level),
                        onClick = { vm.setNoiseLevelFilter(level) }
                    )
                    Text(text = level.name)
                }
            }
        }

        // --- Filter za DATUM ---
        item {
            Text("Opseg Datuma", style = MaterialTheme.typography.titleMedium)

            // Izbor između datuma kreiranja i interakcije
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = currentFilters.dateFilterType == DateFilterType.CREATION,
                    onClick = { vm.setDateFilterType(DateFilterType.CREATION) }
                )
                Text("Datum Kreiranja")
                Spacer(modifier = Modifier.width(8.dp))
                RadioButton(
                    selected = currentFilters.dateFilterType == DateFilterType.INTERACTION,
                    onClick = { vm.setDateFilterType(DateFilterType.INTERACTION) }
                )
                Text("Datum Interakcije")
            }

            // Dugmad za izbor početnog i krajnjeg datuma
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showStartDatePicker = true }, modifier = Modifier.weight(1f)) {
                    Text(formatDate(currentFilters.startDate) ?: "OD")
                }
                Button(onClick = { showEndDatePicker = true }, modifier = Modifier.weight(1f)) {
                    Text(formatDate(currentFilters.endDate) ?: "DO")
                }
            }
        }

        // --- Dugmad za akciju ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { vm.clearFilters() }) {
                    Text("Poništi Filtere")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onApplyFilters) {
                    Text("Primeni")
                }
            }
        }
    }

    // --- Dijalozi za kalendar (DatePicker) ---
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    datePickerState.selectedDateMillis?.let { vm.setStartDate(Date(it)) }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    datePickerState.selectedDateMillis?.let { vm.setEndDate(Date(it)) }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// Pomoćna funkcija za formatiranje datuma za prikaz na dugmetu
private fun formatDate(date: Date?): String? {
    if (date == null) return null
    // Podesi format kako ti odgovara
    val format = SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault())
    return format.format(date)
}