package com.example.crowdtransportfeedback.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.crowdtransportfeedback.BuildConfig
import com.example.crowdtransportfeedback.domain.TransportType
import com.example.crowdtransportfeedback.location.AndroidLocationProvider
import com.example.crowdtransportfeedback.location.Coordinates
import com.example.crowdtransportfeedback.ui.map.MapFilter
import com.example.crowdtransportfeedback.ui.map.visibleMapMarkers
import com.example.crowdtransportfeedback.ui.viewmodel.FeedbackViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.util.Locale
import kotlinx.coroutines.launch

private val Bucharest = LatLng(44.4268, 26.1025)

@Composable
fun MapScreen(vm: FeedbackViewModel, onFeedbackClick: (Long) -> Unit) {
    if (!BuildConfig.MAPS_API_KEY_CONFIGURED) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Google Maps is not configured. Add MAPS_API_KEY to local.properties and rebuild the app.")
        }
        return
    }

    val context = LocalContext.current
    val feedback by vm.feedbackList.collectAsState()
    var filter by remember { mutableStateOf(MapFilter.ALL) }
    val markers = remember(feedback, filter) { visibleMapMarkers(feedback, filter) }
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(Bucharest, 12f)
    }
    val scope = rememberCoroutineScope()
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    var currentLocation by remember { mutableStateOf<Coordinates?>(null) }
    var pendingFeedbackId by remember { mutableStateOf<Long?>(null) }

    fun locate() {
        if (!hasPermission) return
        AndroidLocationProvider(context).getLocation { result ->
            result.getOrNull()?.let { location ->
                currentLocation = location
                scope.launch { cameraState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 15f)) }
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasPermission = grants.values.any { it }
        if (hasPermission) locate()
    }
    LaunchedEffect(hasPermission) { if (hasPermission && currentLocation == null) locate() }
    LaunchedEffect(pendingFeedbackId) {
        pendingFeedbackId?.let { id ->
            pendingFeedbackId = null
            onFeedbackClick(id)
        }
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            properties = MapProperties(isMyLocationEnabled = hasPermission),
            uiSettings = MapUiSettings(myLocationButtonEnabled = false)
        ) {
            markers.forEach { marker ->
                key(marker.localId) {
                    val markerState = rememberMarkerState(
                        key = marker.localId.toString(),
                        position = LatLng(marker.latitude, marker.longitude)
                    )
                    Marker(
                        state = markerState,
                        title = "${marker.transportType.displayName} ${marker.line}",
                        snippet = String.format(Locale.US, "Overall rating: %.1f/5 · @%s", marker.overallRating, marker.publicUsername),
                        icon = BitmapDescriptorFactory.defaultMarker(markerHue(marker.transportType)),
                        onInfoWindowClick = { pendingFeedbackId = marker.localId }
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MapFilter.entries.forEach { option ->
                FilterChip(selected = filter == option, onClick = { filter = option }, label = { Text(option.label) })
            }
        }
        Button(
            onClick = {
                if (hasPermission) locate() else permissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) { Text(if (hasPermission) "My location" else "Enable location") }
    }
}

internal fun markerHue(type: TransportType): Float = when (type) {
    TransportType.BUS -> BitmapDescriptorFactory.HUE_BLUE
    TransportType.METRO -> BitmapDescriptorFactory.HUE_RED
    TransportType.TRAM -> BitmapDescriptorFactory.HUE_ORANGE
    TransportType.TROLLEYBUS -> BitmapDescriptorFactory.HUE_GREEN
    TransportType.NIGHT_BUS -> BitmapDescriptorFactory.HUE_VIOLET
}
