package com.nextserve.serveitpartnernew.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.nextserve.serveitpartnernew.data.model.Job
import com.nextserve.serveitpartnernew.data.model.JobCoordinates
import com.nextserve.serveitpartnernew.data.model.JobInboxEntry
import android.content.Context
import android.content.SharedPreferences

/**
 * Repository for managing jobs/bookings from Firestore
 * Handles data normalization and Firestore queries
 */
class JobsRepository(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val context: Context? = null
) {
    private val prefs: SharedPreferences? = context?.getSharedPreferences("jobs_cache", Context.MODE_PRIVATE)
    private val CACHE_KEY_LAST_JOBS = "last_jobs_cache"
    private val CACHE_KEY_LAST_UPDATE = "last_update_time"
    private val bookingsCollection = firestore.collection("Bookings")

    /**
     * Listen to new jobs available for the provider
     * Uses snapshot listener for real-time updates
     * 
     * NOTE: Since notifiedProviderIds is stored INSIDE bookings[] array items (not at document root),
     * we must query all Bookings documents and filter client-side.
     * Firestore's whereArrayContains only works on document-level fields, not nested array items.
     */
    fun listenToNewJobs(providerId: String): Flow<List<Job>> = callbackFlow {
        // Query ALL Bookings documents (since notifiedProviderIds is nested in array)
        // This is necessary because Firestore can't query array item fields
        val listener = bookingsCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Emit empty list on error instead of closing, allowing retry
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val jobs = mutableListOf<Job>()
                snapshot.documents.forEach { document ->
                    val extractedJobs = extractJobsFromDocument(document)
                    // Filter for available jobs where provider was notified
                    // This checks notifiedProviderIds INSIDE each booking item
                    val availableJobs = extractedJobs.filter { job ->
                        job.isAvailable(providerId)
                    }
                    jobs.addAll(availableJobs)
                }

                trySend(jobs)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Listen to ongoing jobs for the provider
     * Uses snapshot listener for real-time updates
     * 
     * NOTE: Since providerId is stored INSIDE bookings[] array items,
     * we must query all Bookings documents and filter client-side.
     */
    fun listenToOngoingJobs(providerId: String): Flow<List<Job>> = callbackFlow {
        // Query ALL Bookings documents (since providerId is nested in array)
        val listener = bookingsCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Emit empty list on error instead of closing, allowing retry
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val jobs = mutableListOf<Job>()
                snapshot.documents.forEach { document ->
                    val extractedJobs = extractJobsFromDocument(document)
                    // Filter for ongoing jobs by this provider
                    val ongoingJobs = extractedJobs.filter { it.isOngoing(providerId) }
                    jobs.addAll(ongoingJobs)
                }

                trySend(jobs)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get completed jobs history with pagination
     * One-time fetch (no realtime listener)
     * 
     * NOTE: Since providerId is stored INSIDE bookings[] array items (not at document root),
     * we must query all documents and filter client-side.
     */
    suspend fun getCompletedJobs(
        providerId: String,
        limit: Int = 20,
        lastDocument: com.google.firebase.firestore.DocumentSnapshot? = null
    ): Result<Pair<List<Job>, com.google.firebase.firestore.DocumentSnapshot?>> {
        return try {
            // Query all Bookings documents (since providerId is nested in array)
            // We'll filter and sort client-side
            var query = bookingsCollection
                .limit(100) // Get more documents to filter from

            if (lastDocument != null) {
                query = query.startAfter(lastDocument)
            }

            val snapshot = query.get().await()
            val allJobs = mutableListOf<Job>()

            // Extract all jobs from all documents
            snapshot.documents.forEach { document ->
                val extractedJobs = extractJobsFromDocument(document)
                allJobs.addAll(extractedJobs)
            }

            // Filter for completed jobs by this provider and sort by completion date
            val completedJobs = allJobs
                .filter { it.isCompleted(providerId) }
                .sortedByDescending { it.completedAt?.toDate() ?: java.util.Date(0) }
                .take(limit)

            val lastDoc = if (snapshot.documents.isNotEmpty()) {
                snapshot.documents.last()
            } else {
                null
            }

            Result.success(Pair(completedJobs, lastDoc))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if provider has any ongoing job
     * 
     * NOTE: Since providerId is stored INSIDE bookings[] array items,
     * we must query all documents and filter client-side.
     */
    suspend fun hasOngoingJob(providerId: String): Boolean {
        return try {
            // Query all Bookings documents (limited for performance)
            val snapshot = bookingsCollection
                .limit(50) // Reasonable limit for checking ongoing jobs
                .get()
                .await()

            // Extract jobs and check for ongoing ones
            snapshot.documents.forEach { document ->
                val extractedJobs = extractJobsFromDocument(document)
                val hasOngoing = extractedJobs.any { it.isOngoing(providerId) }
                if (hasOngoing) {
                    return true
                }
            }

            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Accept a job by calling Cloud Function
     */
    suspend fun acceptJob(bookingId: String, providerId: String): Result<Unit> {
        return try {
            val data = hashMapOf(
                "bookingId" to bookingId,
                "providerId" to providerId
            )

            functions.getHttpsCallable("acceptJobRequest")
                .call(data)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get complete job details by booking ID
     * Fetches from Firestore and optionally from serveit_users for customer address
     */
    suspend fun getJobDetails(
        bookingId: String,
        customerPhoneNumber: String
    ): Result<Job> {
        return try {
            // Query all Bookings documents to find the job
            val snapshot = bookingsCollection.get().await()
            
            var foundJob: Job? = null
            
            snapshot.documents.forEach { document ->
                val extractedJobs = extractJobsFromDocument(document)
                val job = extractedJobs.find { it.bookingId == bookingId }
                if (job != null) {
                    foundJob = job
                    return@forEach
                }
            }
            
            if (foundJob != null) {
                // Try to fetch customer address from serveit_users if not in booking
                if (foundJob!!.customerAddress.isNullOrEmpty()) {
                    try {
                        val customerDoc = firestore.collection("serveit_users")
                            .document(customerPhoneNumber)
                            .get()
                            .await()
                        
                        val customerData = customerDoc.data
                        val address = customerData?.get("address") as? String
                            ?: customerData?.get("fullAddress") as? String
                        
                        if (address != null) {
                            foundJob = foundJob!!.copy(customerAddress = address)
                        }
                    } catch (e: Exception) {
                        // Silently fail - address is optional
                    }
                }
                
                Result.success(foundJob!!)
            } else {
                Result.failure(Exception("Job not found: $bookingId"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Extract jobs from a Bookings document
     * Handles both array format and single booking format
     */
    private fun extractJobsFromDocument(document: com.google.firebase.firestore.DocumentSnapshot): List<Job> {
        val data = document.data ?: return emptyList()
        val customerPhoneNumber = document.id
        val jobs = mutableListOf<Job>()

        // Check if document has bookings array (primary format)
        val bookingsArray = data["bookings"] as? List<Map<String, Any>>
        if (bookingsArray != null) {
            // Array format
            bookingsArray.forEach { bookingData ->
                val job = mapBookingToJob(bookingData, customerPhoneNumber)
                if (job != null) {
                    jobs.add(job)
                }
            }
        } else {
            // Single booking format (legacy)
            val job = mapBookingToJob(data, customerPhoneNumber)
            if (job != null) {
                jobs.add(job)
            }
        }

        return jobs
    }

    /**
     * Update job status in Firestore
     * Handles both array and single booking formats
     */
    suspend fun updateJobStatus(
        bookingId: String,
        customerPhoneNumber: String,
        newStatus: String,
        timestampField: String?
    ): Result<Unit> {
        return try {
            val documentRef = bookingsCollection.document(customerPhoneNumber)
            
            // Get current document
            val snapshot = documentRef.get().await()
            val data = snapshot.data ?: throw Exception("Document not found")
            
            // Check if document has bookings array (primary format)
            val bookingsArray = data["bookings"] as? MutableList<Map<String, Any>>
            if (bookingsArray != null) {
                // Array format - find and update the specific booking
                val bookingIndex = bookingsArray.indexOfFirst { 
                    (it["bookingId"] as? String) == bookingId 
                }
                
                if (bookingIndex == -1) {
                    throw Exception("Booking not found in array")
                }
                
                val booking = bookingsArray[bookingIndex].toMutableMap()
                booking["status"] = newStatus
                booking["bookingStatus"] = newStatus // Keep both fields in sync
                
                // Set timestamp if provided
                timestampField?.let { field ->
                    booking[field] = Timestamp.now()
                }
                
                bookingsArray[bookingIndex] = booking
                documentRef.update("bookings", bookingsArray).await()
            } else {
                // Single booking format (legacy)
                if ((data["bookingId"] as? String) != bookingId) {
                    throw Exception("Booking ID mismatch")
                }
                
                val updates = mutableMapOf<String, Any>(
                    "status" to newStatus,
                    "bookingStatus" to newStatus // Keep both fields in sync
                )
                timestampField?.let { field ->
                    updates[field] = Timestamp.now()
                }
                
                documentRef.update(updates).await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mark job as arrived
     */
    suspend fun markJobAsArrived(bookingId: String, customerPhoneNumber: String): Result<Unit> {
        return updateJobStatus(bookingId, customerPhoneNumber, "arrived", "arrivedAt")
    }

    /**
     * Mark job as in progress
     */
    suspend fun markJobAsInProgress(bookingId: String, customerPhoneNumber: String): Result<Unit> {
        return updateJobStatus(bookingId, customerPhoneNumber, "in_progress", "serviceStartedAt")
    }

    /**
     * Mark job as payment pending
     */
    suspend fun markJobAsPaymentPending(bookingId: String, customerPhoneNumber: String): Result<Unit> {
        return updateJobStatus(bookingId, customerPhoneNumber, "payment_pending", null)
    }

    /**
     * Mark job as completed
     */
    suspend fun markJobAsCompleted(bookingId: String, customerPhoneNumber: String): Result<Unit> {
        return updateJobStatus(bookingId, customerPhoneNumber, "completed", "completedAt")
    }

    /**
     * Update payment information for a job
     */
    suspend fun updateJobPaymentInfo(
        bookingId: String,
        customerPhoneNumber: String,
        paymentMode: String,
        paymentAmount: Double,
        paymentStatus: String,
        completionOTP: String? = null,
        otpGeneratedAt: Long? = null,
        qrUpiUri: String? = null,
        upiNote: String? = null
    ): Result<Unit> {
        return try {
            val documentRef = bookingsCollection.document(customerPhoneNumber)

            // Get current document
            val snapshot = documentRef.get().await()
            val data = snapshot.data ?: throw Exception("Document not found")

            val updates = mutableMapOf<String, Any>(
                "paymentMode" to paymentMode,
                "paymentAmount" to paymentAmount,
                "paymentStatus" to paymentStatus
            )

            // Add optional fields only if provided
            completionOTP?.let { updates["completionOTP"] = it }
            otpGeneratedAt?.let { updates["otpGeneratedAt"] = it }
            qrUpiUri?.let { updates["qrUpiUri"] = it }
            upiNote?.let { updates["upiNote"] = it }

            // Check if document has bookings array (primary format)
            val bookingsArray = data["bookings"] as? MutableList<Map<String, Any>>
            if (bookingsArray != null) {
                // Array format - find and update the specific booking
                val bookingIndex = bookingsArray.indexOfFirst {
                    (it["bookingId"] as? String) == bookingId
                }

                if (bookingIndex == -1) {
                    throw Exception("Booking not found in array")
                }

                val booking = bookingsArray[bookingIndex].toMutableMap()
                booking.putAll(updates)
                bookingsArray[bookingIndex] = booking
                documentRef.update("bookings", bookingsArray).await()
            } else {
                // Single booking format (legacy)
                if ((data["bookingId"] as? String) != bookingId) {
                    throw Exception("Booking ID mismatch")
                }

                documentRef.update(updates).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate a 6-digit OTP
     */
    fun generateOTP(): String {
        return (100000..999999).random().toString()
    }

    /**
     * Map Firestore booking data to Job model
     * Handles field name inconsistencies (status vs bookingStatus)
     */
    private fun mapBookingToJob(
        bookingData: Map<String, Any>,
        customerPhoneNumber: String
    ): Job? {
        val bookingId = bookingData["bookingId"] as? String ?: return null
        val serviceName = bookingData["serviceName"] as? String ?: "Unknown Service"
        
        // Handle status field inconsistency
        val status = (bookingData["status"] as? String) 
            ?: (bookingData["bookingStatus"] as? String)?.lowercase()
            ?: "pending"

        val totalPrice = (bookingData["totalPrice"] as? Number)?.toDouble() ?: 0.0
        val userName = bookingData["userName"] as? String ?: "Customer"
        
        val notifiedProviderIds = (bookingData["notifiedProviderIds"] as? List<*>)?.mapNotNull { 
            it as? String 
        } ?: emptyList()
        
        val providerId = bookingData["providerId"] as? String
        val providerName = bookingData["providerName"] as? String
        val providerMobileNo = bookingData["providerMobileNo"] as? String
        val acceptedByProviderId = bookingData["acceptedByProviderId"] as? String

        // Extract job coordinates
        val jobCoordinates = (bookingData["jobCoordinates"] as? Map<String, Any>)?.let { coords ->
            val lat = (coords["latitude"] as? Number)?.toDouble()
            val lng = (coords["longitude"] as? Number)?.toDouble()
            if (lat != null && lng != null) {
                JobCoordinates(lat, lng)
            } else {
                null
            }
        }

        // Extract timestamps
        val createdAt = bookingData["createdAt"] as? Timestamp
        val acceptedAt = bookingData["acceptedAt"] as? Timestamp
        val arrivedAt = bookingData["arrivedAt"] as? Timestamp
        val serviceStartedAt = bookingData["serviceStartedAt"] as? Timestamp
        val completedAt = bookingData["completedAt"] as? Timestamp

        val subServicesSelected = bookingData["subServicesSelected"] as? Map<String, Any>
        
        // Extract location name (city, locality, or address)
        val locationName = bookingData["locationName"] as? String
            ?: bookingData["city"] as? String
            ?: bookingData["locality"] as? String
            ?: bookingData["address"] as? String

        // Extract customer address (try multiple field names)
        val customerAddress = bookingData["customerAddress"] as? String
            ?: bookingData["address"] as? String
            ?: bookingData["fullAddress"] as? String
            ?: bookingData["locationAddress"] as? String

        // Extract notes/description
        val notes = bookingData["notes"] as? String
            ?: bookingData["description"] as? String
            ?: bookingData["specialInstructions"] as? String
            ?: bookingData["instructions"] as? String

        // Extract customer email
        val customerEmail = bookingData["customerEmail"] as? String
            ?: bookingData["email"] as? String

        // Extract estimated duration
        val estimatedDuration = (bookingData["estimatedDuration"] as? Number)?.toInt()
            ?: (bookingData["duration"] as? Number)?.toInt()
            ?: (bookingData["estimatedTime"] as? Number)?.toInt()

        // Extract expiresAt (if available from inbox entry)
        val expiresAt = bookingData["expiresAt"] as? Timestamp

        // Extract payment-related fields (optional, backward-compatible)
        val paymentMode = bookingData["paymentMode"] as? String
        val paymentAmount = (bookingData["paymentAmount"] as? Number)?.toDouble()
        val paymentStatus = bookingData["paymentStatus"] as? String
        val completionOTP = bookingData["completionOTP"] as? String
        val otpGeneratedAt = (bookingData["otpGeneratedAt"] as? Number)?.toLong()
        val qrUpiUri = bookingData["qrUpiUri"] as? String
        val upiNote = bookingData["upiNote"] as? String

        return Job(
            bookingId = bookingId,
            serviceName = serviceName,
            status = status.lowercase(),
            totalPrice = totalPrice,
            userName = userName,
            customerPhoneNumber = customerPhoneNumber,
            notifiedProviderIds = notifiedProviderIds,
            providerId = providerId,
            providerName = providerName,
            providerMobileNo = providerMobileNo,
            acceptedByProviderId = acceptedByProviderId,
            jobCoordinates = jobCoordinates,
            createdAt = createdAt,
            acceptedAt = acceptedAt,
            arrivedAt = arrivedAt,
            serviceStartedAt = serviceStartedAt,
            completedAt = completedAt,
            subServicesSelected = subServicesSelected,
            locationName = locationName,
            customerAddress = customerAddress,
            notes = notes,
            customerEmail = customerEmail,
            estimatedDuration = estimatedDuration,
            expiresAt = expiresAt,
            paymentMode = paymentMode,
            paymentAmount = paymentAmount,
            paymentStatus = paymentStatus,
            completionOTP = completionOTP,
            otpGeneratedAt = otpGeneratedAt,
            qrUpiUri = qrUpiUri,
            upiNote = upiNote
        )
    }

    /**
     * Listen to new jobs from inbox (for job discovery)
     * This is the optimized way to discover jobs - reads from provider-specific inbox
     */
    fun listenToNewJobsFromInbox(providerId: String): Flow<List<JobInboxEntry>> = callbackFlow {
        val listener = firestore
            .collection("provider_job_inbox")
            .document(providerId)
            .collection("jobs")
            .whereEqualTo("status", "pending")
            .whereGreaterThan("expiresAt", Timestamp.now())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val inboxEntries = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    mapInboxDataToEntry(data)
                }

                trySend(inboxEntries)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get full booking details from source of truth (Bookings collection)
     * This is the AUTHORITATIVE source - always use this for job details
     * 
     * @param customerPhone Customer phone number (document ID)
     * @param bookingIndex Index in bookings[] array
     */
    suspend fun getFullBookingDetails(
        customerPhone: String,
        bookingIndex: Int
    ): Result<Job> {
        return try {
            val documentRef = bookingsCollection.document(customerPhone)
            val snapshot = documentRef.get().await()
            val data = snapshot.data ?: throw Exception("Document not found")

            val bookingsArray = data["bookings"] as? List<Map<String, Any>>
            if (bookingsArray != null && bookingsArray.isNotEmpty()) {
                // Array format
                if (bookingIndex >= bookingsArray.size) {
                    throw Exception("Booking index out of range")
                }
                val bookingData = bookingsArray[bookingIndex]
                val job = mapBookingToJob(bookingData, customerPhone)
                if (job != null) {
                    Result.success(job)
                } else {
                    Result.failure(Exception("Failed to map booking data"))
                }
            } else {
                // Single booking format (legacy) - use index 0
                val job = mapBookingToJob(data, customerPhone)
                if (job != null) {
                    Result.success(job)
                } else {
                    Result.failure(Exception("Failed to map booking data"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Map inbox document data to JobInboxEntry
     */
    private fun mapInboxDataToEntry(data: Map<String, Any>): JobInboxEntry? {
        val bookingId = data["bookingId"] as? String ?: return null
        val customerPhone = data["customerPhone"] as? String ?: return null
        val bookingDocPath = data["bookingDocPath"] as? String ?: return null
        val bookingIndex = (data["bookingIndex"] as? Number)?.toInt() ?: 0
        val serviceName = data["serviceName"] as? String ?: "Unknown Service"
        val priceSnapshot = (data["priceSnapshot"] as? Number)?.toDouble() ?: 0.0
        val status = data["status"] as? String ?: "pending"
        val distanceKm = (data["distanceKm"] as? Number)?.toDouble()
        val createdAt = data["createdAt"] as? Timestamp
        val expiresAt = data["expiresAt"] as? Timestamp

        return JobInboxEntry(
            bookingId = bookingId,
            customerPhone = customerPhone,
            bookingDocPath = bookingDocPath,
            bookingIndex = bookingIndex,
            serviceName = serviceName,
            priceSnapshot = priceSnapshot,
            status = status,
            distanceKm = distanceKm,
            createdAt = createdAt,
            expiresAt = expiresAt
        )
    }
}

