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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import com.nikolaM.soundscout.data.model.NoiseReport
import com.nikolaM.soundscout.data.repository.NoiseRepository
import com.nikolaM.soundscout.data.model.NoiseLevel
import com.nikolaM.soundscout.data.model.NoiseType
import com.nikolaM.soundscout.data.model.UserProfile
import kotlinx.coroutines.launch
import com.nikolaM.soundscout.data.repository.AuthRepository
import com.nikolaM.soundscout.ui.navigation.Routes
import com.nikolaM.soundscout.util.bitmapDescriptorFromVector
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.res.painterResource
import com.nikolaM.soundscout.data.services.LocationService
import com.nikolaM.soundscout.ui.filter.FilterSheet
import com.nikolaM.soundscout.ui.filter.MapViewModel

enum class LegendState {
    BOJE, OZNAKE
}


@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LoggedInHome(navController: NavController, onLogout: () -> Unit, vm: MapViewModel) {
    val currentFilters by vm.filters.collectAsState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showFilterSheet by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var isSearchActive by remember { mutableStateOf(false) }
    val searchQuery by vm.searchQuery.collectAsState()
    val searchSuggestions by vm.searchSuggestions.collectAsState()


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



    LaunchedEffect(Unit) {
        permissionState.launchMultiplePermissionRequest()
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        NoiseTypeSearchBar(
                            query = searchQuery,
                            suggestions = searchSuggestions,
                            onQueryChanged = { vm.onSearchQueryChanged(it) },
                            onSuggestionClicked = { noiseType ->
                                vm.toggleNoiseTypeFilter(noiseType)
                                isSearchActive = false
                                vm.onSearchQueryChanged("")
                            },
                            onClose = { isSearchActive = false }
                        )
                    } else
                    {
                    Text("SS Mapa")} },
                actions = {
                    if (!isSearchActive)
                    {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Pretraga")
                        }

                        IconButton(onClick = { vm.clearFilters() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Resetuj filtere")
                        }
                        IconButton(onClick = { navController.navigate(Routes.REPORT_LIST) }) {
                                Icon(Icons.Default.List, contentDescription = "Prikaži listu")
                        }

                        IconButton(onClick = { navController.navigate(Routes.RANKING) }) {
                                Icon(Icons.Default.Leaderboard, contentDescription = "Rang lista")
                        }

                        IconButton(onClick = { showFilterSheet = true }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filteri")
                        }

                        TextButton(onClick = onLogout) {
                                Text("Odjava")

                        }
                    }
                }

            )
        }
    ) { paddingValues ->

            Box(modifier = Modifier.padding(paddingValues)) {

                NoiseMap(
                    vm = vm,
                    locationPermissionGranted = locationPermissionsGranted
                )


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

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState
        ) {
            FilterSheet(
                initialFilters = currentFilters,
                onDismiss = { showFilterSheet = false },
                onApply = { newFilters ->
                    vm.applyFilters(newFilters)

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


    val location by if (locationPermissionGranted) {
        LocationService.locationFlow.collectAsState()
    } else {
        remember { mutableStateOf(null) }
    }

    var otherUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    var showAddNoiseDialog by remember { mutableStateOf(false) }

    val finalReportsToShow by vm.finalVisibleReports.collectAsState()

    var selectedReport by remember { mutableStateOf<NoiseReport?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }


    if (locationPermissionGranted) {
        val context = LocalContext.current
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
                        AuthRepository.incrementUserPoints(currentUserId, 5)
                    }
                }
            }
        )
    }


    Box(modifier = Modifier.fillMaxSize()) {
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(LatLng(43.32, 21.89), 12f) // lokacija za nis kad nema dozvola za lokaciju
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
                            1500
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
                val icon = bitmapDescriptorFromVector(vectorResId = iconResId, tintColor = iconColor)

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
                .align(Alignment.TopStart)
                .padding(16.dp)

        )

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                selectedReport?.let { report ->
                    NoiseReportDetailsSheet(
                        report = report,
                        onVote = { isLike ->
                            coroutineScope.launch {

                                AuthRepository.currentUid()?.let { voterId ->
                                    NoiseRepository.voteOnReport(report.id, report.userId, voterId, isLike)
                                }

                                sheetState.hide()

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
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Izaberi subjektivni nivo buke:")
                NoiseLevel.entries.forEach { level ->
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
                NoiseType.entries.forEach { type ->
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

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { onVote(true) }, enabled = !alreadyVoted) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Like",
                        tint = if (userLikedThis)
                            Color.Blue
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant



                    )
                }
                Text("${report.likes}")
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { onVote(false) }, enabled = !alreadyVoted) {
                    Icon(
                        imageVector = Icons.Default.ThumbDown,
                        contentDescription = "Dislike",
                        tint = if (userDislikedThis)
                            Color.Red
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("${report.dislikes}")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapLegend(
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            IconButton(
                onClick = { isExpanded = !isExpanded }, // Klikom se menja stanje
                modifier = Modifier
                    .padding(4.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.Info, contentDescription = "Prikaži/Sakrij legendu")
            }

            AnimatedVisibility(visible = isExpanded) {
                var tabState by remember { mutableStateOf(LegendState.BOJE) }
                //Column(modifier = Modifier.width(220.dp).padding(horizontal = 8.dp, vertical = 4.dp))
                Column(modifier = Modifier.fillMaxWidth(fraction = 0.38f).padding(horizontal = 8.dp, vertical = 4.dp)){
                    TabRow(selectedTabIndex = tabState.ordinal) {
                        Tab(
                            selected = tabState == LegendState.BOJE,
                            onClick = { tabState = LegendState.BOJE },
                            text = { Text("B") }
                        )
                        Tab(
                            selected = tabState == LegendState.OZNAKE,
                            onClick = { tabState = LegendState.OZNAKE },
                            text = { Text("O") }
                        )
                    }
                    when (tabState) {
                        LegendState.BOJE -> ColorLegendContent()
                        LegendState.OZNAKE -> IconLegendContent()
                    }
                }
            }
        }
    }
}

@Composable
fun ColorLegendContent() {
    Column(modifier = Modifier.padding(8.dp)) {
        Text("Jačina buke:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 4.dp))
        NoiseLevel.entries.forEach { level ->
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

@Composable
fun IconLegendContent() {
    Column(modifier = Modifier.padding(8.dp)) {
        Text("Tipovi oznaka:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 4.dp))

        LegendItem(text = "Moja lokacija") {
            Icon(Icons.Default.LocationOn, contentDescription = "Moja lokacija", tint = Color.Red)
        }
        LegendItem(text = "Drugi korisnik") {
            Icon(Icons.Default.LocationOn, contentDescription = "Drugi korisnik", tint = Color.Blue)
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            thickness = DividerDefaults.Thickness,
            color = DividerDefaults.color
        )

        NoiseType.entries.forEach { type ->
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

@Composable
fun NoiseTypeSearchBar(
    query: String,
    suggestions: List<NoiseType>,
    onQueryChanged: (String) -> Unit,
    onSuggestionClicked: (NoiseType) -> Unit,
    onClose: () -> Unit // Funkcija za zatvaranje search bara
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(suggestions) {
        // Otvori dropdown ako ima predloga i ako nije prazan query
        isDropdownExpanded = suggestions.isNotEmpty() && query.isNotEmpty()
    }

    Box {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Pretraži tip buke...") },
            leadingIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zatvori pretragu")
                }
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChanged("") }) { // Dugme za brisanje teksta
                        Icon(Icons.Default.Close, contentDescription = "Obriši tekst")
                    }
                }
            },
            singleLine = true
        )

        DropdownMenu(
            expanded = isDropdownExpanded,
            onDismissRequest = { isDropdownExpanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            suggestions.forEach { noiseType ->
                DropdownMenuItem(
                    text = { Text(noiseType.name) },
                    onClick = {
                        onSuggestionClicked(noiseType)
                        isDropdownExpanded = false
                    }
                )
            }
        }
    }
}