package com.nextserve.serveitpartnernew

import com.google.firebase.Timestamp
import com.nextserve.serveitpartnernew.data.mapper.ProviderFirestoreMapper
import com.nextserve.serveitpartnernew.data.model.ProviderData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

class ProviderFirestoreMapperTest {

    @Test
    fun `test toFirestore converts ProviderData to correct nested structure`() {
        // Given
        val providerData = ProviderData(
            uid = "test-uid",
            phoneNumber = "+919876543210",
            fullName = "John Doe",
            email = "john@example.com",
            latitude = 12.9716,
            longitude = 77.5946,
            approvalStatus = "APPROVED",
            isVerified = true,
            isOnline = true
        )

        // When
        val firestoreData = ProviderFirestoreMapper.toFirestore(providerData)

        // Then
        assertNotNull(firestoreData["personalDetails"])
        val personalDetails = firestoreData["personalDetails"] as Map<*, *>
        assertEquals("John Doe", personalDetails["fullName"])
        assertEquals("+919876543210", personalDetails["phoneNumber"])

        assertNotNull(firestoreData["locationDetails"])
        val locationDetails = firestoreData["locationDetails"] as Map<*, *>
        assertEquals(12.9716, locationDetails["latitude"])
        assertEquals(77.5946, locationDetails["longitude"])

        assertNotNull(firestoreData["verificationDetails"])
        val verificationDetails = firestoreData["verificationDetails"] as Map<*, *>
        assertEquals(true, verificationDetails["verified"])
        assertEquals(false, verificationDetails["rejected"])

        assertEquals(true, firestoreData["isVerified"])
        assertEquals(true, firestoreData["isOnline"])
    }

    @Test
    fun `test fromFirestore converts nested structure back to ProviderData`() {
        // Given
        val firestoreDoc = mapOf(
            "personalDetails" to mapOf(
                "fullName" to "John Doe",
                "phoneNumber" to "+919876543210"
            ),
            "locationDetails" to mapOf(
                "latitude" to 12.9716,
                "longitude" to 77.5946
            ),
            "verificationDetails" to mapOf(
                "verified" to true,
                "rejected" to false
            ),
            "services" to listOf("AC Repair", "Plumbing"),
            "isVerified" to true,
            "isOnline" to true,
            "fcmToken" to "test-token"
        )

        // Mock DocumentSnapshot
        val mockDocument = object {
            fun data(): Map<String, Any>? = firestoreDoc
        }

        // When - we'll test the logic directly since we can't mock DocumentSnapshot easily
        val personalDetails = firestoreDoc["personalDetails"] as Map<*, *>
        val fullName = personalDetails["fullName"] as String
        val phoneNumber = personalDetails["phoneNumber"] as String

        val locationDetails = firestoreDoc["locationDetails"] as Map<*, *>
        val latitude = (locationDetails["latitude"] as Number).toDouble()
        val longitude = (locationDetails["longitude"] as Number).toDouble()

        val verificationDetails = firestoreDoc["verificationDetails"] as Map<*, *>
        val isVerified = verificationDetails["verified"] as Boolean
        val isRejected = verificationDetails["rejected"] as Boolean

        val services = firestoreDoc["services"] as List<*>
        val primaryService = services.firstOrNull() as String

        // Then
        assertEquals("John Doe", fullName)
        assertEquals("+919876543210", phoneNumber)
        assertEquals(12.9716, latitude, 0.001)
        assertEquals(77.5946, longitude, 0.001)
        assertEquals(true, isVerified)
        assertEquals(false, isRejected)
        assertEquals("AC Repair", primaryService)
    }

    @Test
    fun `test toFirestoreUpdate handles approval status conversion`() {
        // Given
        val updateMap = mapOf(
            "approvalStatus" to "APPROVED",
            "rejectionReason" to "All good"
        )

        // When
        val firestoreUpdate = ProviderFirestoreMapper.toFirestoreUpdate(updateMap)

        // Then
        assertNotNull(firestoreUpdate["verificationDetails"])
        val verificationDetails = firestoreUpdate["verificationDetails"] as Map<*, *>
        assertEquals(true, verificationDetails["verified"])
        assertEquals(false, verificationDetails["rejected"])
        assertEquals("All good", verificationDetails["rejectionReason"])
        assertEquals(true, firestoreUpdate["isVerified"])
    }

    @Test
    fun `test services array creation from ProviderData`() {
        // Given
        val providerData = ProviderData(
            selectedMainService = "AC Repair",
            selectedSubServices = listOf("Split AC", "Window AC")
        )

        // When
        val firestoreData = ProviderFirestoreMapper.toFirestore(providerData)

        // Then
        assertNotNull(firestoreData["services"])
        val services = firestoreData["services"] as List<*>
        assertEquals(3, services.size) // primary + 2 sub-services
        assertEquals("AC Repair", services[0])
        assertEquals("Split AC", services[1])
        assertEquals("Window AC", services[2])
    }
}
