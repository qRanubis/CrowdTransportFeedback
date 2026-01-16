package com.example.crowdtransportfeedback.ui.screens


import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

@Composable
fun AddFeedbackScreen(
    onSave: (Int, String, String, Double, Double) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }

    var scoreText by remember { mutableStateOf("4") }
    var line by remember { mutableStateOf("41") }
    var comment by remember { mutableStateOf("") }

    var lat by remember { mutableStateOf<Double?>(null) }
    var lon by remember { mutableStateOf<Double?>(null) }
    var locStatus by remember { mutableStateOf("Getting location...") }

    fun fetchLocation() {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!(fineGranted || coarseGranted)) {
            locStatus = "Permission required"
            return
        }

        locStatus = "Getting location..."

        fused.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    lat = location.latitude
                    lon = location.longitude
                    locStatus = "Location OK (last)"
                } else {
                    // fallback: ia locatie curenta
                    locStatus = "Getting current location..."
                    fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener { loc2 ->
                            if (loc2 != null) {
                                lat = loc2.latitude
                                lon = loc2.longitude
                                locStatus = "Location OK (current)"
                            } else {
                                lat = null
                                lon = null
                                locStatus = "Location still null (check emulator settings)"
                            }
                        }
                        .addOnFailureListener {
                            lat = null
                            lon = null
                            locStatus = "Current location error"
                        }
                }
            }
            .addOnFailureListener {
                lat = null
                lon = null
                locStatus = "Last location error"
            }
    }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val fine = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fine || coarse) {
            fetchLocation()
        } else {
            locStatus = "Permission denied (cannot save)"
            lat = null
            lon = null
        }
    }

    // permisune locatie
    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            fetchLocation()
        } else {
            locStatus = "Requesting permission..."
            permLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val canSave = (lat != null && lon != null)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Add feedback", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "$locStatus  lat=${lat ?: "-"}  lon=${lon ?: "-"}",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(onClick = { fetchLocation() }) {
            Text("Retry location")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = scoreText,
            onValueChange = { scoreText = it },
            label = { Text("Score (1-5)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = line,
            onValueChange = { line = it },
            label = { Text("Line (ex: 41, M2)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            label = { Text("Comment") },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Button(
                onClick = {
                    val score = scoreText.toIntOrNull() ?: 0
                    onSave(score, comment, line, lat!!, lon!!)
                },
                enabled = canSave
            ) {
                Text("Save")
            }

            Spacer(modifier = Modifier.width(12.dp))

            OutlinedButton(onClick = { onCancel() }) {
                Text("Cancel")
            }
        }

        if (!canSave) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Location is required to save.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
