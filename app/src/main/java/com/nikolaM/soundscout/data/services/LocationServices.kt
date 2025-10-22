package com.nikolaM.soundscout.data.services

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
import com.nikolaM.soundscout.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
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
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val usersWithIds = snapshot.documents.mapNotNull { doc ->
                    val user = doc.toObject(UserProfile::class.java)
                    user?.copy(uid = doc.id)
                }
                allUsers = usersWithIds

                locationFlow.value?.let { myLocation ->
                    checkForNearbyUsers(myLocation)
                }
            }
        }
    }

    private fun startListeningForNoiseReports() {
        NoiseRepository.getFilteredNoiseReports().addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val reportsWithIds = snapshot.documents.mapNotNull { doc ->
                    val report = doc.toObject(NoiseReport::class.java)
                    report?.copy(id = doc.id)
                }
                allNoiseReports = reportsWithIds

                locationFlow.value?.let { myLocation ->
                    checkForNearbyNoiseReports(myLocation)
                }
            }
        }
    }

    private fun checkForNearbyNoiseReports(myLocation: Location) {
        val myUid = AuthRepository.currentUid() ?: return

        allNoiseReports.forEach { report ->
            if (report.userId != myUid) {
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

    private fun checkForNearbyUsers(myLocation: Location) {
        val myUid = AuthRepository.currentUid() ?: return

        allUsers.forEach { user ->
            if (user.uid != myUid) {
                user.lastKnownLocation?.let { geoPoint ->
                    val userLocation = Location("").apply {
                        latitude = geoPoint.latitude
                        longitude = geoPoint.longitude
                    }

                    val distance = myLocation.distanceTo(userLocation)


                    if (distance < NEARBY_RADIUS_METERS && !notifiedUserIds.contains(user.uid)) {
                        notifiedUserIds.add(user.uid)
                        showNearbyUserNotification(user)
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun showNearbyUserNotification(user: UserProfile) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

            return
        }

        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "location_channel")
            .setContentTitle("Korisnik u blizini!")
            .setContentText("${user.username} se nalazi u vašoj blizini.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(Random.nextInt(), notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentUid()?.let { AuthRepository.setUserOnlineStatus(it, true) }
                startLocationTracking()
            }
            ACTION_STOP -> stopLocationTracking()
        }
        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startLocationTracking() {
        createNotificationChannel()
        val notification = createNotification()
        startForeground(1, notification)

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(5000)
            .build()

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    @SuppressLint("MissingPermission")
    private fun showNearbyReportNotification(report: NoiseReport) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
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

        NotificationManagerCompat.from(this).notify(report.id.hashCode(), notification)
    }
    private fun stopLocationTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("location_channel", "Location Tracking", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "location_channel")
            .setContentTitle("SoundScout prati lokaciju")
            .setContentText("Aplikacija je aktivna u pozadini.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }
}