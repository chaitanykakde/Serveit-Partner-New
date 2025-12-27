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

/**
 * Repository for managing jobs/bookings from Firestore
 * Handles data normalization and Firestore queries
 */
class JobsRepository(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions
) {
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
            locationName = locationName
        )
    }
}

