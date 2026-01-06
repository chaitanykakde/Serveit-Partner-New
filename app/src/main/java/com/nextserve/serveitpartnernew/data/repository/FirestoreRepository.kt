package com.nextserve.serveitpartnernew.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.nextserve.serveitpartnernew.data.mapper.ProviderFirestoreMapper
import com.nextserve.serveitpartnernew.data.model.MainService
import com.nextserve.serveitpartnernew.data.model.ProviderData
import kotlinx.coroutines.tasks.await

class FirestoreRepository(
    private val firestore: FirebaseFirestore
) {
    // CRITICAL: Changed from "providers" to "partners" to match Cloud Functions
    private val partnersCollection = firestore.collection("partners")

    suspend fun getProviderData(uid: String): Result<ProviderData?> {
        return try {
            val document = partnersCollection.document(uid).get().await()
            if (document.exists()) {
                // Use mapper to convert nested Firestore structure to flat app model
                val providerData = ProviderFirestoreMapper.fromFirestore(document, uid)
                Result.success(providerData)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "Failed to get provider data for uid: $uid", e)
            Result.failure(Exception("Failed to load profile data. Please check your connection and try again."))
        }
    }

    suspend fun createProviderDocument(uid: String, phoneNumber: String): Result<Unit> {
        return try {
            // Ensure phone number has +91 prefix
            val formattedPhone = if (phoneNumber.startsWith("+91")) {
                phoneNumber
            } else if (phoneNumber.startsWith("91") && phoneNumber.length > 10) {
                "+$phoneNumber"
            } else {
                "+91$phoneNumber"
            }

            android.util.Log.d("FirestoreRepository", "📱 Creating provider document for uid: $uid with phone: $formattedPhone")

            val providerData = ProviderData(
                uid = uid,
                phoneNumber = formattedPhone,
                createdAt = Timestamp.now(),
                lastLoginAt = Timestamp.now(),
                onboardingStatus = "IN_PROGRESS",
                currentStep = 1
            )

            // Transform flat model to nested Firestore structure using mapper
            val firestoreData = ProviderFirestoreMapper.toFirestore(providerData)

            android.util.Log.d("FirestoreRepository", "🔄 Firestore data structure: personalDetails.mobileNo = ${firestoreData["personalDetails"]?.let { (it as Map<*, *>)["mobileNo"] }}")

            // Write to partners collection (Cloud Functions expect this)
            partnersCollection.document(uid).set(firestoreData).await()

            android.util.Log.d("FirestoreRepository", "✅ Provider document created successfully for uid: $uid")

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "Failed to get provider data for uid: $uid", e)
            Result.failure(Exception("Failed to load profile data. Please check your connection and try again."))
        }
    }

    suspend fun updateLastLogin(uid: String): Result<Unit> {
        return try {
            partnersCollection.document(uid)
                .update("lastLoginAt", Timestamp.now())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "Failed to get provider data for uid: $uid", e)
            Result.failure(Exception("Failed to load profile data. Please check your connection and try again."))
        }
    }

    /**
     * Update provider's online status
     * Required by Cloud Functions for job dispatch
     */
    suspend fun updateOnlineStatus(uid: String, isOnline: Boolean): Result<Unit> {
        return try {
            partnersCollection.document(uid)
                .update("isOnline", isOnline)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "Failed to get provider data for uid: $uid", e)
            Result.failure(Exception("Failed to load profile data. Please check your connection and try again."))
        }
    }

    suspend fun updateProviderData(uid: String, data: Map<String, Any>): Result<Unit> {
        return try {
            // Transform flat update map to nested Firestore structure
            val firestoreUpdate = ProviderFirestoreMapper.toFirestoreUpdate(data)
            partnersCollection.document(uid)
                .update(firestoreUpdate)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "Failed to get provider data for uid: $uid", e)
            Result.failure(Exception("Failed to load profile data. Please check your connection and try again."))
        }
    }

    suspend fun saveOnboardingStep(uid: String, data: Map<String, Any>): Result<Unit> {
        return try {
            val updateData = data.toMutableMap()
            updateData["updatedAt"] = Timestamp.now()
            
            // Transform flat update map to nested Firestore structure
            val firestoreUpdate = ProviderFirestoreMapper.toFirestoreUpdate(updateData)
            
            // Write to partners collection with merge to preserve existing nested fields
            partnersCollection.document(uid)
                .set(firestoreUpdate, SetOptions.merge())
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "Failed to get provider data for uid: $uid", e)
            Result.failure(Exception("Failed to load profile data. Please check your connection and try again."))
        }
    }

    suspend fun updateCurrentStep(uid: String, step: Int): Result<Unit> {
        return try {
            partnersCollection.document(uid)
                .update(
                    mapOf(
                        "currentStep" to step,
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "Failed to get provider data for uid: $uid", e)
            Result.failure(Exception("Failed to load profile data. Please check your connection and try again."))
        }
    }

    suspend fun saveDocumentUrls(
        uid: String,
        aadhaarFrontUrl: String,
        aadhaarBackUrl: String
    ): Result<Unit> {
        return try {
            partnersCollection.document(uid)
                .update(
                    mapOf(
                        "aadhaarFrontUrl" to aadhaarFrontUrl,
                        "aadhaarBackUrl" to aadhaarBackUrl,
                        "documentsUploadedAt" to Timestamp.now(),
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "Failed to get provider data for uid: $uid", e)
            Result.failure(Exception("Failed to load profile data. Please check your connection and try again."))
        }
    }

    suspend fun submitForVerification(uid: String): Result<Unit> {
        return try {
            // Transform approvalStatus string to nested verificationDetails booleans
            val updateMap = mapOf(
                "onboardingStatus" to "SUBMITTED",
                "approvalStatus" to "PENDING", // Will be converted to booleans by mapper
                "submittedAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            )
            
            // Use mapper to convert to nested structure
            val firestoreUpdate = ProviderFirestoreMapper.toFirestoreUpdate(updateMap)
            
            partnersCollection.document(uid)
                .update(firestoreUpdate)
                .await()
            
            // Note: FCM notification will be sent by backend/admin when status changes
            // The notification will be received via ServeitFirebaseMessagingService
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "Failed to get provider data for uid: $uid", e)
            Result.failure(Exception("Failed to load profile data. Please check your connection and try again."))
        }
    }

    suspend fun checkOnboardingStatus(uid: String): Result<ProviderData?> {
        return getProviderData(uid)
    }

    suspend fun getMainServices(gender: String): Result<List<MainService>> {
        return try {
            val serviceType = when (gender.lowercase()) {
                "male", "men" -> "menservices"
                "female", "women" -> "womenservices"
                else -> return Result.success(emptyList()) // Invalid gender = empty list, not error
            }

            val serviceListRef = firestore.collection("services")
                .document(serviceType)
                .collection("serviceList")

            val snapshot = serviceListRef.get().await()
            val services = snapshot.documents.mapNotNull { document ->
                try {
                    // Read raw document data instead of auto-deserialization
                    val data = document.data ?: return@mapNotNull null

                    // Extract name (fallback to document ID if missing)
                    val name = data["name"] as? String ?: document.id

                    // Extract isActive (default = true)
                    val isActive = data["isActive"] as? Boolean ?: true

                    // Skip inactive services
                    if (!isActive) return@mapNotNull null

                    // Extract sub-service names from subServices Map
                    val subServiceNames = when (val subServices = data["subServices"]) {
                        is Map<*, *> -> {
                            // subServices is a Map<String, Map<String, Any>>
                            // Keys are sub-service names
                            (subServices as Map<String, *>).keys.toList()
                        }
                        else -> {
                            // No sub-services or malformed data
                            android.util.Log.w("FirestoreRepository", "No subServices Map found for service: ${document.id}")
                            emptyList<String>()
                        }
                    }

                    MainService(
                        id = document.id,
                        name = name,
                        subServiceNames = subServiceNames
                    )
                } catch (e: Exception) {
                    // Skip malformed documents silently
                    android.util.Log.w("FirestoreRepository", "Skipping malformed service document: ${document.id}", e)
                    null
                }
            }

            android.util.Log.d("FirestoreRepository", "Loaded ${services.size} services for $gender from /services/$serviceType/serviceList")
            Result.success(services)

        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "Service load failed for gender: $gender", e)
            // Return empty list instead of failure for missing collections/network issues
            android.util.Log.w("FirestoreRepository", "Returning empty services list due to error: ${e.message}")
            Result.success(emptyList())
        }
    }

    suspend fun getSubServices(gender: String, mainServiceName: String): Result<List<String>> {
        return try {
            val serviceType = when (gender.lowercase()) {
                "male", "men" -> "menservices"
                "female", "women" -> "womenservices"
                else -> return Result.failure(Exception("Invalid gender: $gender"))
            }

            val serviceDocRef = firestore.collection("services")
                .document(serviceType)
                .collection("serviceList")
                .document(mainServiceName)

            val document = serviceDocRef.get().await()
            if (!document.exists()) {
                android.util.Log.w("FirestoreRepository", "Service document not found: $mainServiceName")
                return Result.success(emptyList()) // Return empty list instead of error
            }

            val data = document.data ?: return Result.success(emptyList())

            // Extract sub-service names from subServices Map
            val subServiceNames = when (val subServices = data["subServices"]) {
                is Map<*, *> -> {
                    // subServices is a Map<String, Map<String, Any>>
                    // Keys are sub-service names
                    (subServices as Map<String, *>).keys.toList()
                }
                else -> {
                    // No sub-services or malformed data
                    android.util.Log.w("FirestoreRepository", "No subServices Map found for service: $mainServiceName")
                    emptyList<String>()
                }
            }

            android.util.Log.d("FirestoreRepository", "Loaded ${subServiceNames.size} sub-services for $mainServiceName")
            Result.success(subServiceNames)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "Failed to get sub services for mainService: $mainServiceName", e)
            Result.success(emptyList()) // Return empty list instead of failure
        }
    }
}

