package com.nikolaM.soundscout.ui.screens.home


import com.nikolaM.soundscout.R
import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.nikolaM.soundscout.services.LocationService
import com.nikolaM.soundscout.data.model.NoiseReport
import com.nikolaM.soundscout.data.repository.NoiseRepository
import com.nikolaM.soundscout.data.model.NoiseLevel
import com.nikolaM.soundscout.data.model.NoiseType
import com.nikolaM.soundscout.data.model.UserProfile
import kotlinx.coroutines.launch
import com.nikolaM.soundscout.data.repository.AuthRepository
import com.nikolaM.soundscout.ui.navigation.Routes
import com.nikolaM.soundscout.ui.util.bitmapDescriptorFromVector
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nikolaM.soundscout.ui.filter.FilterSheet
import com.nikolaM.soundscout.ui.filter.MapViewModel


@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LoggedInHome(navController: NavController, onLogout: () -> Unit, vm: MapViewModel) { // <-- 1. DODAJEMO onLogout KAO PARAMETAR
    val context = LocalContext.current



    val sheetState = rememberModalBottomSheetState()
    var showFilterSheet by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val permissionsList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS // <-- DODAJEMO DOZVOLU ZA NOTIFIKACIJE
        )
    } else {
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    val permissionState = rememberMultiplePermissionsState(
        permissions = permissionsList
    )

    LaunchedEffect(Unit) {
        permissionState.launchMultiplePermissionRequest()
    }

    // 2. KORISTIMO Scaffold DA LAKO DODAMO GORNJU TRAKU
    Scaffold(
        topBar = {
            // 3. TopAppBar SA NASLOVOM I DUGMETOM ZA ODJAVU
            TopAppBar(
                title = { Text("SoundScout Mapa") },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.REPORT_LIST) }) {
                        Icon(Icons.Default.List, contentDescription = "Prikaži listu")
                    }

                    IconButton(onClick = { navController.navigate(Routes.RANKING) }) {
                        Icon(Icons.Default.Leaderboard, contentDescription = "Rang lista")
                    }

                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filteri")
                    }

                    TextButton(onClick = onLogout) { // Pozivamo onLogout funkciju
                        Text("Odjava")
                    }
                }

            )
        }
    ) { paddingValues ->

        Box(modifier = Modifier.padding(paddingValues)) {
            when {
                permissionState.allPermissionsGranted -> {
                    LaunchedEffect(Unit) {
                        val intent = Intent(context, LocationService::class.java).apply {
                            action = LocationService.ACTION_START
                        }
                        context.startService(intent)
                    }
                    NoiseMap(vm=vm)
                }
                permissionState.shouldShowRationale -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Potrebna nam je dozvola za lokaciju za prikaz mape.")
                        Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                            Text("Dozvoli")
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Dozvola za lokaciju je odbijena. Molimo omogućite je u podešavanjima.")
                    }
                }
            }
        }
    }
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState
        ) {
            FilterSheet(
                vm = vm,
                onApplyFilters = {
                    // <<-- ISPRAVLJENA LINIJA -->>
                    coroutineScope.launch {
                        sheetState.hide()
                        if (!sheetState.isVisible) {
                            showFilterSheet = false
                        }
                    }
                }
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoiseMap(vm: MapViewModel) {

    val noiseReports by vm.noiseReports.collectAsState()


    // === Stanja koja pratimo (trenutna lokacija, prijave, korisnici, itd.) ===
    val location by LocationService.locationFlow.collectAsState()
    //var noiseReports by remember { mutableStateOf<List<NoiseReport>>(emptyList()) }
    var otherUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    var showAddNoiseDialog by remember { mutableStateOf(false) }

    // Stanja za Bottom Sheet (prozor sa detaljima prijave)
    var selectedReport by remember { mutableStateOf<NoiseReport?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    // === Logika za preuzimanje podataka sa servera ===

    // Slušamo promene u 'noise_reports' kolekciji u realnom vremenu
//    LaunchedEffect(Unit) {
//        NoiseRepository.getAllNoiseReports().addSnapshotListener { snapshot, error ->
//            if (error != null) {
//                // TODO: Handle error
//                return@addSnapshotListener
//            }
//            if (snapshot != null) {
//                // NOVI, ISPRAVAN NAČIN
//                val reportsWithIds = snapshot.documents.mapNotNull { doc ->
//                    // Pretvori dokument u NoiseReport objekat
//                    val report = doc.toObject(NoiseReport::class.java)
//                    // Napravi kopiju objekta, ali sada mu postavi i ispravan ID
//                    report?.copy(id = doc.id)
//                }
//                noiseReports = reportsWithIds
//            }
//        }
//    }

    // Slušamo promene u 'users' kolekciji za aktivne korisnike
    LaunchedEffect(Unit) {
        AuthRepository.getActiveUsers().addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val usersWithIds = snapshot.documents.mapNotNull { doc ->
                    val user = doc.toObject(UserProfile::class.java)
                    user?.copy(uid = doc.id)
                }
                otherUsers = usersWithIds
            }
        }
    }

    // Filtriranje korisnika koji su u blizini
    val nearbyUsers = remember(location, otherUsers) {
        val myLocation = location ?: return@remember emptyList()
        val myUid = AuthRepository.currentUid()
        val NEARBY_RADIUS_METERS = 500f

        otherUsers.filter { user ->
            if (user.uid == myUid || user.lastKnownLocation == null) return@filter false
            val userLocation = android.location.Location("").apply {
                latitude = user.lastKnownLocation.latitude
                longitude = user.lastKnownLocation.longitude
            }
            myLocation.distanceTo(userLocation) < NEARBY_RADIUS_METERS
        }
    }

    // === UI (Korisnički interfejs) ===

    // Prikazujemo dijalog za dodavanje nove prijave ako je 'showAddNoiseDialog' true
    if (showAddNoiseDialog) {
        AddNoiseReportDialog(
            onDismiss = { showAddNoiseDialog = false },
            onConfirm = { noiseLevel, noiseType ->
                showAddNoiseDialog = false
                coroutineScope.launch {
                    val currentLoc = location
                    val currentUserId = AuthRepository.currentUid()
                    if (currentLoc != null && currentUserId != null) {
                        val userProfile = AuthRepository.getUserProfile(currentUserId)
                        val newReport = NoiseReport(
                            userId = currentUserId,
                            username = userProfile?.username ?: "Nepoznat",
                            location = com.google.firebase.firestore.GeoPoint(currentLoc.latitude, currentLoc.longitude),
                            noiseLevel = noiseLevel,
                            noiseType = noiseType
                        )
                        NoiseRepository.addNoiseReport(newReport)
                        AuthRepository.incrementUserPoints(currentUserId, 5) // +5 poena za dodavanje
                    }
                }
            }
        )
    }

    // Glavni Box layout koji drži mapu, dugme i Bottom Sheet
    Box(modifier = Modifier.fillMaxSize()) {
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(LatLng(43.32, 21.89), 12f) // Niš
        }

        LaunchedEffect(location) {
            location?.let {
                val userLocation = LatLng(it.latitude, it.longitude)
                // Animiraj kameru samo ako je daleko od trenutnog centra
                if (cameraPositionState.position.target.latitude != userLocation.latitude ||
                    cameraPositionState.position.target.longitude != userLocation.longitude) {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLng(userLocation), 1000)
                }
            }
        }

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Marker za trenutnu lokaciju korisnika
            location?.let {
                Marker(state = MarkerState(position = LatLng(it.latitude, it.longitude)), title = "Moja lokacija")
            }

            // Markeri za prijave buke
            noiseReports.forEach { report ->
                val iconResId = when (report.noiseType) {
                    NoiseType.SAOBRACAJ -> R.drawable.ic_traffic
                    NoiseType.GRADNJA -> R.drawable.ic_construction
                    NoiseType.MUZIKA -> R.drawable.ic_music
                    NoiseType.KOMSIJE -> R.drawable.ic_neighbors
                    NoiseType.INDUSTRIJA -> R.drawable.ic_industry
                    NoiseType.PRIRODA -> R.drawable.ic_nature
                }
                val iconColor = when (report.noiseLevel) {
                    NoiseLevel.TIHO -> Color.Green
                    NoiseLevel.UMERENO -> Color(0xFFADD8E6) // Svetlo plava
                    NoiseLevel.BUCNO -> Color.Yellow
                    NoiseLevel.VEOMA_BUCNO -> Color(0xFFFFA500) // Narandžasta
                    NoiseLevel.VRLO_VISOKA_BUKA -> Color.Red
                    NoiseLevel.EKSTRENMA_BUKA -> Color(0xFF800080) // Ljubičasta
                }
                val icon = bitmapDescriptorFromVector(vectorResId = iconResId, tintColor = iconColor)

                Marker(
                    state = MarkerState(position = LatLng(report.location.latitude, report.location.longitude)),
                    title = "Nivo buke: ${report.noiseLevel}",
                    snippet = "Tip: ${report.noiseType} | Prijavio: ${report.username}",
                    icon = icon,
                    onClick = {
                        selectedReport = report
                        showBottomSheet = true
                        true // Vraćamo true da sprečimo podrazumevani info prozor
                    }
                )
            }

            // Markeri za druge korisnike u blizini
            nearbyUsers.forEach { user ->
                user.lastKnownLocation?.let { geoPoint ->
                    Marker(
                        state = MarkerState(position = LatLng(geoPoint.latitude, geoPoint.longitude)),
                        title = user.username,
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                    )
                }
            }
        }

        // Dugme za dodavanje prijave buke
        Button(
            onClick = { showAddNoiseDialog = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 40.dp, vertical = 16.dp)
        ) {
            Text("DODAJ PRIJAVU BUKE")
        }

        // Prikaz Bottom Sheet-a sa detaljima ako je 'showBottomSheet' true
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                selectedReport?.let { report ->
                    NoiseReportDetailsSheet(
                        report = report,
                        onVote = { isLike ->
                            // Pokrećemo korutinu za sve operacije
                            coroutineScope.launch {

                                // 1. Prvo pošaljemo glas u bazu i sačekamo da se završi
                                AuthRepository.currentUid()?.let { voterId ->
                                    NoiseRepository.voteOnReport(report.id, report.userId, voterId, isLike)
                                }

                                // 2. Nakon toga, kažemo sheet-u da se sakrije i sačekamo da se animacija završi
                                sheetState.hide()

                                // 3. Kada se animacija završi, ažuriramo i naše 'showBottomSheet' stanje na 'false'
                                //    Ovaj 'if' je dodatna provera da budemo sigurni
                                if (!sheetState.isVisible) {
                                    showBottomSheet = false
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

// =============================================================================================
// POMOĆNE FUNKCIJE - One takođe moraju biti u istom fajlu, ispod NoiseMap
// =============================================================================================

@Composable
fun AddNoiseReportDialog(
    onDismiss: () -> Unit,
    onConfirm: (noiseLevel: NoiseLevel, noiseType: NoiseType) -> Unit
) {
    var selectedLevel by remember { mutableStateOf(NoiseLevel.TIHO) }
    var selectedType by remember { mutableStateOf(NoiseType.SAOBRACAJ) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Prijava Nivoa Buke") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) { // Dodato skrolovanje za manje ekrane
                Text("Izaberi subjektivni nivo buke:")
                NoiseLevel.values().forEach { level ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedLevel = level }
                    ) {
                        RadioButton(selected = (level == selectedLevel), onClick = { selectedLevel = level })
                        Text(text = level.name)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Izaberi tip buke:")
                NoiseType.values().forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedType = type }
                    ) {
                        RadioButton(selected = (type == selectedType), onClick = { selectedType = type })
                        Text(text = type.name)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(selectedLevel, selectedType) }) { Text("Potvrdi") } },
        dismissButton = { Button(onClick = onDismiss) { Text("Odustani") } }
    )
}


@Composable
fun NoiseReportDetailsSheet(
    report: NoiseReport,
    onVote: (isLike: Boolean) -> Unit
) {
    val currentUserId = AuthRepository.currentUid()
    val alreadyVoted = report.likedBy.contains(currentUserId) || report.dislikedBy.contains(currentUserId)
    val userLikedThis = report.likedBy.contains(currentUserId)
    val userDislikedThis = report.dislikedBy.contains(currentUserId)

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Detalji Prijave", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Prijavio: ${report.username}")
        Text("Nivo buke: ${report.noiseLevel}")
        Text("Tip buke: ${report.noiseType}")

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // DUGME ZA LIKE
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { onVote(true) }, enabled = !alreadyVoted) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Like",
                        // ===== KLJUČNA IZMENA: Dinamičko bojenje =====
                        tint = if (userLikedThis)
                            Color.Blue
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant



                    )
                }
                Text("${report.likes}")
            }

            // DUGME ZA DISLIKE
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { onVote(false) }, enabled = !alreadyVoted) {
                    Icon(
                        imageVector = Icons.Default.ThumbDown,
                        contentDescription = "Dislike",
                        // ===== KLJUČNA IZMENA: Dinamičko bojenje =====
                        tint = if (userDislikedThis)
                            Color.Red
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("${report.dislikes}")
            }
        }
        Spacer(modifier = Modifier.height(16.dp)) // Dodat razmak na dnu
    }
}