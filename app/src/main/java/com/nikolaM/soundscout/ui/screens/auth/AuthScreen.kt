package com.nikolaM.soundscout.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.nikolaM.soundscout.R
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color

@Composable
fun AuthScreen(
    vm: AuthViewModel,
    onNavigateToRegistration: () -> Unit
) {

    val ui by vm.ui.collectAsState()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.soundscout),
            contentDescription = "SoundScout Logo"
        )
        Spacer(Modifier.height(24.dp))
        Text("Welcome to SoundScout", style = MaterialTheme.typography.headlineLarge, color=Color.Blue);

        Spacer(Modifier.height(24.dp))


        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Korisničko ime") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Šifra") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))


        Button(
            onClick = { vm.signIn(username, password) },
            enabled = !ui.isLoading && username.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Prijavi se")
        }


        TextButton(
            onClick = onNavigateToRegistration,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Nemaš nalog? Registruj se")
        }


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
    }
}