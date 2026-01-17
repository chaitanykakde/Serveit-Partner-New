package com.nextserve.serveitpartnernew.data.mapper

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.nextserve.serveitpartnernew.data.model.ProviderData
import com.nextserve.serveitpartnernew.data.model.VerificationDetails

/**
 * Transformation layer to convert between:
 * - Flat App Model (ProviderData) ↔ Nested Firestore Document (partners/{uid})
 * 
 * This mapper ensures the app writes data in the exact format expected by Cloud Functions.
 * Cloud Functions expect nested structure in 'partners' collection.
 */
object ProviderFirestoreMapper {

    /**
     * Converts flat ProviderData model to nested Firestore document structure.
     * This is used when WRITING to Firestore (partners collection).
     * 
     * @param providerData Flat app model
     * @return Map representing nested Firestore document structure
     */
    fun toFirestore(providerData: ProviderData): Map<String, Any> {
        val firestoreData = mutableMapOf<String, Any>()

        // ============================================
        // PERSONAL DETAILS (Nested)
        // ============================================
        val personalDetails = mutableMapOf<String, Any>()
        if (providerData.fullName.isNotEmpty()) {
            personalDetails["fullName"] = providerData.fullName
        }
        if (providerData.phoneNumber.isNotEmpty()) {
            personalDetails["phoneNumber"] = providerData.phoneNumber
            // REQUIRED for Cloud Function acceptJobRequest compatibility
            personalDetails["mobileNo"] = providerData.phoneNumber
        }
        if (providerData.gender.isNotEmpty()) {
            personalDetails["gender"] = providerData.gender
        }
        if (personalDetails.isNotEmpty()) {
            firestoreData["personalDetails"] = personalDetails
        }

        // ============================================
        // LOCATION DETAILS (Nested)
        // ============================================
        val locationDetails = mutableMapOf<String, Any>()
        providerData.latitude?.let {
            locationDetails["latitude"] = it
        }
        providerData.longitude?.let {
            locationDetails["longitude"] = it
        }
        if (providerData.fullAddress.isNotEmpty()) {
            locationDetails["address"] = providerData.fullAddress
        }
        if (locationDetails.isNotEmpty()) {
            firestoreData["locationDetails"] = locationDetails
        }

        // ============================================
        // SERVICES ARRAY (Root level)
        // Combine primaryService + selectedSubServices
        // ============================================
        val services = mutableListOf<String>()
        if (providerData.primaryService.isNotEmpty()) {
            services.add(providerData.primaryService)
        }
        // Add sub-services, avoiding duplicates
        providerData.selectedSubServices.forEach { subService ->
            if (subService.isNotEmpty() && !services.contains(subService)) {
                services.add(subService)
            }
        }
        if (services.isNotEmpty()) {
            firestoreData["services"] = services
        }

        // ============================================
        // VERIFICATION DETAILS (Nested Map)
        // SINGLE SOURCE OF TRUTH for verification status
        // ============================================
        val verificationDetailsMap = mutableMapOf<String, Any>()
        verificationDetailsMap["status"] = providerData.verificationDetails.status
        
        providerData.verificationDetails.rejectedReason?.let { reason ->
            if (reason.isNotEmpty()) {
                verificationDetailsMap["rejectedReason"] = reason
            }
        }
        
        providerData.verificationDetails.verifiedBy?.let { verifiedBy ->
            verificationDetailsMap["verifiedBy"] = verifiedBy
        }
        
        providerData.verificationDetails.verifiedAt?.let { verifiedAt ->
            verificationDetailsMap["verifiedAt"] = verifiedAt
        }
        
        firestoreData["verificationDetails"] = verificationDetailsMap

        // ============================================
        // ROOT LEVEL FIELDS
        // ============================================
        
        // isVerified: boolean (root level - for backward compatibility with Cloud Functions)
        firestoreData["isVerified"] = providerData.verificationDetails.status == "verified"
        
        // isOnline: boolean (default false)
        firestoreData["isOnline"] = false
        
        // FCM Token
        if (providerData.fcmToken.isNotEmpty()) {
            firestoreData["fcmToken"] = providerData.fcmToken
        }
        
        // Service Radius
        firestoreData["serviceRadius"] = providerData.serviceRadius
        
        // Language
        if (providerData.language.isNotEmpty()) {
            firestoreData["language"] = providerData.language
        }
        
        // ============================================
        // ONBOARDING STATUS FIELDS (Keep for app use)
        // ============================================
        if (providerData.onboardingStatus.isNotEmpty()) {
            firestoreData["onboardingStatus"] = providerData.onboardingStatus
        }
        firestoreData["currentStep"] = providerData.currentStep
        
        // Timestamps
        providerData.createdAt?.let { firestoreData["createdAt"] = it }
        providerData.lastLoginAt?.let { firestoreData["lastLoginAt"] = it }
        providerData.submittedAt?.let { firestoreData["submittedAt"] = it }
        providerData.updatedAt?.let { firestoreData["updatedAt"] = it }
        providerData.documentsUploadedAt?.let { firestoreData["documentsUploadedAt"] = it }
        providerData.verificationDetails.verifiedAt?.let { firestoreData["verifiedAt"] = it }
        
        // Additional fields for app functionality
        if (providerData.email.isNotEmpty()) {
            firestoreData["email"] = providerData.email
        }
        if (providerData.selectedMainService.isNotEmpty()) {
            firestoreData["selectedMainService"] = providerData.selectedMainService
        }
        if (providerData.otherService.isNotEmpty()) {
            firestoreData["otherService"] = providerData.otherService
        }
        if (providerData.state.isNotEmpty()) {
            firestoreData["state"] = providerData.state
        }
        if (providerData.city.isNotEmpty()) {
            firestoreData["city"] = providerData.city
        }
        if (providerData.address.isNotEmpty()) {
            firestoreData["address"] = providerData.address
        }
        if (providerData.pincode.isNotEmpty()) {
            firestoreData["pincode"] = providerData.pincode
        }
        if (providerData.aadhaarFrontUrl.isNotEmpty()) {
            firestoreData["aadhaarFrontUrl"] = providerData.aadhaarFrontUrl
        }
        if (providerData.aadhaarBackUrl.isNotEmpty()) {
            firestoreData["aadhaarBackUrl"] = providerData.aadhaarBackUrl
        }
        // reviewedBy is now in verificationDetails.verifiedBy (already handled above)
        if (providerData.profilePhotoUrl.isNotEmpty()) {
            firestoreData["profilePhotoUrl"] = providerData.profilePhotoUrl
        }

        return firestoreData
    }

    /**
     * Converts partial update map (from app) to nested Firestore structure.
     * Used for incremental updates during onboarding.
     * 
     * @param updateMap Flat update map from app
     * @return Nested Firestore update map
     */
    fun toFirestoreUpdate(updateMap: Map<String, Any>): Map<String, Any> {
        val firestoreUpdate = mutableMapOf<String, Any>()

        // Handle location fields
        val locationDetails = mutableMapOf<String, Any>()
        var hasLocation = false
        
        updateMap["latitude"]?.let {
            locationDetails["latitude"] = it
            hasLocation = true
        }
        updateMap["longitude"]?.let {
            locationDetails["longitude"] = it
            hasLocation = true
        }
        updateMap["fullAddress"]?.let {
            locationDetails["address"] = it
            hasLocation = true
        }
        if (hasLocation) {
            firestoreUpdate["locationDetails"] = locationDetails
        }

        // Handle personal details
        val personalDetails = mutableMapOf<String, Any>()
        var hasPersonal = false
        
        updateMap["fullName"]?.let {
            personalDetails["fullName"] = it
            hasPersonal = true
        }
        updateMap["phoneNumber"]?.let {
            personalDetails["phoneNumber"] = it
            // REQUIRED for Cloud Function acceptJobRequest compatibility
            personalDetails["mobileNo"] = it
            hasPersonal = true
        }
        updateMap["gender"]?.let {
            personalDetails["gender"] = it
            hasPersonal = true
        }
        if (hasPersonal) {
            firestoreUpdate["personalDetails"] = personalDetails
        }

        // Handle services array
        val services = mutableListOf<String>()
        // Use primaryService if available, otherwise use selectedMainService
        val primaryService = (updateMap["primaryService"] as? String) ?: (updateMap["selectedMainService"] as? String)
        primaryService?.let { primary ->
            if (primary.isNotEmpty()) {
                services.add(primary)
            }
        }
        (updateMap["selectedSubServices"] as? List<*>)?.forEach { subService ->
            if (subService is String && subService.isNotEmpty() && !services.contains(subService)) {
                services.add(subService)
            }
        }
        if (services.isNotEmpty()) {
            firestoreUpdate["services"] = services
        }

        // Handle verification status conversion
        updateMap["approvalStatus"]?.let { status ->
            if (status is String) {
                val isApproved = status == "APPROVED"
                val isRejected = status == "REJECTED"
                
                val verificationDetails = mutableMapOf<String, Any>()
                verificationDetails["verified"] = isApproved
                verificationDetails["rejected"] = isRejected
                
                updateMap["rejectionReason"]?.let { reason ->
                    if (reason is String && reason.isNotEmpty()) {
                        verificationDetails["rejectionReason"] = reason
                    }
                }
                
                firestoreUpdate["verificationDetails"] = verificationDetails
                firestoreUpdate["isVerified"] = isApproved
            }
        }

        // Copy other fields as-is (timestamps, onboarding status, etc.)
        updateMap.forEach { (key, value) ->
            // Skip fields that we've already transformed
            if (key !in listOf("latitude", "longitude", "fullAddress", 
                              "fullName", "phoneNumber", "gender",
                              "primaryService", "selectedSubServices",
                              "approvalStatus", "rejectionReason")) {
                firestoreUpdate[key] = value
            }
        }

        return firestoreUpdate
    }

    /**
     * Converts nested Firestore document to flat ProviderData model.
     * This is used when READING from Firestore.
     * 
     * @param document Firestore document snapshot
     * @param uid User ID (document ID)
     * @return ProviderData model (flat structure for app use)
     */
    fun fromFirestore(document: DocumentSnapshot, uid: String): ProviderData? {
        val data = document.data ?: return null

        // Extract nested personalDetails
        val personalDetails = data["personalDetails"] as? Map<*, *>
        val fullName = personalDetails?.get("fullName") as? String ?: ""
        val phoneNumber = personalDetails?.get("phoneNumber") as? String ?: ""
        val gender = personalDetails?.get("gender") as? String ?: ""

        // Extract nested locationDetails
        val locationDetails = data["locationDetails"] as? Map<*, *>
        val latitude = (locationDetails?.get("latitude") as? Number)?.toDouble()
        val longitude = (locationDetails?.get("longitude") as? Number)?.toDouble()
        val fullAddress = locationDetails?.get("address") as? String ?: ""

        // Extract services array
        val services = data["services"] as? List<*>
        val primaryService = services?.firstOrNull() as? String ?: ""
        val selectedSubServices = services?.drop(1)?.filterIsInstance<String>() ?: emptyList()

        // Extract verificationDetails map (SINGLE SOURCE OF TRUTH)
        val verificationDetailsMap = data["verificationDetails"] as? Map<*, *>
        
        // Read status from verificationDetails.status (preferred) or fallback to legacy fields
        val status = when {
            verificationDetailsMap?.get("status") != null -> 
                verificationDetailsMap["status"] as? String ?: "pending"
            // Legacy fallback: Check root level isVerified and nested booleans
            (data["isVerified"] as? Boolean) == true -> "verified"
            verificationDetailsMap?.get("verified") as? Boolean == true -> "verified"
            verificationDetailsMap?.get("rejected") as? Boolean == true -> "rejected"
            else -> "pending"
        }
        
        // Try both field names: "rejectedReason" (current) and "rejectionReason" (legacy)
        val rejectionReason = (verificationDetailsMap?.get("rejectedReason") as? String)
            ?: (verificationDetailsMap?.get("rejectionReason") as? String)
        val verifiedBy = verificationDetailsMap?.get("verifiedBy") as? String
        val verifiedAt = verificationDetailsMap?.get("verifiedAt") as? Timestamp
        
        val verificationDetails = VerificationDetails(
            status = status,
            rejectedReason = rejectionReason,
            verifiedBy = verifiedBy,
            verifiedAt = verifiedAt
        )
        
        // Extract isOnline from root level (Cloud Functions requirement)
        val isOnline = data["isOnline"] as? Boolean ?: false

        return ProviderData(
            uid = uid,
            phoneNumber = phoneNumber,
            createdAt = data["createdAt"] as? Timestamp,
            lastLoginAt = data["lastLoginAt"] as? Timestamp,
            onboardingStatus = data["onboardingStatus"] as? String ?: "IN_PROGRESS",
            currentStep = (data["currentStep"] as? Number)?.toInt() ?: 1,
            submittedAt = data["submittedAt"] as? Timestamp,
            updatedAt = data["updatedAt"] as? Timestamp,
            fullName = fullName,
            gender = gender,
            primaryService = primaryService,
            email = data["email"] as? String ?: "",
            selectedMainService = data["selectedMainService"] as? String ?: "",
            selectedSubServices = selectedSubServices,
            otherService = data["otherService"] as? String ?: "",
            state = data["state"] as? String ?: "",
            city = data["city"] as? String ?: "",
            address = data["address"] as? String ?: "",
            fullAddress = fullAddress,
            pincode = data["pincode"] as? String ?: "",
            serviceRadius = (data["serviceRadius"] as? Number)?.toDouble() ?: 5.0,
            latitude = latitude,
            longitude = longitude,
            aadhaarFrontUrl = data["aadhaarFrontUrl"] as? String ?: "",
            aadhaarBackUrl = data["aadhaarBackUrl"] as? String ?: "",
            documentsUploadedAt = data["documentsUploadedAt"] as? Timestamp,
            verificationDetails = verificationDetails,
            fcmToken = data["fcmToken"] as? String ?: "",
            language = data["language"] as? String ?: "en",
            profilePhotoUrl = data["profilePhotoUrl"] as? String ?: "",
            isOnline = isOnline
        )
    }
}

