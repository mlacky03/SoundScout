package com.nikolaM.soundscout.ui.filter

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObjects
import com.nikolaM.soundscout.data.model.NoiseLevel
import com.nikolaM.soundscout.data.model.NoiseReport
import com.nikolaM.soundscout.data.model.NoiseType
import com.nikolaM.soundscout.data.repository.NoiseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.awaitClose

enum class DateFilterType {
    CREATION,
    INTERACTION
}
data class ReportFilters(
    val authorId: String? = null,
    val noiseTypes: List<NoiseType> = emptyList(),
    val noiseLevel: NoiseLevel? = null,
    val startDate: java.util.Date? = null,
    val endDate: java.util.Date? = null,

    val dateFilterType: DateFilterType = DateFilterType.CREATION,

    val radiusMeters: Int? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModel : ViewModel() {


    private val _filters = MutableStateFlow(ReportFilters())
    val filters = _filters.asStateFlow()

    private val _location = MutableStateFlow<Location?>(null)

    fun updateUserLocation(location: Location) {
        _location.value = location
    }

    val noiseReports: StateFlow<List<NoiseReport>> = _filters.flatMapLatest { currentFilters ->
        // Svaki put kad se filteri promene, ova funkcija će se ponovo pozvati
        // i napraviće se novi upit ka bazi
        NoiseRepository.getFilteredNoiseReports(currentFilters)
            .snapshotFlow() // Pretvara Firestore listener u Kotlin Flow
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setRadiusFilter(radius: Int?) {
        _filters.update { it.copy(radiusMeters = radius) }
    }

    fun setNoiseLevelFilter(noiseLevel: NoiseLevel?) {
        _filters.update { it.copy(noiseLevel = noiseLevel) }
    }

    // Funkcije koje UI poziva da bi promenio filtere
    fun setAuthorFilter(authorId: String?) {
        _filters.update { it.copy(authorId = authorId) }
    }

    fun setStartDate(date: java.util.Date?) {
        _filters.update { it.copy(startDate = date) }
    }

    fun setDateFilterType(type: DateFilterType) {
        _filters.update { it.copy(dateFilterType = type) }
    }

    fun setEndDate(date: java.util.Date?) {
        _filters.update { it.copy(endDate = date) }
    }

    fun applyFilters(newFilters: ReportFilters) {
        _filters.value = newFilters
    }

    fun clearFilters() {
        _filters.value = ReportFilters()
    }

    fun toggleNoiseTypeFilter(noiseType: NoiseType) {
        _filters.update { currentFilters ->
            val currentTypes = currentFilters.noiseTypes.toMutableList()
            if (currentTypes.contains(noiseType)) {
                currentTypes.remove(noiseType) // Ako već postoji, ukloni ga
            } else {
                currentTypes.add(noiseType) // Ako ne postoji, dodaj ga
            }
            currentFilters.copy(noiseTypes = currentTypes)
        }
    }

//    @OptIn(ExperimentalCoroutinesApi::class)
//    val finalVisibleReports: StateFlow<List<NoiseReport>> = combine(
//        noiseReports, // 1. Lista sa servera (filtrirana po atributima)
//        filters,      // 2. Naši filteri iz UI-ja
//        _location     // 3. Trenutna lokacija korisnika
//    ) { reports, currentFilters, myLocation ->
//
//        // Ako filter za radijus NIJE uključen, samo vrati listu sa servera
//        if (currentFilters.radiusMeters == null || myLocation == null) {
//            reports
//        } else {
//            // Ako JESTE uključen, uradi dodatno filtriranje po radijusu
//            val radius = currentFilters.radiusMeters
//            reports.filter { report ->
//                val reportLocation = Location("").apply {
//                    latitude = report.location.latitude
//                    longitude = report.location.longitude
//                }
//                myLocation.distanceTo(reportLocation) < radius
//            }
//        }
//    }.stateIn(
//        scope = viewModelScope,
//        started = SharingStarted.WhileSubscribed(5000),
//        initialValue = emptyList()
//    )
//}

    val finalVisibleReports: StateFlow<List<NoiseReport>> = combine(
        noiseReports,
        filters,
        _location
    ) { reports, currentFilters, myLocation ->
        if (currentFilters.radiusMeters != null && myLocation != null) {
            val radius = currentFilters.radiusMeters
            reports.filter { report ->
                val reportLocation = Location("").apply {
                    latitude = report.location.latitude
                    longitude = report.location.longitude
                }
                myLocation.distanceTo(reportLocation) < radius
            }
        } else {
            reports
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
}

// Mala pomoćna funkcija za konverziju
fun Query.snapshotFlow(): Flow<List<NoiseReport>> = callbackFlow {
    val listener = addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)
            return@addSnapshotListener
        }
        if (snapshot != null) {
            val reportsWithIds = snapshot.documents.mapNotNull { doc ->
                doc.toObject(NoiseReport::class.java)?.copy(id = doc.id)
            }
            Log.d("ViewModelDataFlow", "Snapshot received. Reports count: ${reportsWithIds.size}")
            trySend(reportsWithIds)
        }
    }
    awaitClose { listener.remove() }


}

