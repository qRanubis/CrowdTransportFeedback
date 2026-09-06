package com.example.crowdtransportfeedback.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import java.util.Locale

@Composable
fun AddFeedbackScreen(vm: FeedbackViewModel, onSaved: () -> Unit, onCancel: () -> Unit) {
    val state by vm.formState.collectAsState()
    val context = LocalContext.current
    val provider = remember { AndroidLocationProvider(context) }
    var navigationHandled by rememberSaveable { mutableStateOf(false) }

    fun hasPermission() = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

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

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) fetch() else vm.setLocationState(LocationState.PermissionDenied)
    }

    fun requestPermission() {
        vm.setLocationState(LocationState.RequestingPermission)
        launcher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    fun cancelAndNavigateBack() {
        if (!state.isSubmitting && !navigationHandled) {
            navigationHandled = true
            vm.resetFeedbackForm()
            onCancel()
        }
    }

    BackHandler(enabled = true) {
        if (!state.isSubmitting) cancelAndNavigateBack()
    }

    LaunchedEffect(Unit) {
        if (hasPermission()) {
            fetch()
        } else if (state.locationState !is LocationState.PermissionDenied) {
            requestPermission()
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("Add feedback", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Selector(state.transportType?.displayName ?: "Select transport", TransportType.entries.map { it.displayName }, Modifier.weight(.45f)) { label -> vm.setTransportType(TransportType.entries.first { it.displayName == label }) }
          SearchableLineSelector(state, state.transportType?.let(BucharestTransitCatalog::linesFor).orEmpty(), vm::setLine, Modifier.weight(.55f))
        }

        RatingSelector(
            "Punctuality",
            "1 = Very poor",
            "5 = Very good",
            state.punctualityScore,
            vm::setPunctuality
        )
        RatingSelector(
            "Cleanliness",
            "1 = Very dirty",
            "5 = Very clean",
            state.cleanlinessScore,
            vm::setCleanliness
        )
        RatingSelector(
            "Crowding comfort",
            "1 = Extremely crowded",
            "5 = Plenty of space",
            state.crowdingScore,
            vm::setCrowding
        )

        Spacer(Modifier.height(12.dp)); Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("Overall rating", style = MaterialTheme.typography.labelLarge)
        Text(
            state.overallRating?.let { String.format(Locale.US, "%.1f / 5", it) }
                ?: "Select the three ratings above",
            style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary
        ) } }
        Text(
            "Calculated automatically from punctuality, cleanliness and crowding comfort.",
            style = MaterialTheme.typography.bodySmall
        )

        OutlinedTextField(
            state.comment,
            vm::setComment,
            label = { Text("Comment (optional)") },
            modifier = Modifier.fillMaxWidth().height(120.dp)
        )

        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("Location", style = MaterialTheme.typography.titleMedium)
        Text(locationMessage(state.locationState), color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(
            onClick = { if (hasPermission()) fetch() else requestPermission() },
            enabled = !state.isSubmitting && state.locationState !is LocationState.Loading
        ) {
            Text(if (hasPermission()) "Retry location" else "Allow location")
        } } }

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = ::cancelAndNavigateBack, enabled = !state.isSubmitting, modifier = Modifier.weight(1f).heightIn(min=48.dp)) { Text("Cancel") }
            Button(
                onClick = {
                    vm.submit {
                        if (!navigationHandled) {
                            navigationHandled = true
                            onSaved()
                        }
                    }
                },
                enabled = state.isValid && !state.isSubmitting, modifier = Modifier.weight(1f).heightIn(min=48.dp)
            ) {
                Text(if (state.isSubmitting) "Saving…" else "Save feedback")
            }
        }

        if (!state.isValid) {
            Text(
                "The three ratings, transport type, line, and location are required.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun RatingSelector(
    label: String,
    low: String,
    high: String,
    selected: Int?,
    onSelect: (Int) -> Unit
) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Selector(
    value: String,
    choices: List<String>,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    OutlinedButton({ expanded = true }, modifier.heightIn(min=56.dp)) { Text(value) }
    if (expanded) ModalBottomSheet(onDismissRequest = { expanded = false }) { Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(4.dp)) { Text("Choose transport type", style=MaterialTheme.typography.titleLarge); choices.forEach { choice -> TextButton({onSelect(choice);expanded=false}, Modifier.fillMaxWidth().heightIn(min=48.dp)) { Text(choice, Modifier.fillMaxWidth()) } }; Spacer(Modifier.height(24.dp)) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchableLineSelector(
    state: FeedbackFormState,
    choices: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember(state.transportType) { mutableStateOf("") }

    OutlinedButton({ expanded = true }, enabled = state.transportType != null && !state.isSubmitting, modifier=modifier.heightIn(min=56.dp)) { Text(state.line ?: "Select line") }
    if (expanded) ModalBottomSheet(onDismissRequest = { expanded = false }) {
        Column(Modifier.fillMaxWidth().heightIn(max=560.dp).padding(horizontal=16.dp)) {
            Text("Choose line", style=MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search lines…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
            LazyColumn(Modifier.weight(1f)) { items(filterLines(choices, query)) { line ->
                DropdownMenuItem(
                    text = { Text(line) },
                    onClick = {
                        onSelect(line)
                        query = ""
                        expanded = false
                    }
                )
            } }
        }
    }
}

internal fun filterLines(choices: List<String>, query: String): List<String> {
    val normalized = query.trim()
    return if (normalized.isEmpty()) {
        choices
    } else {
        choices.filter { it.startsWith(normalized, ignoreCase = true) }
    }
}

internal fun locationMessage(state: LocationState): String = when (state) {
    LocationState.Idle, LocationState.PermissionRequired -> "Location permission required"
    LocationState.RequestingPermission -> "Requesting location permission..."
    LocationState.Loading -> "Getting location..."
    is LocationState.Available -> String.format(
        Locale.US,
        "%.5f, %.5f",
        state.latitude,
        state.longitude
    )
    LocationState.PermissionDenied -> "Location permission denied"
    LocationState.Error -> "Unable to get location"
}
