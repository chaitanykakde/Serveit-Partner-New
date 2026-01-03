package com.nextserve.serveitpartnernew.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.nextserve.serveitpartnernew.data.model.Notification

class NotificationsRepository(
    private val firestore: FirebaseFirestore
) {
    /**
     * Listen to notifications for a provider
     */
    fun listenToNotifications(providerId: String): Flow<List<Notification>> = callbackFlow {
        val listener = firestore
            .collection("partners")
            .document(providerId)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val notifications = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    Notification(
                        id = doc.id,
                        title = data["title"] as? String ?: "",
                        message = data["message"] as? String ?: "",
                        type = data["type"] as? String ?: "",
                        timestamp = data["timestamp"] as? Timestamp,
                        isRead = data["isRead"] as? Boolean ?: false,
                        userId = data["userId"] as? String ?: providerId,
                        relatedData = data["relatedData"] as? Map<String, Any>
                    )
                }

                trySend(notifications)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Mark notification as read
     */
    suspend fun markAsRead(providerId: String, notificationId: String): Result<Unit> {
        return try {
            firestore
                .collection("partners")
                .document(providerId)
                .collection("notifications")
                .document(notificationId)
                .update("isRead", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mark all notifications as read
     */
    suspend fun markAllAsRead(providerId: String): Result<Unit> {
        return try {
            val snapshot = firestore
                .collection("partners")
                .document(providerId)
                .collection("notifications")
                .whereEqualTo("isRead", false)
                .get()
                .await()

            val batch = firestore.batch()
            snapshot.documents.forEach { doc ->
                batch.update(doc.reference, "isRead", true)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get unread notification count
     */
    suspend fun getUnreadCount(providerId: String): Result<Int> {
        return try {
            val snapshot = firestore
                .collection("partners")
                .document(providerId)
                .collection("notifications")
                .whereEqualTo("isRead", false)
                .get()
                .await()
            Result.success(snapshot.size())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

