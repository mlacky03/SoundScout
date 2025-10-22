package com.nikolaM.soundscout.ui.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.toObjects
import com.nikolaM.soundscout.data.model.UserProfile
import com.nikolaM.soundscout.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RankingViewModel : ViewModel() {

    private val _users = MutableStateFlow<List<UserProfile>>(emptyList())
    val users = _users.asStateFlow()

    init {
        loadLeaderboard()
    }

    private fun loadLeaderboard() {
        viewModelScope.launch {
            AuthRepository.getLeaderboard(limit = 100).addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    _users.value = snapshot.toObjects()
                }

            }
        }
    }
}