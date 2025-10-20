package com.nikolaM.soundscout.ui.screens.home


import com.nikolaM.soundscout.R
import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.res.painterResource
import com.nikolaM.soundscout.ui.filter.FilterSheet
import com.nikolaM.soundscout.ui.filter.MapViewModel

enum class LegendState {
    BOJE, OZNAKE
}


@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LoggedInHome(navController: NavController, onLogout: () -> Unit, vm: MapViewModel) { // <-- 1. DODAJEMO onLogout KAO PARAMETAR


    val currentFilters by vm.filters.collectAsState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showFilterSheet by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val permissionsList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
    val permissionState = rememberMultiplePermissionsState(permissions = permissionsList)

    val locationPermissionsGranted = remember(permissionState.permissions.map { it.status }) {
        permissionState.permissions.filter {
            it.permission == Manifest.permission.ACCESS_FINE_LOCATION ||
                    it.permission == Manifest.permission.ACCESS_COARSE_LOCATION
        }.all { it.status.isGranted}
    }


    val snackbarHostState = remember { SnackbarHostState() }
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

                // Uvek prikazujemo mapu...

                Text(
                    text = "DEBUG: locationPermissionsGranted = $locationPermissionsGranted",
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                NoiseMap(
                    vm = vm,
                    // ...ali joj prosleđujemo informaciju da li imamo dozvolu
                    locationPermissionGranted = locationPermissionsGranted
                )


                // Ako dozvole NISU date, prikaži dugme za ponovno traženje PREKO mape
                if (!locationPermissionsGranted ) {
                    Button(
                        onClick = {permissionState.launchMultiplePermissionRequest()},
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp, vertical = 16.dp)
                    ) {
                        Text("DOZVOLI PRISTUP LOKACIJI")
                    }
                }
            }

    }
//    if (showFilterSheet) {
//        ModalBottomSheet(
//            onDismissRequest = { showFilterSheet = false },
//            sheetState = sheetState
//        ) {
//            FilterSheet(
//                vm = vm,
//                onApplyFilters = {
//                    // <<-- ISPRAVLJENA LINIJA -->>
//                    coroutineScope.launch {
//                        sheetState.hide()
//                        if (!sheetState.isVisible) {
//                            showFilterSheet = false
//                        }
//                    }
//                }
//            )
//        }
//    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState
        ) {
            // Prosleđujemo početno stanje i definišemo šta se dešava na 'Apply'
            FilterSheet(
                initialFilters = currentFilters,
                onDismiss = { showFilterSheet = false },
                onApply = { newFilters ->
                    // Tek sada zovemo ViewModel da primeni filtere
                    vm.applyFilters(newFilters)

                    // I zatvaramo prozor
                    coroutineScope.launch {
                        sheetState.hide()
                        if (!sheetState.isVisible) {
                            showFilterSheet = false
                        }
                    }
                },
                locationGranted = locationPermissionsGranted
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoiseMap(vm: MapViewModel, locationPermissionGranted: Boolean) {

    val noiseReports by vm.noiseReports.collectAsState()
    val filters by vm.filters.collectAsState()

    val location by if (locationPermissionGranted) {
        LocationService.locationFlow.collectAsState()
    } else {
        remember { mutableStateOf(null) }
    }

    //val location by LocationService.locationFlow.collectAsState()
    //var noiseReports by remember { mutableStateOf<List<NoiseReport>>(emptyList()) }
    var otherUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    var showAddNoiseDialog by remember { mutableStateOf(false) }

    val finalReportsToShow by vm.finalVisibleReports.collectAsState()
    var selectedReport by remember { mutableStateOf<NoiseReport?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }


    if (locationPermissionGranted) {
        val context = LocalContext.current // Potreban nam je context
        // Pokreni servis samo ako imamo dozvolu
        LaunchedEffect(Unit) {
            val intent = Intent(context, LocationService::class.java).apply {
                action = LocationService.ACTION_START
            }
            context.startService(intent)
        }

        LaunchedEffect(location) {
            location?.let { vm.updateUserLocation(it) }
        }


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
    }


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

//    val finalReportsToShow = remember(noiseReports, filters.radiusMeters, location) {
//        val reports = noiseReports // Uzimamo listu iz ViewModela
//        val radius = filters.radiusMeters
//
//        // Ako je filter za radijus uključen (nije null)...
//        if (radius != null) {
//            val myLocation = location ?: return@remember emptyList()
//
//            // ...filtriraj listu po radijusu
//            reports.filter { report ->
//                val reportLocation = android.location.Location("").apply {
//                    latitude = report.location.latitude
//                    longitude = report.location.longitude
//                }
//                myLocation.distanceTo(reportLocation) < radius
//            }
//        } else {
//            // Ako filter NIJE uključen, prikaži sve
//            reports
//        }
//    }


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


    Box(modifier = Modifier.fillMaxSize()) {
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(LatLng(43.32, 21.89), 12f) // Niš
        }

        if (locationPermissionGranted) {
            LaunchedEffect(location) {
                location?.let {
                    val userLocation = LatLng(it.latitude, it.longitude)

                    if (cameraPositionState.position.target.latitude != userLocation.latitude ||
                        cameraPositionState.position.target.longitude != userLocation.longitude
                    ) {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLng(userLocation),
                            1000
                        )
                    }
                }
            }
        }
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            if (locationPermissionGranted) {
                location?.let {
                    Marker(
                        state = MarkerState(position = LatLng(it.latitude, it.longitude)),
                        title = "Moja lokacija"
                    )
                }
            }


            finalReportsToShow.forEach { report ->
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
                    NoiseLevel.UMERENO -> Color(0xFFADD8E6)
                    NoiseLevel.BUCNO -> Color.Yellow
                    NoiseLevel.VEOMA_BUCNO -> Color(0xFFFFA500)
                    NoiseLevel.VRLO_VISOKA_BUKA -> Color.Red
                    NoiseLevel.EKSTRENMA_BUKA -> Color(0xFF800080)
                }
                val icon = bitmapDescriptorFromVector(vectorResId = iconResId, tintColor = iconColor);

                Marker(
                    state = MarkerState(position = LatLng(report.location.latitude, report.location.longitude)),
                    title = "Nivo buke: ${report.noiseLevel}",
                    snippet = "Tip: ${report.noiseType} | Prijavio: ${report.username}",
                    icon = icon,
                    onClick = {
                        selectedReport = report
                        showBottomSheet = true
                        true
                    }
                )
            }

            if (locationPermissionGranted) {
                nearbyUsers.forEach { user ->
                    user.lastKnownLocation?.let { geoPoint ->
                        Marker(
                            state = MarkerState(
                                position = LatLng(
                                    geoPoint.latitude,
                                    geoPoint.longitude
                                )
                            ),
                            title = user.username,
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                        )
                    }
                }
            }
        }

        if (locationPermissionGranted) {
            Button(
                onClick = { showAddNoiseDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp, vertical = 16.dp)
            ) {
                Text("DODAJ PRIJAVU BUKE")
            }
        }

        MapLegend(
            modifier = Modifier
                .align(Alignment.TopStart) // Postavlja je u gornji levi ugao
                .padding(16.dp)

        )

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapLegend(
    modifier: Modifier = Modifier
) {
    // Stanje koje prati da li je legenda proširena (expanded) ili ne
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.animateContentSize(), // Dodaje animaciju promene veličine
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Deo koji je uvek vidljiv - samo ikonica za otvaranje/zatvaranje
            IconButton(
                onClick = { isExpanded = !isExpanded }, // Klikom se menja stanje
                modifier = Modifier
                    .padding(4.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.Info, contentDescription = "Prikaži/Sakrij legendu")
            }

            // Deo koji se prikazuje samo ako je legenda proširena
            AnimatedVisibility(visible = isExpanded) {
                var tabState by remember { mutableStateOf(LegendState.BOJE) }
                //Column(modifier = Modifier.width(220.dp).padding(horizontal = 8.dp, vertical = 4.dp))
                Column(modifier = Modifier.fillMaxWidth(fraction = 0.38f).padding(horizontal = 8.dp, vertical = 4.dp)){
                    TabRow(selectedTabIndex = tabState.ordinal) {
                        Tab(
                            selected = tabState == LegendState.BOJE,
                            onClick = { tabState = LegendState.BOJE },
                            text = { Text("Boje") }
                        )
                        Tab(
                            selected = tabState == LegendState.OZNAKE,
                            onClick = { tabState = LegendState.OZNAKE },
                            text = { Text("Oznake") }
                        )
                    }
                    // Prikazujemo sadržaj u zavisnosti od izabranog taba
                    when (tabState) {
                        LegendState.BOJE -> ColorLegendContent()
                        LegendState.OZNAKE -> IconLegendContent()
                    }
                }
            }
        }
    }
}


//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun MapLegend(
//    modifier: Modifier = Modifier,
//    currentState: LegendState,
//    onStateChange: (LegendState) -> Unit
//) {
//    Card(
//        modifier = modifier,
//        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
//    ) {
//        Column {
//            // Tabovi za biranje prikaza
//            TabRow(selectedTabIndex = currentState.ordinal) {
//                Tab(
//                    selected = currentState == LegendState.BOJE,
//                    onClick = { onStateChange(LegendState.BOJE) },
//                    text = { Text("Boje") }
//                )
//                Tab(
//                    selected = currentState == LegendState.OZNAKE,
//                    onClick = { onStateChange(LegendState.OZNAKE) },
//                    text = { Text("Oznake") }
//                )
//            }
//
//            // Prikazujemo sadržaj u zavisnosti od izabranog taba
//            when (currentState) {
//                LegendState.BOJE -> ColorLegendContent()
//                LegendState.OZNAKE -> IconLegendContent()
//            }
//        }
//    }
//}

// Sadržaj za prikaz boja
@Composable
fun ColorLegendContent() {
    Column(modifier = Modifier.padding(8.dp)) {
        Text("Jačina buke:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 4.dp))
        // Prolazimo kroz sve vrednosti NoiseLevel enuma i prikazujemo ih
        NoiseLevel.values().forEach { level ->
            val color = when (level) {
                NoiseLevel.TIHO -> Color.Green
                NoiseLevel.UMERENO -> Color(0xFFADD8E6)
                NoiseLevel.BUCNO -> Color.Yellow
                NoiseLevel.VEOMA_BUCNO -> Color(0xFFFFA500)
                NoiseLevel.VRLO_VISOKA_BUKA -> Color.Red
                NoiseLevel.EKSTRENMA_BUKA ->Color(0xFF800080)
            }
            LegendItem(text = level.name) {
                Box(modifier = Modifier.size(16.dp).background(color, CircleShape))
            }
        }
    }
}

// Sadržaj za prikaz oznaka (ikonica)
@Composable
fun IconLegendContent() {
    Column(modifier = Modifier.padding(8.dp)) {
        Text("Tipovi oznaka:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 4.dp))

        // Fiksne oznake
        LegendItem(text = "Moja lokacija") {
            // Placeholder za standardni crveni pin
            Icon(Icons.Default.LocationOn, contentDescription = "Moja lokacija", tint = Color.Red)
        }
        LegendItem(text = "Drugi korisnik") {
            // Placeholder za standardni plavi pin
            Icon(Icons.Default.LocationOn, contentDescription = "Drugi korisnik", tint = Color.Blue)
        }

        Divider(modifier = Modifier.padding(vertical = 4.dp))

        // Prolazimo kroz sve vrednosti NoiseType enuma i prikazujemo ih
        NoiseType.values().forEach { type ->
            val iconRes = when (type) {
                NoiseType.SAOBRACAJ -> R.drawable.ic_traffic
                NoiseType.GRADNJA -> R.drawable.ic_construction
                NoiseType.MUZIKA -> R.drawable.ic_music
                NoiseType.KOMSIJE -> R.drawable.ic_neighbors
                NoiseType.INDUSTRIJA -> R.drawable.ic_industry
                NoiseType.PRIRODA -> R.drawable.ic_nature
            }
            LegendItem(text = type.name) {
                Icon(painter = painterResource(id = iconRes), contentDescription = type.name, modifier = Modifier.size(24.dp))
            }
        }
    }
}

// Pomoćna komponenta za jedan red u legendi
@Composable
private fun LegendItem(text: String, icon: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        icon()
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}