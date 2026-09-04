package com.example.crowdtransportfeedback.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

data class Coordinates(val latitude: Double, val longitude: Double)

interface LocationProvider {
    fun getLocation(onResult: (Result<Coordinates>) -> Unit)
}

class AndroidLocationProvider(context: Context) : LocationProvider {
    private val client: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext)

    @SuppressLint("MissingPermission")
    override fun getLocation(onResult: (Result<Coordinates>) -> Unit) {
        try {
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { current ->
                    if (current != null) onResult(Result.success(Coordinates(current.latitude, current.longitude)))
                    else getLastLocation(onResult)
                }
                .addOnFailureListener { getLastLocation(onResult) }
        } catch (error: SecurityException) {
            onResult(Result.failure(error))
        } catch (error: Exception) {
            onResult(Result.failure(error))
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation(onResult: (Result<Coordinates>) -> Unit) {
        try {
            client.lastLocation
                .addOnSuccessListener { location ->
                    if (location == null) onResult(Result.failure(IllegalStateException("No location available")))
                    else onResult(Result.success(Coordinates(location.latitude, location.longitude)))
                }
                .addOnFailureListener { onResult(Result.failure(it)) }
        } catch (error: Exception) {
            onResult(Result.failure(error))
        }
    }
}
