package com.nextserve.serveitpartnernew.data.repository

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.nextserve.serveitpartnernew.data.service.LocationService
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

class LocationRepository(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) {

    /**
     * Get current location with permission check
     */
    suspend fun getCurrentLocation(): Result<android.location.Location> {
        return try {
            if (!hasLocationPermission()) {
                return Result.failure(Exception("Location permission not granted"))
            }

            val location = suspendCancellableCoroutine { continuation ->
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).addOnSuccessListener { location ->
                    if (location != null) {
                        continuation.resume(location)
                    } else {
                        continuation.resume(null)
                    }
                }.addOnFailureListener { exception ->
                    continuation.resume(null)
                }
            }

            if (location != null) {
                Result.success(location)
            } else {
                Result.failure(Exception("Unable to get current location"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get address from location coordinates
     */
    suspend fun getAddressFromLocation(latitude: Double, longitude: Double): Result<String> {
        return try {
            val geocoder = Geocoder(context)
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)

            if (addresses?.isNotEmpty() == true) {
                val address = addresses[0]
                val addressText = buildAddressString(address)
                Result.success(addressText)
            } else {
                Result.failure(Exception("No address found for location"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Start background location tracking
     */
    fun startLocationTracking(providerId: String) {
        if (!hasLocationPermission()) return

        val intent = Intent(context, LocationService::class.java).apply {
            action = LocationService.ACTION_START_TRACKING
            putExtra(LocationService.EXTRA_PROVIDER_ID, providerId)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    /**
     * Stop background location tracking
     */
    fun stopLocationTracking() {
        val intent = Intent(context, LocationService::class.java).apply {
            action = LocationService.ACTION_STOP_TRACKING
        }
        context.startService(intent)
    }

    /**
     * Start location tracking for a specific job
     */
    fun startJobLocationTracking(providerId: String, jobId: String) {
        if (!hasLocationPermission()) return

        val intent = Intent(context, LocationService::class.java).apply {
            action = LocationService.ACTION_START_JOB_TRACKING
            putExtra(LocationService.EXTRA_PROVIDER_ID, providerId)
            putExtra(LocationService.EXTRA_JOB_ID, jobId)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    /**
     * Stop job-specific location tracking
     */
    fun stopJobLocationTracking() {
        val intent = Intent(context, LocationService::class.java).apply {
            action = LocationService.ACTION_STOP_JOB_TRACKING
        }
        context.startService(intent)
    }

    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Calculate distance between two points using Haversine formula
     */
    fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadius = 6371.0 // Earth's radius in kilometers

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return earthRadius * c
    }

    /**
     * Get location accuracy level
     */
    fun getLocationAccuracy(location: android.location.Location): String {
        return when {
            location.accuracy < 10 -> "High"
            location.accuracy < 50 -> "Medium"
            location.accuracy < 100 -> "Low"
            else -> "Poor"
        }
    }

    private fun buildAddressString(address: Address): String {
        val addressParts = mutableListOf<String>()

        // Add feature name (building/landmark)
        address.featureName?.let { addressParts.add(it) }

        // Add thoroughfare (street)
        address.thoroughfare?.let { addressParts.add(it) }

        // Add locality (city)
        address.locality?.let { addressParts.add(it) }

        // Add admin area (state)
        address.adminArea?.let { addressParts.add(it) }

        // Add postal code
        address.postalCode?.let { addressParts.add(it) }

        // Add country
        address.countryName?.let { addressParts.add(it) }

        return addressParts.joinToString(", ")
    }
}