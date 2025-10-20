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
import com.nikolaM.soundscout.services.LocationService
import com.nikolaM.soundscout.ui.navigation.LoggedInNavGraph
import com.nikolaM.soundscout.ui.screens.home.LoggedInHome
import com.nikolaM.soundscout.ui.screens.auth.AuthRoot
import com.nikolaM.soundscout.ui.screens.auth.AuthViewModel
import com.nikolaM.soundscout.ui.theme.SoundScoutTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContent {
            // Uključujemo temu koju smo definisali za celu aplikaciju
            SoundScoutTheme {
                // Kreiramo instancu AuthViewModel-a.
                // 'remember' osigurava da ViewModel "preživi" promene na ekranu.
                val vm = remember { AuthViewModel() }

                // Pratimo stanje (UI state) iz ViewModel-a.
                // 'ui' će sadržati informaciju da li je korisnik ulogovan ili ne.
                val ui by vm.ui.collectAsState()

                // GLAVNA LOGIKA APLIKACIJE: Šta prikazati korisniku?
                if (ui.isLoggedIn) {
                    // AKO JE KORISNIK ULOGOVAN:
                    // Prikazujemo HomeScreen i prosleđujemo mu funkciju za odjavu.
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
                    // AKO KORISNIK NIJE ULOGOVAN:
                    // Prikazujemo AuthRoot, koji sadrži ekrane za prijavu i registraciju.
                    AuthRoot(vm = vm)
                }
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        // Ako Android uništi aplikaciju, pokušaj da javiš serveru da smo offline
        // Ovo nije 100% garantovano da će se izvršiti, ali je najbolja praksa
        AuthRepository.currentUid()?.let { AuthRepository.setUserOnlineStatus(it, false) }
    }
}