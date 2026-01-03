package com.nextserve.serveitpartnernew.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class ProviderData(
    // Auth Info
    val uid: String = "",
    val phoneNumber: String = "",
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Timestamp? = null,
    @get:PropertyName("lastLoginAt") @set:PropertyName("lastLoginAt")
    var lastLoginAt: Timestamp? = null,

    // Onboarding Status
    val onboardingStatus: String = "IN_PROGRESS", // IN_PROGRESS, SUBMITTED, APPROVED, REJECTED
    val currentStep: Int = 1,
    @get:PropertyName("submittedAt") @set:PropertyName("submittedAt")
    var submittedAt: Timestamp? = null,
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAt: Timestamp? = null,

    // Step 1 - Basic Info
    val fullName: String = "",
    val gender: String = "",
    val primaryService: String = "",
    val email: String = "",

    // Step 2 - Services
    val selectedMainService: String = "",
    val selectedSubServices: List<String> = emptyList(),
    val otherService: String = "",

    // Step 3 - Location
    val state: String = "",
    val city: String = "",
    val address: String = "",
    val fullAddress: String = "",
    val pincode: String = "",
    val serviceRadius: Double = 5.0,
    val latitude: Double? = null,
    val longitude: Double? = null,

    // Step 4 - Documents
    val aadhaarFrontUrl: String = "",
    val aadhaarBackUrl: String = "",
    @get:PropertyName("documentsUploadedAt") @set:PropertyName("documentsUploadedAt")
    var documentsUploadedAt: Timestamp? = null,

    // Admin Review
    val approvalStatus: String = "PENDING", // PENDING, APPROVED, REJECTED
    val rejectionReason: String? = null,
    @get:PropertyName("reviewedAt") @set:PropertyName("reviewedAt")
    var reviewedAt: Timestamp? = null,
    val reviewedBy: String? = null,
    
    // FCM Token
    val fcmToken: String = "",
    
    // Language Preference
    val language: String = "en", // en, hi, mr
    
    // Profile Photo
    val profilePhotoUrl: String = "",
    
    // Cloud Functions Required Fields
    val isVerified: Boolean = false, // Required by Cloud Functions at root level
    val isOnline: Boolean = false // Required by Cloud Functions at root level
)

