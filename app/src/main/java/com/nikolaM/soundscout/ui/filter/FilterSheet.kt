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

//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun FilterSheet(
//    vm: MapViewModel,
//    onApplyFilters: () -> Unit // Funkcija koja će biti pozvana da zatvori prozor
//) {
//    val currentFilters by vm.filters.collectAsState()
//
//    // Stanja za prikazivanje kalendara
//    var showStartDatePicker by remember { mutableStateOf(false) }
//    var showEndDatePicker by remember { mutableStateOf(false) }
//
//    // LazyColumn koristimo da bi sadržaj mogao da se skroluje ako ne staje na ekran
//    LazyColumn(
//        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
//        verticalArrangement = Arrangement.spacedBy(24.dp)
//    ) {
//        item {
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .clickable {
//                        // Ako je filter po autoru već uključen, isključujemo ga. U suprotnom, uključujemo.
//                        if (currentFilters.authorId == AuthRepository.currentUid()) {
//                            vm.setAuthorFilter(null) // Poništi filter
//                        } else {
//                            vm.setAuthorFilter(AuthRepository.currentUid()) // Postavi filter na ID trenutnog korisnika
//                        }
//                    }
//            ) {
//                // Switch prikazuje da li je filter aktivan
//                Switch(
//                    checked = (currentFilters.authorId == AuthRepository.currentUid()),
//                    onCheckedChange = { isChecked ->
//                        if (isChecked) {
//                            vm.setAuthorFilter(AuthRepository.currentUid())
//                        } else {
//                            vm.setAuthorFilter(null)
//                        }
//                    }
//                )
//                Spacer(modifier = Modifier.width(8.dp))
//                Text("Prikaži samo moje prijave")
//            }
//        }
//
//        // --- Filter za TIP BUKE (Multi-select sa Checkbox-ovima) ---
//        item {
//            Text("Tip Buke (može više opcija)", style = MaterialTheme.typography.titleMedium)
//            NoiseType.values().forEach { type ->
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    modifier = Modifier.fillMaxWidth().clickable { vm.toggleNoiseTypeFilter(type) }
//                ) {
//                    Checkbox(
//                        checked = currentFilters.noiseTypes.contains(type),
//                        onCheckedChange = { vm.toggleNoiseTypeFilter(type) }
//                    )
//                    Text(text = type.name)
//                }
//            }
//        }
//
//        // --- Filter za JAČINU BUKE (Single-select sa RadioButton-ima) ---
//        item {
//            Text("Jačina Buke (samo jedna opcija)", style = MaterialTheme.typography.titleMedium)
//            NoiseLevel.values().forEach { level ->
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    modifier = Modifier.fillMaxWidth().clickable {
//                        // Ako je kliknuti nivo već selektovan, pošalji null da se filter poništi.
//                        // U suprotnom, pošalji izabrani nivo.
//                        val newLevel = if (currentFilters.noiseLevel == level) null else level
//                        vm.setNoiseLevelFilter(newLevel)
//                    }
//                ) {
//                    RadioButton(
//                        selected = (currentFilters.noiseLevel == level),
//                        onClick = {
//                            // Ista logika kao gore
//                            val newLevel = if (currentFilters.noiseLevel == level) null else level
//                            vm.setNoiseLevelFilter(newLevel)
//                        }
//                    )
//                    Text(text = level.name)
//                }
//            }
//        }
//
//        // --- Filter za DATUM ---
//        item {
//            Text("Opseg Datuma", style = MaterialTheme.typography.titleMedium)
//
//            // Izbor između datuma kreiranja i interakcije
//            Row(verticalAlignment = Alignment.CenterVertically) {
//                RadioButton(
//                    selected = currentFilters.dateFilterType == DateFilterType.CREATION,
//                    onClick = { vm.setDateFilterType(DateFilterType.CREATION) }
//                )
//                Text("Datum Kreiranja")
//                Spacer(modifier = Modifier.width(8.dp))
//                RadioButton(
//                    selected = currentFilters.dateFilterType == DateFilterType.INTERACTION,
//                    onClick = { vm.setDateFilterType(DateFilterType.INTERACTION) }
//                )
//                Text("Datum Interakcije")
//            }
//
//            // Dugmad za izbor početnog i krajnjeg datuma
//            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                Button(onClick = { showStartDatePicker = true }, modifier = Modifier.weight(1f)) {
//                    Text(formatDate(currentFilters.startDate) ?: "OD")
//                }
//                Button(onClick = { showEndDatePicker = true }, modifier = Modifier.weight(1f)) {
//                    Text(formatDate(currentFilters.endDate) ?: "DO")
//                }
//                TextButton(onClick = {
//                    vm.setStartDate(null)
//                    vm.setEndDate(null)
//                }) {
//                    Text("Poništi")
//                }
//            }
//        }
//
//
//        item {
//            Column {
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    Text("Radijus pretrage", style = MaterialTheme.typography.titleMedium)
//                    // Dugme za poništavanje filtera po radijusu
//                    if (currentFilters.radiusMeters != null) {
//                        TextButton(onClick = { vm.setRadiusFilter(null) }) {
//                            Text("Poništi")
//                        }
//                    }
//                }
//
//                // Prikazujemo trenutnu vrednost (ili poruku da je isključeno)
//                Text(
//                    text = currentFilters.radiusMeters?.let { "${it / 1000f} km" } ?: "Isključeno",
//                    style = MaterialTheme.typography.bodySmall
//                )
//
//                Slider(
//                    value = currentFilters.radiusMeters?.toFloat() ?: 0f,
//                    onValueChange = { vm.setRadiusFilter(it.toInt()) },
//                    valueRange = 500f..5000f, // Od 500 metara do 5000 metara (5km)
//                    steps = 8 // (5000-500)/500 - 1 = 8 koraka. Ovo čini da klizač "skače" na svakih 500m.
//                )
//            }
//        }
//
//        item {
//            Row(
//                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
//                horizontalArrangement = Arrangement.End
//            ) {
//                TextButton(onClick = { vm.clearFilters() }) {
//                    Text("Poništi Filtere")
//                }
//                Spacer(modifier = Modifier.width(8.dp))
//                Button(onClick = onApplyFilters) {
//                    Text("Primeni")
//                }
//            }
//        }
//    }
//
//    // --- Dijalozi za kalendar (DatePicker) ---
//    if (showStartDatePicker) {
//        val datePickerState = rememberDatePickerState(
//            selectableDates = object : SelectableDates {
//                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
//                    // Ako je krajnji datum izabran, dozvoli samo datume PRE ili JEDNAKE njemu
//                    return currentFilters.endDate?.let { endDate ->
//                        utcTimeMillis <= endDate.time
//                    } ?: true // Ako nije, dozvoli sve
//                }
//            }
//        )
//        DatePickerDialog(
//            onDismissRequest = { showStartDatePicker = false },
//            confirmButton = {
//                Button(onClick = {
//                    datePickerState.selectedDateMillis?.let { vm.setStartDate(Date(it)) }
//                    showStartDatePicker = false
//                }) { Text("OK") }
//            },
//            dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") } }
//        ) {
//            DatePicker(state = datePickerState)
//        }
//    }
//
//    if (showEndDatePicker) {
//        val datePickerState = rememberDatePickerState(
//            selectableDates = object : SelectableDates {
//                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
//                    // Ako je početni datum izabran, dozvoli samo datume POSLE ili JEDNAKE njemu
//                    return currentFilters.startDate?.let { startDate ->
//                        utcTimeMillis >= startDate.time
//                    } ?: true // Ako nije, dozvoli sve
//                }
//            }
//        )
//        DatePickerDialog(
//            onDismissRequest = { showEndDatePicker = false },
//            confirmButton = {
//                Button(onClick = {
//                    datePickerState.selectedDateMillis?.let { vm.setEndDate(Date(it)) }
//                    showEndDatePicker = false
//                }) { Text("OK") }
//            },
//            dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") } }
//        ) {
//            DatePicker(state = datePickerState)
//        }
//    }
//}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    initialFilters: ReportFilters,
    onApply: (ReportFilters) -> Unit,
    onDismiss: () -> Unit,
    locationGranted: Boolean
) {
    var tempFilters by remember { mutableStateOf(initialFilters) }

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
                        // <<-- IZMENA: Sada menjamo samo LOKALNO STANJE 'tempFilters' -->>
                        val currentUid = AuthRepository.currentUid()
                        val newAuthorId = if (tempFilters.authorId == currentUid) null else currentUid
                        tempFilters = tempFilters.copy(authorId = newAuthorId)
                    }
            ) {
                Text("Prikaži samo moje prijave", modifier = Modifier.weight(1f))
                Switch(
                    checked = (tempFilters.authorId != null), // Provera je sada jednostavnija
                    onCheckedChange = { isChecked ->
                        // <<-- IZMENA: I ovde menjamo samo 'tempFilters' -->>
                        val newAuthorId = if (isChecked) AuthRepository.currentUid() else null
                        tempFilters = tempFilters.copy(authorId = newAuthorId)
                    }
                )
            }
        }

        // --- Filter za TIP BUKE (Multi-select sa Checkbox-ovima) ---
        item {
            Text("Tip Buke", style = MaterialTheme.typography.titleMedium)
            NoiseType.values().forEach { type ->
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

        // --- Filter za JAČINU BUKE (Single-select sa RadioButton-ima) ---
        item {
            Text("Jačina Buke", style = MaterialTheme.typography.titleMedium)
            NoiseLevel.values().forEach { level ->
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

        // --- Filter za DATUM ---
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

    // --- Dijalozi za kalendar (DatePicker) ---
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
                    datePickerState.selectedDateMillis?.let { tempFilters = tempFilters.copy(endDate = Date(it)) }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }
}



private fun formatDate(date: Date?): String? {
    if (date == null) return null
    // Podesi format kako ti odgovara
    val format = SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault())
    return format.format(date)
}