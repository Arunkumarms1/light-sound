package com.light.sound

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LightSoundUI(activity: MainActivity) {
    var text by remember { mutableStateOf("HELLO") }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text("LightSound", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Message to transmit") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.FLASHLIGHT))
                activity.startFlashPattern(text)
            }) {
                Text("Flashlight TX")
            }
            Button(onClick = {
                launcher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                activity.playSoundPattern(text)
            }) {
                Text("Sound TX")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = {
            launcher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
            activity.startListening()
        }) {
            Text("Listen (15s)")
        }
        Spacer(modifier = Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Encoding: Binary / Morse-style", style = MaterialTheme.typography.bodyMedium)
                Text("One device blinks flashlight; other listens via mic or detects light.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
