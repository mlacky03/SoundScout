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
    initialFilters: ReportFilters,
    onApply: (ReportFilters) -> Unit,
    onDismiss: () -> Unit,
    locationGranted: Boolean
) {
    var tempFilters by remember { mutableStateOf(initialFilters) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }


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

                        val currentUid = AuthRepository.currentUid()
                        val newAuthorId = if (tempFilters.authorId == currentUid) null else currentUid
                        tempFilters = tempFilters.copy(authorId = newAuthorId)
                    }
            ) {
                Text("Prikaži samo moje prijave", modifier = Modifier.weight(1f))
                Switch(
                    checked = (tempFilters.authorId != null),
                    onCheckedChange = { isChecked ->

                        val newAuthorId = if (isChecked) AuthRepository.currentUid() else null
                        tempFilters = tempFilters.copy(authorId = newAuthorId)
                    }
                )
            }
        }


        item {
            Text("Tip Buke", style = MaterialTheme.typography.titleMedium)
            NoiseType.entries.forEach { type ->
                Row(modifier = Modifier.fillMaxWidth().clickable {
                    val currentTypes = tempFilters.noiseTypes.toMutableList()
                    if (currentTypes.contains(type)) currentTypes.remove(type) else currentTypes.add(type)
                    tempFilters = tempFilters.copy(noiseTypes = currentTypes)
                }) {
                    Checkbox(
                        checked = tempFilters.noiseTypes.contains(type),
                        onCheckedChange = { isChecked ->
                            val currentTypes = tempFilters.noiseTypes.toMutableList()
                            if (isChecked) currentTypes.add(type) else currentTypes.remove(type)
                            tempFilters = tempFilters.copy(noiseTypes = currentTypes)
                        }
                    )
                    Text(text = type.name)
                }
            }
        }


        item {
            Text("Jačina Buke", style = MaterialTheme.typography.titleMedium)
            NoiseLevel.entries.forEach { level ->
                Row(modifier = Modifier.fillMaxWidth().clickable {
                    val newLevel = if (tempFilters.noiseLevel == level) null else level
                    tempFilters = tempFilters.copy(noiseLevel = newLevel)
                }) {
                    RadioButton(
                        selected = (tempFilters.noiseLevel == level),
                        onClick = {
                            val newLevel = if (tempFilters.noiseLevel == level) null else level
                            tempFilters = tempFilters.copy(noiseLevel = newLevel)
                        }
                    )
                    Text(text = level.name)
                }
            }
        }


        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Opseg Datuma", style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(modifier = Modifier.weight(1f).clickable { tempFilters = tempFilters.copy(dateFilterType = DateFilterType.CREATION) }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = tempFilters.dateFilterType == DateFilterType.CREATION, onClick = { tempFilters = tempFilters.copy(dateFilterType = DateFilterType.CREATION) })
                        Text("Kreiranja")
                    }
                    Row(modifier = Modifier.weight(1f).clickable { tempFilters = tempFilters.copy(dateFilterType = DateFilterType.INTERACTION) }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = tempFilters.dateFilterType == DateFilterType.INTERACTION, onClick = { tempFilters = tempFilters.copy(dateFilterType = DateFilterType.INTERACTION) })
                        Text("Interakcije")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { tempFilters = tempFilters.copy(startDate = null, endDate = null) }) { Text("Poništi") }
                    Button(onClick = { showStartDatePicker = true }, modifier = Modifier.weight(1f)) { Text(formatDate(tempFilters.startDate) ?: "OD") }
                    Button(onClick = { showEndDatePicker = true }, modifier = Modifier.weight(1f)) { Text(formatDate(tempFilters.endDate) ?: "DO") }
                }
            }
        }

        if (locationGranted) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Radijus pretrage", style = MaterialTheme.typography.titleMedium)
                        if (tempFilters.radiusMeters != null) {
                            TextButton(onClick = {
                                tempFilters = tempFilters.copy(radiusMeters = null)
                            }) {
                                Text("Poništi")
                            }
                        }
                    }
                    Text(
                        text = tempFilters.radiusMeters?.let { "${it / 1000f} km" } ?: "Isključeno",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = tempFilters.radiusMeters?.toFloat() ?: 0f,
                        onValueChange = {
                            tempFilters = tempFilters.copy(radiusMeters = it.toInt())
                        },
                        valueRange = 500f..5000f,
                        steps = 8
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { tempFilters = ReportFilters() }) { Text("Resetuj Sve") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { onApply(tempFilters) }) { Text("Primeni") }
            }
        }
    }


    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return tempFilters.endDate?.let { utcTimeMillis <= it.time } ?: true
            }
        })
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    datePickerState.selectedDateMillis?.let { tempFilters = tempFilters.copy(startDate = Date(it)) }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return tempFilters.startDate?.let { utcTimeMillis >= it.time } ?: true
            }
        })
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    //datePickerState.selectedDateMillis?.let { tempFilters = tempFilters.copy(endDate = Date(it)) }
                    datePickerState.selectedDateMillis?.let { millis ->



                        val selectedDate = Date(millis)


                        val calendar = Calendar.getInstance()
                        calendar.time = selectedDate
                        calendar.set(Calendar.HOUR_OF_DAY, 23)
                        calendar.set(Calendar.MINUTE, 59)
                        calendar.set(Calendar.SECOND, 59)


                        tempFilters = tempFilters.copy(endDate = calendar.time)
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }
}



private fun formatDate(date: Date?): String? {
    if (date == null) return null
    val format = SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault())
    return format.format(date)
}