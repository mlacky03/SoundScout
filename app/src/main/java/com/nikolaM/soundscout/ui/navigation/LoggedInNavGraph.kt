package com.nikolaM.soundscout.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nikolaM.soundscout.ui.filter.MapViewModel
import com.nikolaM.soundscout.ui.home.ReportListScreen
import com.nikolaM.soundscout.ui.screens.home.LoggedInHome
import com.nikolaM.soundscout.ui.ranking.RankingScreen

object Routes {
    const val HOME = "home"
    const val RANKING = "ranking"
    const val REPORT_LIST = "report_list"

}

@Composable
fun LoggedInNavGraph(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val mapViewModel: MapViewModel = viewModel()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            LoggedInHome(navController = navController, onLogout = onLogout,mapViewModel)
        }
        composable(Routes.RANKING) {
            RankingScreen(navController = navController)
        }

        composable(Routes.REPORT_LIST) {
            ReportListScreen(navController = navController, vm = mapViewModel)
        }
    }
}