package com.nextserve.serveitpartnernew.data.repository

import android.content.Context
import android.location.Address
import android.location.Geocoder
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import java.util.Locale

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val state: String,
    val city: String,
    val address: String,
    val fullAddress: String,
    val pincode: String
)

class LocationRepository(
    private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    suspend fun getCurrentLocation(): Result<android.location.Location> {
        return try {
            val cancellationToken = CancellationTokenSource()
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationToken.token
            ).await()
            
            if (location != null) {
                Result.success(location)
            } else {
                Result.failure(Exception("Location is null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reverseGeocode(latitude: Double, longitude: Double): Result<LocationData> {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            
            if (addresses.isNullOrEmpty()) {
                return Result.failure(Exception("No address found"))
            }

            val address = addresses[0]
            val locationData = LocationData(
                latitude = latitude,
                longitude = longitude,
                state = address.adminArea ?: "",
                city = address.locality ?: address.subAdminArea ?: "",
                address = buildAddressLine(address),
                fullAddress = address.getAddressLine(0) ?: "",
                pincode = address.postalCode ?: ""
            )
            
            Result.success(locationData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildAddressLine(address: Address): String {
        val parts = mutableListOf<String>()
        address.featureName?.let { parts.add(it) }
        address.thoroughfare?.let { parts.add(it) }
        address.subLocality?.let { parts.add(it) }
        return parts.joinToString(", ")
    }

    suspend fun getCurrentLocationWithAddress(): Result<LocationData> {
        return try {
            val location = getCurrentLocation().getOrThrow()
            reverseGeocode(location.latitude, location.longitude)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

