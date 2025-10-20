package com.nikolaM.soundscout.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.nikolaM.soundscout.R // Može javiti grešku za R dok ne dodaš ikonicu
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.toObjects
import com.nikolaM.soundscout.data.model.NoiseReport
import com.nikolaM.soundscout.data.model.UserProfile
import com.nikolaM.soundscout.data.repository.AuthRepository
import com.nikolaM.soundscout.data.repository.AuthRepository.currentUid
import com.nikolaM.soundscout.data.repository.NoiseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.random.Random

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var allUsers = listOf<UserProfile>()
    private val notifiedUserIds = mutableSetOf<String>()
    private val NEARBY_RADIUS_METERS = 500f

    private var allNoiseReports = listOf<NoiseReport>()
    private val notifiedReportIds = mutableSetOf<String>()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // 'companion object' nam omogućava da pristupamo toku lokacija spolja
    companion object {
        private val _locationFlow = MutableStateFlow<Location?>(null)
        val locationFlow = _locationFlow.asStateFlow()

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                // Kada dobijemo novu lokaciju, ažuriramo naš StateFlow
                locationResult.lastLocation?.let { location ->
                    _locationFlow.value = location

                    AuthRepository.currentUid()?.let { uid ->
                        serviceScope.launch {
                            AuthRepository.updateUserLocation(uid, location)
                        }
                    }
                    checkForNearbyUsers(location)
                    checkForNearbyNoiseReports(location)
                }
            }
        }
        startListeningForUsers()
        startListeningForNoiseReports()
    }

    private fun startListeningForUsers() {
        AuthRepository.getActiveUsers().addSnapshotListener { snapshot, error ->
            if (error != null) {
                // TODO: Handle error
                return@addSnapshotListener
            }
            if (snapshot != null) {
                // ===== ISPRAVKA: Ručno mapiramo ID-jeve i za korisnike =====
                val usersWithIds = snapshot.documents.mapNotNull { doc ->
                    val user = doc.toObject(UserProfile::class.java)
                    // Firestore kao ID dokumenta u 'users' kolekciji koristi UID,
                    // pa ga samo kopiramo u naše 'uid' polje.
                    user?.copy(uid = doc.id)
                }
                allUsers = usersWithIds
                // =========================================================

                locationFlow.value?.let { myLocation ->
                    checkForNearbyUsers(myLocation)
                }
            }
        }
    }

    private fun startListeningForNoiseReports() {
        NoiseRepository.getFilteredNoiseReports().addSnapshotListener { snapshot, error ->
            if (error != null) {
                // TODO: Handle error
                return@addSnapshotListener
            }
            if (snapshot != null) {
                // ===== ISPRAVKA: Ručno mapiramo ID-jeve, kao što smo radili u UI-ju =====
                val reportsWithIds = snapshot.documents.mapNotNull { doc ->
                    val report = doc.toObject(NoiseReport::class.java)
                    // Pravimo kopiju objekta, ali sada mu postavljamo i ispravan ID iz dokumenta
                    report?.copy(id = doc.id)
                }
                allNoiseReports = reportsWithIds
                // ======================================================================

                // Sada kada allNoiseReports lista ima ispravne ID-jeve,
                // ova provera će raditi kako treba za svaki novi objekat.
                locationFlow.value?.let { myLocation ->
                    checkForNearbyNoiseReports(myLocation)
                }
            }
        }
    }

    private fun checkForNearbyNoiseReports(myLocation: Location) {
        // Prvo uzmemo ID trenutno ulogovanog korisnika
        val myUid = AuthRepository.currentUid() ?: return

        allNoiseReports.forEach { report ->
            // <<-- KLJUČNA IZMENA: Proveravamo da li je autor reporta neko drugi -->>
            if (report.userId != myUid) {

                // Ostatak koda se izvršava samo ako nismo mi autori
                val reportLocation = Location("").apply {
                    latitude = report.location.latitude
                    longitude = report.location.longitude
                }
                val distance = myLocation.distanceTo(reportLocation)

                if (distance < NEARBY_RADIUS_METERS && !notifiedReportIds.contains(report.id)) {
                    notifiedReportIds.add(report.id)
                    showNearbyReportNotification(report)
                }
            }
        }
    }

    // GLAVNA LOGIKA: Provera blizine
    private fun checkForNearbyUsers(myLocation: Location) {
        val myUid = AuthRepository.currentUid() ?: return

        allUsers.forEach { user ->
            if (user.uid != myUid) { // Ne proveravamo sami sebe
                user.lastKnownLocation?.let { geoPoint ->
                    val userLocation = Location("").apply {
                        latitude = geoPoint.latitude
                        longitude = geoPoint.longitude
                    }

                    val distance = myLocation.distanceTo(userLocation)

                    // Ako je korisnik u radijusu I ako mu nismo već poslali notifikaciju...
                    if (distance < NEARBY_RADIUS_METERS && !notifiedUserIds.contains(user.uid)) {
                        notifiedUserIds.add(user.uid) // Dodaj ga na listu "obaveštenih"
                        showNearbyUserNotification(user) // Prikaži notifikaciju
                    }
                    // Bonus: Ovde možeš dodati 'else' blok koji uklanja korisnika iz 'notifiedUserIds'
                    // ako se udalji, kako bi notifikacija mogla ponovo da se pošalje ako se vrati.
                }
            }
        }
    }

    // Funkcija koja pravi i prikazuje notifikaciju
    @SuppressLint("MissingPermission")
    private fun showNearbyUserNotification(user: UserProfile) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // Ako nemamo dozvolu za notifikacije, ne radi ništa.
            return
        }

        createNotificationChannel() // Kreiramo kanal i za ovu notifikaciju
        val notification = NotificationCompat.Builder(this, "location_channel")
            .setContentTitle("Korisnik u blizini!")
            .setContentText("${user.username} se nalazi u vašoj blizini.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true) // Notifikacija nestaje kad se klikne na nju
            .build()

        // Koristimo NotificationManagerCompat da prikažemo notifikaciju
        // Dajemo svakoj notifikaciji jedinstven ID da ne bi gazile jedna drugu
        NotificationManagerCompat.from(this).notify(Random.nextInt(), notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                // Postavi status na ONLINE
                currentUid()?.let { AuthRepository.setUserOnlineStatus(it, true) }
                startLocationTracking()
            }
            ACTION_STOP -> stopLocationTracking()
        }
        return START_NOT_STICKY // Servis se ne restartuje automatski ako ga sistem ugasi
    }

    @SuppressLint("MissingPermission") // Dozvolu proveravamo pre pozivanja servisa
    private fun startLocationTracking() {
        createNotificationChannel()
        val notification = createNotification()
        startForeground(1, notification) // Obavezno za Foreground Service

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000) // Svakih 10 sekundi
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(5000) // Najmanji interval 5 sekundi
            .build()

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    @SuppressLint("MissingPermission")
    private fun showNearbyReportNotification(report: NoiseReport) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // Ako nemamo dozvolu za notifikacije, ne radi ništa.
            return
        }
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "location_channel")
            .setContentTitle("Nova prijava buke u blizini!")
            .setContentText("Nivo: ${report.noiseLevel}, Tip: ${report.noiseType}")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        // IZMENA: Koristimo report.id.hashCode() umesto Random.nextInt()
        NotificationManagerCompat.from(this).notify(report.id.hashCode(), notification)
    }
    private fun stopLocationTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        // Proveravamo da li je verzija Androida OREO (API 26) ili novija
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("location_channel", "Location Tracking", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        // Na starijim verzijama, ovaj kod se jednostavno preskoči, jer kanali ne postoje.
        // Notifikacija će se i dalje prikazati, ali bez kanala.
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "location_channel")
            .setContentTitle("SoundScout prati lokaciju")
            .setContentText("Aplikacija je aktivna u pozadini.")
            // Ovde moraš da imaš neku ikonicu u drawable folderu.
            // Privremeno možeš koristiti podrazumevanu launcher ikonicu.
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }
}