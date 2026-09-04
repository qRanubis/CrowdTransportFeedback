package com.example.crowdtransportfeedback.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.crowdtransportfeedback.domain.BucharestTransitCatalog
import com.example.crowdtransportfeedback.domain.TransportType
import com.example.crowdtransportfeedback.location.AndroidLocationProvider
import com.example.crowdtransportfeedback.ui.form.FeedbackFormState
import com.example.crowdtransportfeedback.ui.form.LocationState
import com.example.crowdtransportfeedback.ui.viewmodel.FeedbackViewModel

@Composable
fun AddFeedbackScreen(vm: FeedbackViewModel, onSaved: () -> Unit, onCancel: () -> Unit) {
    val state by vm.formState.collectAsState()
    val context = LocalContext.current
    val provider = remember { AndroidLocationProvider(context) }
    var navigationHandled by rememberSaveable { mutableStateOf(false) }

    fun hasPermission() = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    fun fetch() {
        if (!hasPermission()) {
            vm.setLocationState(LocationState.PermissionRequired)
            return
        }
        vm.setLocationState(LocationState.Loading)
        provider.getLocation { result ->
            vm.setLocationState(
                result.fold(
                    { LocationState.Available(it.latitude, it.longitude) },
                    { LocationState.Error }
                )
            )
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) fetch() else vm.setLocationState(LocationState.PermissionDenied)
    }

    fun requestPermission() {
        vm.setLocationState(LocationState.RequestingPermission)
        launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    fun cancelAndNavigateBack() {
        if (!state.isSubmitting && !navigationHandled) {
            navigationHandled = true
            vm.resetFeedbackForm()
            onCancel()
        }
    }

    BackHandler(enabled = true) {
        if (!state.isSubmitting) {
            cancelAndNavigateBack()
        }
    }

    LaunchedEffect(Unit) {
        if (hasPermission()) {
            fetch()
        } else if (state.locationState !is LocationState.PermissionDenied) {
            requestPermission()
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Add feedback", style = MaterialTheme.typography.titleLarge)
        RatingSelector("Overall trust", "1 = Very low", "5 = Very high", state.overallTrust, vm::setOverall)
        RatingSelector("Crowding comfort", "1 = Extremely crowded", "5 = Plenty of space", state.crowdingScore, vm::setCrowding)
        RatingSelector("Cleanliness", "1 = Very dirty", "5 = Very clean", state.cleanlinessScore, vm::setCleanliness)
        RatingSelector("Punctuality", "1 = Very poor", "5 = Very good", state.punctualityScore, vm::setPunctuality)
        Selector("Transport type", state.transportType?.displayName ?: "Select transport type", TransportType.entries.map { it.displayName }) { label ->
            vm.setTransportType(TransportType.entries.first { it.displayName == label })
        }
        val type = state.transportType
        SearchableLineSelector(state, type?.let(BucharestTransitCatalog::linesFor).orEmpty(), vm::setLine)
        OutlinedTextField(state.comment, vm::setComment, label = { Text("Comment (optional)") }, modifier = Modifier.fillMaxWidth().height(120.dp))
        Spacer(Modifier.height(12.dp))
        Text("Location", style = MaterialTheme.typography.labelLarge)
        Text(locationMessage(state.locationState))
        OutlinedButton(
            onClick = { if (hasPermission()) fetch() else requestPermission() },
            enabled = !state.isSubmitting && state.locationState !is LocationState.Loading
        ) {
            Text(if (hasPermission()) "Retry location" else "Allow location")
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row {
            Button(
                onClick = {
                    vm.submit {
                        if (!navigationHandled) {
                            navigationHandled = true
                            onSaved()
                        }
                    }
                },
                enabled = state.isValid && !state.isSubmitting
            ) {
                Text(if (state.isSubmitting) "Saving..." else "Save")
            }
            Spacer(Modifier.width(12.dp))
            OutlinedButton(onClick = ::cancelAndNavigateBack, enabled = !state.isSubmitting) {
                Text("Cancel")
            }
        }
        if (!state.isValid) {
            Text("All ratings, transport type, line, and location are required.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun RatingSelector(label: String, low: String, high: String, selected: Int?, onSelect: (Int) -> Unit) {
    Spacer(Modifier.height(12.dp))
    Text(label, style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..5).forEach { value ->
            FilterChip(selected == value, { onSelect(value) }, { Text("$value") })
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(low, style = MaterialTheme.typography.bodySmall)
        Text(high, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Selector(label: String, value: String, choices: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Spacer(Modifier.height(12.dp))
    Text(label, style = MaterialTheme.typography.labelLarge)
    Box {
        OutlinedButton({ expanded = true }, Modifier.fillMaxWidth()) { Text(value) }
        DropdownMenu(expanded, { expanded = false }) {
            choices.forEach { choice ->
                DropdownMenuItem({ Text(choice) }, { onSelect(choice); expanded = false })
            }
        }
    }
}

@Composable
private fun SearchableLineSelector(state: FeedbackFormState, choices: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember(state.transportType) { mutableStateOf("") }
    Spacer(Modifier.height(12.dp))
    Text("Line", style = MaterialTheme.typography.labelLarge)
    OutlinedButton(
        { if (state.transportType != null) expanded = true },
        Modifier.fillMaxWidth(),
        enabled = state.transportType != null
    ) {
        Text(state.line ?: "Select line")
    }
    DropdownMenu(expanded, { expanded = false }) {
        OutlinedTextField(query, { query = it }, label = { Text("Filter lines") }, modifier = Modifier.padding(8.dp))
        choices.filter { it.contains(query, ignoreCase = true) }.take(20).forEach { line ->
            DropdownMenuItem({ Text(line) }, { onSelect(line); expanded = false })
        }
    }
}

private fun locationMessage(state: LocationState) = when (state) {
    LocationState.Idle, LocationState.PermissionRequired -> "Location permission required"
    LocationState.RequestingPermission -> "Requesting location permission..."
    LocationState.Loading -> "Getting location..."
    is LocationState.Available -> "Location available"
    LocationState.PermissionDenied -> "Location permission denied"
    LocationState.Error -> "Unable to get location"
}
