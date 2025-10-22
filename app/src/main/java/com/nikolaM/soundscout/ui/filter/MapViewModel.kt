package com.nikolaM.soundscout.ui.filter

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.Query
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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _location = MutableStateFlow<Location?>(null)

    fun updateUserLocation(location: Location) {
        _location.value = location
    }

    val searchSuggestions: StateFlow<List<NoiseType>> = _searchQuery
        .debounce(300)
        .map { query ->
            if (query.isBlank()) {
                emptyList()
            } else {
                NoiseType.entries.filter { noiseType ->
                    noiseType.name.contains(query, ignoreCase = true)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    val noiseReports: StateFlow<List<NoiseReport>> = _filters.flatMapLatest { currentFilters ->

        NoiseRepository.getFilteredNoiseReports(currentFilters)
            .snapshotFlow()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )


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
                currentTypes.remove(noiseType)
            } else {
                currentTypes.add(noiseType)
            }
            currentFilters.copy(noiseTypes = currentTypes)
        }
    }

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

