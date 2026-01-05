package com.nextserve.serveitpartnernew.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Simplified service model for UI consumption
 * Only contains names to avoid dependency on Firestore schema consistency
 */
data class MainService(
    val id: String = "",
    val name: String = "",
    val subServiceNames: List<String> = emptyList()
)

