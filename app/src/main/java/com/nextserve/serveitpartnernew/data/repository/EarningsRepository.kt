package com.nextserve.serveitpartnernew.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import com.nextserve.serveitpartnernew.data.model.Earning
import com.nextserve.serveitpartnernew.data.model.EarningsSummary
import java.util.Calendar

class EarningsRepository(
    private val firestore: FirebaseFirestore
) {
    private val earningsCollection = firestore.collection("earnings")

    /**
     * Get today's earnings for a provider
     */
    suspend fun getTodayEarnings(providerId: String): Result<EarningsSummary> {
        return try {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayStart = Timestamp(today.time)

            val snapshot = earningsCollection
                .whereEqualTo("partnerId", providerId)
                .whereGreaterThanOrEqualTo("date", todayStart)
                .get()
                .await()

            val earnings = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                Earning(
                    id = doc.id,
                    partnerId = data["partnerId"] as? String ?: "",
                    amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                    date = data["date"] as? Timestamp,
                    bookingId = data["bookingId"] as? String,
                    serviceName = data["serviceName"] as? String,
                    jobCompletedAt = data["jobCompletedAt"] as? Timestamp,
                    paymentStatus = data["paymentStatus"] as? String ?: "pending"
                )
            }

            val total = earnings.sumOf { it.amount }
            val byService = earnings.groupBy { it.serviceName ?: "Unknown" }
                .mapValues { (_, list) -> list.sumOf { it.amount } }

            Result.success(
                EarningsSummary(
                    totalEarnings = total,
                    jobsCount = earnings.size,
                    period = "today",
                    earningsByService = byService
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get weekly earnings for a provider
     */
    suspend fun getWeeklyEarnings(providerId: String): Result<EarningsSummary> {
        return try {
            val weekStart = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val weekStartTimestamp = Timestamp(weekStart.time)

            val snapshot = earningsCollection
                .whereEqualTo("partnerId", providerId)
                .whereGreaterThanOrEqualTo("date", weekStartTimestamp)
                .get()
                .await()

            val earnings = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                Earning(
                    id = doc.id,
                    partnerId = data["partnerId"] as? String ?: "",
                    amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                    date = data["date"] as? Timestamp,
                    bookingId = data["bookingId"] as? String,
                    serviceName = data["serviceName"] as? String,
                    jobCompletedAt = data["jobCompletedAt"] as? Timestamp,
                    paymentStatus = data["paymentStatus"] as? String ?: "pending"
                )
            }

            val total = earnings.sumOf { it.amount }
            val byService = earnings.groupBy { it.serviceName ?: "Unknown" }
                .mapValues { (_, list) -> list.sumOf { it.amount } }

            Result.success(
                EarningsSummary(
                    totalEarnings = total,
                    jobsCount = earnings.size,
                    period = "week",
                    earningsByService = byService
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get monthly earnings for a provider
     */
    suspend fun getMonthlyEarnings(providerId: String): Result<EarningsSummary> {
        return try {
            val monthStart = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val monthStartTimestamp = Timestamp(monthStart.time)

            val snapshot = earningsCollection
                .whereEqualTo("partnerId", providerId)
                .whereGreaterThanOrEqualTo("date", monthStartTimestamp)
                .get()
                .await()

            val earnings = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                Earning(
                    id = doc.id,
                    partnerId = data["partnerId"] as? String ?: "",
                    amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                    date = data["date"] as? Timestamp,
                    bookingId = data["bookingId"] as? String,
                    serviceName = data["serviceName"] as? String,
                    jobCompletedAt = data["jobCompletedAt"] as? Timestamp,
                    paymentStatus = data["paymentStatus"] as? String ?: "pending"
                )
            }

            val total = earnings.sumOf { it.amount }
            val byService = earnings.groupBy { it.serviceName ?: "Unknown" }
                .mapValues { (_, list) -> list.sumOf { it.amount } }

            Result.success(
                EarningsSummary(
                    totalEarnings = total,
                    jobsCount = earnings.size,
                    period = "month",
                    earningsByService = byService
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get earnings history with pagination
     */
    suspend fun getEarningsHistory(
        providerId: String,
        limit: Int = 20,
        lastDocument: com.google.firebase.firestore.DocumentSnapshot? = null
    ): Result<Pair<List<Earning>, com.google.firebase.firestore.DocumentSnapshot?>> {
        return try {
            var query: Query = earningsCollection
                .whereEqualTo("partnerId", providerId)
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(limit.toLong())

            if (lastDocument != null) {
                query = query.startAfter(lastDocument)
            }

            val snapshot = query.get().await()

            val earnings = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                Earning(
                    id = doc.id,
                    partnerId = data["partnerId"] as? String ?: "",
                    amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                    date = data["date"] as? Timestamp,
                    bookingId = data["bookingId"] as? String,
                    serviceName = data["serviceName"] as? String,
                    jobCompletedAt = data["jobCompletedAt"] as? Timestamp,
                    paymentStatus = data["paymentStatus"] as? String ?: "pending"
                )
            }

            val lastDoc = if (snapshot.documents.isNotEmpty()) {
                snapshot.documents.last()
            } else {
                null
            }

            Result.success(Pair(earnings, lastDoc))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calculate earnings from completed jobs (fallback if earnings collection is empty)
     */
    suspend fun calculateEarningsFromJobs(providerId: String): Result<EarningsSummary> {
        return try {
            // Query completed jobs from Bookings collection
            val bookingsSnapshot = firestore.collection("Bookings")
                .get()
                .await()

            // Note: This is a fallback method - ideally earnings should be stored in earnings collection
            // when jobs are completed
            bookingsSnapshot.documents.forEach { doc ->
                val data = doc.data ?: return@forEach
                val bookingsArray = data["bookings"] as? List<Map<String, Any>>
                
                if (bookingsArray != null) {
                    bookingsArray.forEach { booking ->
                        val jobProviderId = booking["providerId"] as? String
                        val status = booking["status"] as? String
                        if (jobProviderId == providerId && status == "completed") {
                            // Map to Job model (simplified)
                            val totalPrice = (booking["totalPrice"] as? Number)?.toDouble() ?: 0.0
                            val serviceName = booking["serviceName"] as? String ?: "Unknown"
                            val completedAt = booking["completedAt"] as? Timestamp
                            
                            // Create earning-like summary
                            // Note: This is a fallback - ideally earnings should be in earnings collection
                        }
                    }
                }
            }

            // For now, return empty summary
            // In production, this should aggregate from completed jobs
            Result.success(
                EarningsSummary(
                    totalEarnings = 0.0,
                    jobsCount = 0,
                    period = "all",
                    earningsByService = emptyMap()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

