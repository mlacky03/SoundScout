package com.nikolaM.soundscout.ui.screens.auth

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController


object AuthRoutes {
    const val LANDING = "auth_landing"
    const val REGISTRATION = "auth_registration"
}

@Composable
fun AuthRoot(vm: AuthViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AuthRoutes.LANDING
    ) {

        composable(AuthRoutes.LANDING) {
            AuthScreen(
                vm = vm,
                onNavigateToRegistration = {
                    navController.navigate(AuthRoutes.REGISTRATION)
                }
            )
        }


        composable(AuthRoutes.REGISTRATION) {
            RegistrationScreen(
                vm = vm,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
