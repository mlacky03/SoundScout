package com.nikolaM.soundscout

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.nikolaM.soundscout.data.repository.AuthRepository
import com.nikolaM.soundscout.data.services.LocationService
import com.nikolaM.soundscout.ui.navigation.LoggedInNavGraph
import com.nikolaM.soundscout.ui.screens.auth.AuthRoot
import com.nikolaM.soundscout.ui.screens.auth.AuthViewModel
import com.nikolaM.soundscout.ui.theme.SoundScoutTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContent {

            SoundScoutTheme {
                val vm = remember { AuthViewModel() }
                val ui by vm.ui.collectAsState()


                if (ui.isLoggedIn) {
                    LoggedInNavGraph(
                        onLogout = {
                            val intent = Intent(this, LocationService::class.java).apply {
                                action = LocationService.ACTION_STOP
                            }
                            startService(intent)
                            AuthRepository.currentUid()?.let { AuthRepository.setUserOnlineStatus(it, false) }
                            vm.signOut()
                        }

                    )
                } else {

                    AuthRoot(vm = vm)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AuthRepository.currentUid()?.let { AuthRepository.setUserOnlineStatus(it, false) }
    }
}