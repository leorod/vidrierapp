package com.vidrierapp.data.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.vidrierapp.domain.location.LocationProvider
import com.vidrierapp.domain.model.GeoPoint
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class FusedLocationProvider(context: Context) : LocationProvider {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override suspend fun currentLocation(): GeoPoint? = try {
        lastKnown() ?: current()
    } catch (_: SecurityException) {
        null
    }

    @SuppressLint("MissingPermission")
    private suspend fun lastKnown(): GeoPoint? = suspendCancellableCoroutine { continuation ->
        client.lastLocation
            .addOnSuccessListener { location ->
                continuation.resume(location?.let { GeoPoint(it.latitude, it.longitude) })
            }
            .addOnFailureListener { continuation.resume(null) }
    }

    @SuppressLint("MissingPermission")
    private suspend fun current(): GeoPoint? = suspendCancellableCoroutine { continuation ->
        val cancellation = CancellationTokenSource()
        continuation.invokeOnCancellation { cancellation.cancel() }
        client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token)
            .addOnSuccessListener { location ->
                continuation.resume(location?.let { GeoPoint(it.latitude, it.longitude) })
            }
            .addOnFailureListener { continuation.resume(null) }
    }
}
