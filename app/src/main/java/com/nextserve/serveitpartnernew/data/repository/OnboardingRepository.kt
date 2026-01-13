package com.nextserve.serveitpartnernew.data.repository

import android.content.Context
import com.google.firebase.Timestamp
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.data.model.MainService
import com.nextserve.serveitpartnernew.data.model.ProviderData
import com.nextserve.serveitpartnernew.domain.onboarding.OnboardingStatus
import com.nextserve.serveitpartnernew.domain.onboarding.OnboardingStep
import kotlinx.coroutines.tasks.await

/**
 * Repository for onboarding data operations.
 * Handles all Firestore interactions for onboarding flow.
 * Clean separation from business logic and UI.
 */
class OnboardingRepository(
    private val context: Context,
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(FirebaseProvider.firestore),
    private val storageRepository: StorageRepository = StorageRepository(FirebaseProvider.storage, context)
) {

    /**
     * Load complete provider data for onboarding.
     */
    suspend fun loadProviderData(uid: String): Result<ProviderData?> {
        return firestoreRepository.getProviderData(uid)
    }

    /**
     * Update current onboarding step.
     */
    suspend fun updateCurrentStep(uid: String, step: Int): Result<Unit> {
        return firestoreRepository.updateCurrentStep(uid, step)
    }

    /**
     * Save step 1 data (basic info).
     */
    suspend fun saveStep1Data(
        uid: String,
        fullName: String,
        gender: String,
        email: String,
        language: String,
        primaryService: String
    ): Result<Unit> {
        val data = mapOf(
            "fullName" to fullName,
            "gender" to gender,
            "email" to email,
            "language" to language,
            "primaryService" to primaryService,
            "onboardingStatus" to OnboardingStatus.IN_PROGRESS.statusString,
            "currentStep" to OnboardingStep.BASIC_INFO.stepNumber
        )
        return firestoreRepository.updateProviderData(uid, data)
    }

    /**
     * Save step 2 data (service selection).
     */
    suspend fun saveStep2Data(
        uid: String,
        selectedMainService: String,
        selectedSubServices: Set<String>,
        otherService: String
    ): Result<Unit> {
        val data = mapOf(
            "selectedMainService" to selectedMainService,
            "selectedSubServices" to selectedSubServices.toList(),
            "otherService" to otherService,
            "currentStep" to OnboardingStep.SERVICE_SELECTION.stepNumber
        )
        return firestoreRepository.updateProviderData(uid, data)
    }

    /**
     * Save step 3 data (location).
     */
    suspend fun saveStep3Data(
        uid: String,
        state: String,
        city: String,
        address: String,
        fullAddress: String,
        pincode: String,
        serviceRadius: Float,
        latitude: Double?,
        longitude: Double?
    ): Result<Unit> {
        val data = mutableMapOf<String, Any>(
            "state" to state,
            "city" to city,
            "address" to address,
            "fullAddress" to fullAddress,
            "pincode" to pincode,
            "serviceRadius" to serviceRadius.toDouble(),
            "currentStep" to OnboardingStep.LOCATION.stepNumber
        )

        latitude?.let { data["latitude"] = it }
        longitude?.let { data["longitude"] = it }
        return firestoreRepository.updateProviderData(uid, data)
    }

    /**
     * Submit complete onboarding application.
     */
    suspend fun submitOnboarding(uid: String, finalData: Map<String, Any>): Result<Unit> {
        val submissionData = finalData.toMutableMap().apply {
            put("onboardingStatus", OnboardingStatus.SUBMITTED.statusString)
            put("approvalStatus", "PENDING")
            put("submittedAt", Timestamp.now())
            put("currentStep", OnboardingStep.REVIEW.stepNumber)
        }
        return firestoreRepository.updateProviderData(uid, submissionData)
    }

    /**
     * Load main services from Firestore.
     * Delegates to FirestoreRepository for actual implementation.
     */
    suspend fun loadMainServices(gender: String): Result<List<MainService>> {
        return firestoreRepository.getMainServices(gender)
    }

    /**
     * Load sub-services for a given gender and main service.
     * Delegates to FirestoreRepository for actual implementation.
     */
    suspend fun loadSubServices(gender: String, mainService: String): Result<List<String>> {
        return firestoreRepository.getSubServices(gender, mainService)
    }

    /**
     * Upload Aadhaar front image.
     */
    suspend fun uploadAadhaarFront(uid: String, imageUri: android.net.Uri): Result<String> {
        return storageRepository.uploadAadhaarDocument(uid, "front", imageUri) { _ -> }
            .onSuccess { downloadUrl ->
                firestoreRepository.updateProviderData(uid, mapOf("aadhaarFrontUrl" to downloadUrl))
            }
    }

    /**
     * Upload Aadhaar back image.
     */
    suspend fun uploadAadhaarBack(uid: String, imageUri: android.net.Uri): Result<String> {
        return storageRepository.uploadAadhaarDocument(uid, "back", imageUri) { _ -> }
            .onSuccess { downloadUrl ->
                firestoreRepository.updateProviderData(uid, mapOf("aadhaarBackUrl" to downloadUrl))
            }
    }

    /**
     * Upload profile photo.
     */
    suspend fun uploadProfilePhoto(uid: String, imageUri: android.net.Uri): Result<String> {
        return storageRepository.uploadProfilePhoto(uid, imageUri) { _ -> }
            .onSuccess { downloadUrl ->
                firestoreRepository.updateProviderData(uid, mapOf("profilePhotoUrl" to downloadUrl))
            }
    }

    /**
     * Delete Aadhaar front image.
     */
    suspend fun deleteAadhaarFront(uid: String): Result<Unit> {
        // Note: StorageRepository doesn't have delete methods, just clear the URL
        return firestoreRepository.updateProviderData(uid, mapOf("aadhaarFrontUrl" to ""))
    }

    /**
     * Delete Aadhaar back image.
     */
    suspend fun deleteAadhaarBack(uid: String): Result<Unit> {
        // Note: StorageRepository doesn't have delete methods, just clear the URL
        return firestoreRepository.updateProviderData(uid, mapOf("aadhaarBackUrl" to ""))
    }

    /**
     * Delete profile photo.
     */
    suspend fun deleteProfilePhoto(uid: String): Result<Unit> {
        // Note: StorageRepository doesn't have delete methods, just clear the URL
        return firestoreRepository.updateProviderData(uid, mapOf("profilePhotoUrl" to ""))
    }

    /**
     * Reset onboarding progress (inner-app only).
     * Clears all onboarding data but keeps user authentication.
     */
    suspend fun resetOnboarding(uid: String): Result<Unit> {
        val resetData = mapOf(
            "onboardingStatus" to OnboardingStatus.NOT_STARTED.statusString,
            "currentStep" to OnboardingStep.BASIC_INFO.stepNumber,
            "fullName" to "",
            "gender" to "",
            "email" to "",
            "primaryService" to "",
            "selectedMainService" to "",
            "selectedSubServices" to emptyList<String>(),
            "otherService" to "",
            "state" to "",
            "city" to "",
            "address" to "",
            "fullAddress" to "",
            "pincode" to "",
            "serviceRadius" to 5.0,
            "latitude" to null,
            "longitude" to null,
            "aadhaarFrontUrl" to "",
            "aadhaarBackUrl" to "",
            "profilePhotoUrl" to "",
            "submittedAt" to null
        ).filterValues { it != null } as Map<String, Any>
        return firestoreRepository.updateProviderData(uid, resetData)
    }
}
