package com.nextserve.serveitpartnernew.utils

import kotlin.math.*

/**
 * Distance calculation utilities using Haversine formula
 * Calculates straight-line distance between two coordinates
 */
object DistanceUtils {
    /**
     * Earth's radius in kilometers
     */
    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * Calculate distance between two coordinates using Haversine formula
     * @param lat1 Latitude of first point
     * @param lon1 Longitude of first point
     * @param lat2 Latitude of second point
     * @param lon2 Longitude of second point
     * @return Distance in kilometers, rounded to 1 decimal place
     */
    fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double? {
        android.util.Log.d("DistanceUtils", "📐 Calculating distance: ($lat1, $lon1) to ($lat2, $lon2)")
        
        // Validate coordinates
        if (!isValidCoordinate(lat1, lon1) || !isValidCoordinate(lat2, lon2)) {
            android.util.Log.w("DistanceUtils", "⚠️ Invalid coordinates: ($lat1, $lon1) or ($lat2, $lon2)")
            return null
        }

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distanceKm = EARTH_RADIUS_KM * c

        // Round to 1 decimal place, but ensure minimum 0.1 km for very small distances
        val roundedDistance = if (distanceKm < 0.1) {
            // For distances less than 0.1 km, show as 0.1 km (minimum display)
            0.1
        } else {
            String.format("%.1f", distanceKm).toDoubleOrNull() ?: distanceKm
        }
        android.util.Log.d("DistanceUtils", "✅ Calculated distance: $distanceKm km -> rounded: $roundedDistance km")
        return roundedDistance
    }

    /**
     * Check if coordinates are valid
     */
    private fun isValidCoordinate(lat: Double, lon: Double): Boolean {
        return lat in -90.0..90.0 && lon in -180.0..180.0
    }
}

