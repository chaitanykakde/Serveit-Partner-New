package com.nextserve.serveitpartnernew.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.nextserve.serveitpartnernew.data.model.*
import kotlinx.coroutines.tasks.await
import com.google.firebase.storage.FirebaseStorage
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose

class JobManagementRepository(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {

    // Timer Management
    suspend fun startJobTimer(jobId: String, providerId: String): Result<Unit> {
        return try {
            val timerRef = firestore.collection("jobTimers")
                .document("${providerId}_${jobId}")

            val timer = JobTimer(
                jobId = jobId,
                providerId = providerId,
                startTime = Timestamp.now(),
                isRunning = true
            )

            timerRef.set(timer).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pauseJobTimer(jobId: String, providerId: String): Result<Unit> {
        return try {
            val timerRef = firestore.collection("jobTimers")
                .document("${providerId}_${jobId}")

            // Add pause entry
            val pauseRef = timerRef.collection("pauses").document()
            val pause = TimerPause(
                startTime = Timestamp.now()
            )
            pauseRef.set(pause).await()

            // Update timer status
            timerRef.update("isRunning", false).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resumeJobTimer(jobId: String, providerId: String): Result<Unit> {
        return try {
            val timerRef = firestore.collection("jobTimers")
                .document("${providerId}_${jobId}")

            // End current pause
            val pausesRef = timerRef.collection("pauses")
            val activePause = pausesRef.whereEqualTo("endTime", null).get().await()

            activePause.documents.firstOrNull()?.let { pauseDoc ->
                pauseDoc.reference.update(
                    mapOf(
                        "endTime" to Timestamp.now(),
                        "duration" to (Timestamp.now().toDate().time - pauseDoc.getTimestamp("startTime")!!.toDate().time)
                    )
                ).await()
            }

            // Resume timer
            timerRef.update("isRunning", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun stopJobTimer(jobId: String, providerId: String): Result<JobTimer?> {
        return try {
            val timerRef = firestore.collection("jobTimers")
                .document("${providerId}_${jobId}")

            val timerDoc = timerRef.get().await()
            if (!timerDoc.exists()) return Result.success(null)

            val startTime = timerDoc.getTimestamp("startTime") ?: return Result.success(null)

            // Calculate total duration
            val pausesRef = timerRef.collection("pauses")
            val pauses = pausesRef.get().await().documents.map { doc ->
                val start = doc.getTimestamp("startTime") ?: Timestamp.now()
                val end = doc.getTimestamp("endTime") ?: Timestamp.now()
                end.toDate().time - start.toDate().time
            }.sum()

            val totalDuration = Timestamp.now().toDate().time - startTime.toDate().time - pauses

            // Update timer
            timerRef.update(
                mapOf(
                    "endTime" to Timestamp.now(),
                    "totalDuration" to totalDuration,
                    "isRunning" to false
                )
            ).await()

            // Return updated timer
            val updatedTimer = timerRef.get().await()
            val timer = updatedTimer.toObject(JobTimer::class.java)
            Result.success(timer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToJobTimer(jobId: String, providerId: String): Flow<JobTimer?> = callbackFlow {
        val timerRef = firestore.collection("jobTimers")
            .document("${providerId}_${jobId}")

        val listener = timerRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val timer = snapshot.toObject(JobTimer::class.java)
                trySend(timer)
            } else {
                trySend(null)
            }
        }

        awaitClose { listener.remove() }
    }

    // Job Notes Management
    suspend fun addJobNote(jobId: String, providerId: String, note: String, type: String = "note"): Result<Unit> {
        return try {
            val notesRef = firestore.collection("jobNotes")
                .document()

            val jobNote = JobNote(
                id = notesRef.id,
                jobId = jobId,
                providerId = providerId,
                note = note,
                timestamp = Timestamp.now(),
                type = type
            )

            notesRef.set(jobNote).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToJobNotes(jobId: String, providerId: String): Flow<List<JobNote>> = callbackFlow {
        val notesRef = firestore.collection("jobNotes")
            .whereEqualTo("jobId", jobId)
            .whereEqualTo("providerId", providerId)
            .orderBy("timestamp")

        val listener = notesRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val notes = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(JobNote::class.java)
                }
                trySend(notes)
            } else {
                trySend(emptyList())
            }
        }

        awaitClose { listener.remove() }
    }

    // Photo Upload
    suspend fun uploadJobPhoto(
        jobId: String,
        providerId: String,
        photoUri: Uri,
        photoType: String,
        caption: String? = null
    ): Result<String> {
        return try {
            val photoRef = storage.reference
                .child("jobs/$jobId/photos/${System.currentTimeMillis()}.jpg")

            val uploadTask = photoRef.putFile(photoUri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()

            // Save photo metadata to Firestore
            val photoDoc = firestore.collection("jobPhotos").document()
            val jobPhoto = JobPhoto(
                id = photoDoc.id,
                jobId = jobId,
                providerId = providerId,
                photoUrl = downloadUrl.toString(),
                photoType = photoType,
                caption = caption,
                timestamp = Timestamp.now()
            )

            photoDoc.set(jobPhoto).await()

            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Completion Checklist
    suspend fun createJobCompletionChecklist(
        jobId: String,
        providerId: String,
        serviceType: String
    ): Result<Unit> {
        return try {
            val checklistRef = firestore.collection("jobCompletionChecklists")
                .document("${providerId}_${jobId}")

            // Create checklist based on service type
            val checklistItems = when (serviceType.lowercase()) {
                "ac repair" -> listOf(
                    JobCompletionItem("ac_check", "Check AC unit condition", false, true),
                    JobCompletionItem("filters_clean", "Clean/replace air filters", false, true),
                    JobCompletionItem("refrigerant_check", "Check refrigerant levels", false, true),
                    JobCompletionItem("test_operation", "Test AC operation", false, true)
                )
                "plumbing" -> listOf(
                    JobCompletionItem("leak_check", "Check for leaks", false, true),
                    JobCompletionItem("pressure_test", "Test water pressure", false, true),
                    JobCompletionItem("fixtures_test", "Test all fixtures", false, true),
                    JobCompletionItem("cleanup", "Clean work area", false, false)
                )
                else -> listOf(
                    JobCompletionItem("service_complete", "Service completed", false, true),
                    JobCompletionItem("customer_satisfied", "Customer satisfied", false, true),
                    JobCompletionItem("cleanup", "Clean work area", false, false)
                )
            }

            val checklist = JobCompletionChecklist(
                jobId = jobId,
                providerId = providerId,
                items = checklistItems
            )

            checklistRef.set(checklist).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateChecklistItem(
        jobId: String,
        providerId: String,
        itemId: String,
        isCompleted: Boolean
    ): Result<Unit> {
        return try {
            val checklistRef = firestore.collection("jobCompletionChecklists")
                .document("${providerId}_${jobId}")

            val checklistDoc = checklistRef.get().await()
            if (!checklistDoc.exists()) return Result.failure(Exception("Checklist not found"))

            val checklist = checklistDoc.toObject(JobCompletionChecklist::class.java)
                ?: return Result.failure(Exception("Invalid checklist"))

            val updatedItems = checklist.items.map { item ->
                if (item.id == itemId) item.copy(isCompleted = isCompleted) else item
            }

            val allRequiredCompleted = updatedItems.filter { it.isRequired }.all { it.isCompleted }

            checklistRef.update(
                mapOf(
                    "items" to updatedItems,
                    "allRequiredCompleted" to allRequiredCompleted
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToCompletionChecklist(jobId: String, providerId: String): Flow<JobCompletionChecklist?> = callbackFlow {
        val checklistRef = firestore.collection("jobCompletionChecklists")
            .document("${providerId}_${jobId}")

        val listener = checklistRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val checklist = snapshot.toObject(JobCompletionChecklist::class.java)
                trySend(checklist)
            } else {
                trySend(null)
            }
        }

        awaitClose { listener.remove() }
    }

    // Customer Feedback
    suspend fun requestCustomerFeedback(jobId: String, providerId: String): Result<Unit> {
        return try {
            // This would typically trigger a notification to customer
            // For now, just log the request
            val feedbackRequestRef = firestore.collection("feedbackRequests").document()

            feedbackRequestRef.set(mapOf(
                "jobId" to jobId,
                "providerId" to providerId,
                "requestedAt" to Timestamp.now(),
                "status" to "pending"
            )).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
