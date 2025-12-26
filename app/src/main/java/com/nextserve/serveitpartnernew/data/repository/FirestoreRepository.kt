package com.nextserve.serveitpartnernew.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.nextserve.serveitpartnernew.data.mapper.ProviderFirestoreMapper
import com.nextserve.serveitpartnernew.data.model.MainServiceModel
import com.nextserve.serveitpartnernew.data.model.ProviderData
import com.nextserve.serveitpartnernew.data.model.SubServiceModel
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
            Result.failure(e)
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
            
            // Write to partners collection (Cloud Functions expect this)
            partnersCollection.document(uid).set(firestoreData).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateLastLogin(uid: String): Result<Unit> {
        return try {
            partnersCollection.document(uid)
                .update("lastLoginAt", Timestamp.now())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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
            Result.failure(e)
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
            Result.failure(e)
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
            Result.failure(e)
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
            Result.failure(e)
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

