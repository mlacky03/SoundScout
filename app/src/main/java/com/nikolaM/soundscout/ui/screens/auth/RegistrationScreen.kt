package com.nikolaM.soundscout.ui.screens.auth

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    vm: AuthViewModel,
    onNavigateBack: () -> Unit // Funkcija za povratak nazad
) {
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current

    // Polja
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    // Foto
    var selectedGallery by remember { mutableStateOf<Uri?>(null) }
    var cameraBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val galleryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        cameraBitmap = null // Resetuj drugu opciju
        selectedGallery = uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        selectedGallery = null // Resetuj drugu opciju
        cameraBitmap = bmp
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Ako je korisnik DAO dozvolu, SADA pokrećemo kameru
            cameraLauncher.launch(null)
        } else {
            // Opciono: Ovde možeš prikazati poruku da je dozvola odbijena
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kreiraj nalog") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()) // Omogućava skrolovanje
        ) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(username, { username = it }, label = { Text("Korisničko ime") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(password, { password = it }, label = { Text("Šifra (min 6)") },
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            OutlinedTextField(phone, { phone = it }, label = { Text("Broj telefona") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            OutlinedTextField(name, { name = it }, label = { Text("Ime") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            OutlinedTextField(surname, { surname = it }, label = { Text("Prezime") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp))


            val imageSelected = selectedGallery != null || cameraBitmap != null

            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    // <<-- Onemogući dugme ako je slika već izabrana -->>
                    enabled = !imageSelected
                ) { Text("Izaberi foto") }

                OutlinedButton(
                    onClick = {
                        val permission = Manifest.permission.CAMERA
                        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                            cameraLauncher.launch(null)
                        } else {
                            cameraPermissionLauncher.launch(permission)
                        }
                    },
                    // <<-- Onemogući dugme ako je slika već izabrana -->>
                    enabled = !imageSelected
                ) {
                    Text("Snimi kamerom")
                }
            }

            if (imageSelected) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Preview slike
                    AsyncImage(
                        model = selectedGallery ?: cameraBitmap,
                        contentDescription = "Izabrana slika za profil",
                        modifier = Modifier.size(64.dp)
                    )

                    // Poruka o uspehu
                    Text(
                        text = if (selectedGallery != null) "Slika iz galerije izabrana!" else "Fotografija snimljena!",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Dugme za uklanjanje slike i ponovni izbor
                    TextButton(onClick = {
                        selectedGallery = null
                        cameraBitmap = null
                    }) {
                        Text("Ukloni")
                    }
                }
            }

            Button(
                onClick = {
                    vm.signUp(context, username, password, name, surname, phone, selectedGallery, cameraBitmap)
                },
                enabled = !ui.isLoading && username.isNotBlank() && password.length >= 6 && name.isNotBlank() && surname.isNotBlank() && phone.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
            ) { Text("Registruj se") }

            // Prikaz greške ili učitavanja
            if (ui.isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 16.dp))
            }
            ui.error?.let {
                Text(
                    text = "Greška: $it",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}