package com.nikolaM.soundscout.ui.filter

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
    val authorId: String? = null, // Autor je i dalje samo jedan
    val noiseTypes: List<NoiseType> = emptyList(), // <<-- IZMENA
    val noiseLevel: NoiseLevel? = null, // <<-- IZMENA
    val startDate: java.util.Date? = null,
    val endDate: java.util.Date? = null,

    val dateFilterType: DateFilterType = DateFilterType.CREATION
)

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModel : ViewModel() {


    private val _filters = MutableStateFlow(ReportFilters())
    val filters = _filters.asStateFlow()


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

