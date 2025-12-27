package com.nextserve.serveitpartnernew.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.nextserve.serveitpartnernew.data.model.MainServiceModel
import com.nextserve.serveitpartnernew.data.model.ProviderData
import com.nextserve.serveitpartnernew.data.model.SubServiceModel
import kotlinx.coroutines.tasks.await

class FirestoreRepository(
    private val firestore: FirebaseFirestore
) {
    private val providersCollection = firestore.collection("providers")

    suspend fun getProviderData(uid: String): Result<ProviderData?> {
        return try {
            val document = providersCollection.document(uid).get().await()
            if (document.exists()) {
                val data = document.toObject(ProviderData::class.java)
                Result.success(data?.copy(uid = uid))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createProviderDocument(uid: String, phoneNumber: String): Result<Unit> {
        return try {
            // #region agent log
            try {
                java.io.File("c:\\Users\\Chaitany Kakde\\StudioProjects\\Serveit-Partner-New\\.cursor\\debug.log").appendText("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"location\":\"FirestoreRepository.kt:30\",\"message\":\"createProviderDocument called\",\"data\":{\"uid\":\"$uid\",\"phoneNumber\":\"$phoneNumber\",\"collection\":\"providers\"},\"timestamp\":${System.currentTimeMillis()}}\n")
            } catch (e: Exception) {}
            // #endregion
            
            // Ensure phone number has +91 prefix
            val formattedPhone = if (phoneNumber.startsWith("+91")) {
                phoneNumber
            } else if (phoneNumber.startsWith("91") && phoneNumber.length > 10) {
                "+$phoneNumber"
            } else {
                "+91$phoneNumber"
            }
            
            val data = ProviderData(
                uid = uid,
                phoneNumber = formattedPhone,
                createdAt = Timestamp.now(),
                lastLoginAt = Timestamp.now(),
                onboardingStatus = "IN_PROGRESS",
                currentStep = 1
            )
            
            // #region agent log
            try {
                java.io.File("c:\\Users\\Chaitany Kakde\\StudioProjects\\Serveit-Partner-New\\.cursor\\debug.log").appendText("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"location\":\"FirestoreRepository.kt:49\",\"message\":\"Writing to Firestore\",\"data\":{\"uid\":\"$uid\",\"collection\":\"providers\",\"dataStructure\":\"flat\",\"hasLocationDetails\":false,\"hasVerificationDetails\":false,\"hasPersonalDetails\":false},\"timestamp\":${System.currentTimeMillis()}}\n")
            } catch (e: Exception) {}
            // #endregion
            
            providersCollection.document(uid).set(data).await()
            
            // #region agent log
            try {
                java.io.File("c:\\Users\\Chaitany Kakde\\StudioProjects\\Serveit-Partner-New\\.cursor\\debug.log").appendText("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"location\":\"FirestoreRepository.kt:50\",\"message\":\"createProviderDocument success\",\"data\":{\"uid\":\"$uid\"},\"timestamp\":${System.currentTimeMillis()}}\n")
            } catch (e: Exception) {}
            // #endregion
            
            Result.success(Unit)
        } catch (e: Exception) {
            // #region agent log
            try {
                java.io.File("c:\\Users\\Chaitany Kakde\\StudioProjects\\Serveit-Partner-New\\.cursor\\debug.log").appendText("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"location\":\"FirestoreRepository.kt:52\",\"message\":\"createProviderDocument error\",\"data\":{\"error\":\"${e.message}\"},\"timestamp\":${System.currentTimeMillis()}}\n")
            } catch (e2: Exception) {}
            // #endregion
            Result.failure(e)
        }
    }

    suspend fun updateLastLogin(uid: String): Result<Unit> {
        return try {
            providersCollection.document(uid)
                .update("lastLoginAt", Timestamp.now())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProviderData(uid: String, data: Map<String, Any>): Result<Unit> {
        return try {
            providersCollection.document(uid)
                .update(data)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveOnboardingStep(uid: String, data: Map<String, Any>): Result<Unit> {
        return try {
            // #region agent log
            try {
                val dataKeys = data.keys.joinToString(",")
                val hasLocation = data.containsKey("latitude") || data.containsKey("longitude")
                val hasServices = data.containsKey("selectedSubServices") || data.containsKey("primaryService")
                java.io.File("c:\\Users\\Chaitany Kakde\\StudioProjects\\Serveit-Partner-New\\.cursor\\debug.log").appendText("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\",\"location\":\"FirestoreRepository.kt:78\",\"message\":\"saveOnboardingStep called\",\"data\":{\"uid\":\"$uid\",\"collection\":\"providers\",\"dataKeys\":\"$dataKeys\",\"hasLocation\":$hasLocation,\"hasServices\":$hasServices,\"structure\":\"flat\"},\"timestamp\":${System.currentTimeMillis()}}\n")
            } catch (e: Exception) {}
            // #endregion
            
            val updateData = data.toMutableMap()
            updateData["updatedAt"] = Timestamp.now()
            
            providersCollection.document(uid)
                .set(updateData, SetOptions.merge())
                .await()
            
            // #region agent log
            try {
                java.io.File("c:\\Users\\Chaitany Kakde\\StudioProjects\\Serveit-Partner-New\\.cursor\\debug.log").appendText("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\",\"location\":\"FirestoreRepository.kt:85\",\"message\":\"saveOnboardingStep success\",\"data\":{\"uid\":\"$uid\"},\"timestamp\":${System.currentTimeMillis()}}\n")
            } catch (e: Exception) {}
            // #endregion
            
            Result.success(Unit)
        } catch (e: Exception) {
            // #region agent log
            try {
                java.io.File("c:\\Users\\Chaitany Kakde\\StudioProjects\\Serveit-Partner-New\\.cursor\\debug.log").appendText("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\",\"location\":\"FirestoreRepository.kt:88\",\"message\":\"saveOnboardingStep error\",\"data\":{\"error\":\"${e.message}\"},\"timestamp\":${System.currentTimeMillis()}}\n")
            } catch (e2: Exception) {}
            // #endregion
            Result.failure(e)
        }
    }

    suspend fun updateCurrentStep(uid: String, step: Int): Result<Unit> {
        return try {
            providersCollection.document(uid)
                .update(
                    mapOf(
                        "currentStep" to step,
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveDocumentUrls(
        uid: String,
        aadhaarFrontUrl: String,
        aadhaarBackUrl: String
    ): Result<Unit> {
        return try {
            providersCollection.document(uid)
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
            Result.failure(e)
        }
    }

    suspend fun submitForVerification(uid: String): Result<Unit> {
        return try {
            // #region agent log
            try {
                java.io.File("c:\\Users\\Chaitany Kakde\\StudioProjects\\Serveit-Partner-New\\.cursor\\debug.log").appendText("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"C\",\"location\":\"FirestoreRepository.kt:130\",\"message\":\"submitForVerification called\",\"data\":{\"uid\":\"$uid\",\"collection\":\"providers\",\"updates\":[\"onboardingStatus=SUBMITTED\",\"approvalStatus=PENDING\"],\"structure\":\"flat\"},\"timestamp\":${System.currentTimeMillis()}}\n")
            } catch (e: Exception) {}
            // #endregion
            
            providersCollection.document(uid)
                .update(
                    mapOf(
                        "onboardingStatus" to "SUBMITTED",
                        "approvalStatus" to "PENDING",
                        "submittedAt" to Timestamp.now(),
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()
            
            // #region agent log
            try {
                java.io.File("c:\\Users\\Chaitany Kakde\\StudioProjects\\Serveit-Partner-New\\.cursor\\debug.log").appendText("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"C\",\"location\":\"FirestoreRepository.kt:141\",\"message\":\"submitForVerification success\",\"data\":{\"uid\":\"$uid\"},\"timestamp\":${System.currentTimeMillis()}}\n")
            } catch (e: Exception) {}
            // #endregion
            
            // Note: FCM notification will be sent by backend/admin when status changes
            // The notification will be received via ServeitFirebaseMessagingService
            Result.success(Unit)
        } catch (e: Exception) {
            // #region agent log
            try {
                java.io.File("c:\\Users\\Chaitany Kakde\\StudioProjects\\Serveit-Partner-New\\.cursor\\debug.log").appendText("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"C\",\"location\":\"FirestoreRepository.kt:147\",\"message\":\"submitForVerification error\",\"data\":{\"error\":\"${e.message}\"},\"timestamp\":${System.currentTimeMillis()}}\n")
            } catch (e2: Exception) {}
            // #endregion
            Result.failure(e)
        }
    }

    suspend fun checkOnboardingStatus(uid: String): Result<ProviderData?> {
        return getProviderData(uid)
    }

    suspend fun getMainServices(gender: String): Result<List<MainServiceModel>> {
        return try {
            val serviceType = when (gender.lowercase()) {
                "male", "men" -> "menservices"
                "female", "women" -> "womenservices"
                else -> return Result.failure(Exception("Invalid gender: $gender"))
            }

            val serviceListRef = firestore.collection("services")
                .document(serviceType)
                .collection("serviceList")

            val snapshot = serviceListRef.get().await()
            val services = snapshot.documents.mapNotNull { document ->
                val service = document.toObject(MainServiceModel::class.java)
                service?.let {
                    if (it.isActive) it else null
                }
            }

            Result.success(services)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSubServices(gender: String, mainServiceName: String): Result<List<SubServiceModel>> {
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
                return Result.failure(Exception("Service document not found: $mainServiceName"))
            }

            val data = document.data ?: return Result.failure(Exception("Service document is empty"))

            // Handle both array and map formats for subServices
            val subServicesList = when (val subServices = data["subServices"]) {
                is List<*> -> {
                    // Array format
                    subServices.mapNotNull { item ->
                        when (item) {
                            is Map<*, *> -> {
                                val map = item as Map<String, Any>
                                SubServiceModel(
                                    name = map["name"] as? String ?: "",
                                    description = map["description"] as? String ?: "",
                                    unit = map["unit"] as? String ?: ""
                                )
                            }
                            else -> null
                        }
                    }
                }
                is Map<*, *> -> {
                    // Map format - convert map values to list
                    (subServices as Map<String, Map<String, Any>>).values.map { serviceMap ->
                        SubServiceModel(
                            name = serviceMap["name"] as? String ?: "",
                            description = serviceMap["description"] as? String ?: "",
                            unit = serviceMap["unit"] as? String ?: ""
                        )
                    }
                }
                else -> emptyList()
            }

            Result.success(subServicesList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

