package com.nikolaM.soundscout.ui.screens.home

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nikolaM.soundscout.ui.ranking.RankingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(navController: NavController) {
    val vm: RankingViewModel = viewModel()
    val users by vm.users.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rang Lista Korisnika") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            itemsIndexed(users) { index, user ->
                ListItem(
                    headlineContent = { Text("${index + 1}. ${user.username}") },
                    supportingContent = { Text("${user.name} ${user.surname}") },
                    trailingContent = { Text("${user.points} poena") }
                )
                HorizontalDivider()
            }
        }
    }
}