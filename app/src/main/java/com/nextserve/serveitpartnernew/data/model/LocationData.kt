package com.nextserve.serveitpartnernew.data.model

/**
 * Data class representing location information
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val pincode: String? = null,
    val fullAddress: String? = null
) {
    /**
     * Returns formatted address for display
     */
    fun getDisplayAddress(): String {
        return fullAddress ?: buildString {
            address?.let { append("$it, ") }
            city?.let { append("$it, ") }
            state?.let { append("$it, ") }
            pincode?.let { append(it) }
        }.trimEnd(',', ' ')
    }

    /**
     * Checks if location data is valid
     */
    fun isValid(): Boolean {
        return latitude != 0.0 && longitude != 0.0
    }
}
