package com.nextserve.serveitpartnernew.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.data.repository.AgoraRepository
import com.nextserve.serveitpartnernew.data.repository.CallPermissionResponse
import com.nextserve.serveitpartnernew.ui.screen.call.ProviderCallActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Foreground service to handle call setup operations
 * Survives Activity lifecycle changes to prevent call initialization cancellation
 */
class CallSetupService : Service() {

    companion object {
        private const val TAG = "CallSetupService"
        private const val CHANNEL_ID = "call_setup"
        private const val CHANNEL_NAME = "Call Setup"
        private const val NOTIFICATION_ID = 2001

        private const val EXTRA_BOOKING_ID = "booking_id"
        private const val EXTRA_ACTION = "action"

        const val ACTION_START_SETUP = "start_setup"
        const val ACTION_END_SETUP = "end_setup"

        // Singleton instance for communication
        private var instance: CallSetupService? = null

        fun startCallSetup(context: Context, bookingId: String) {
            val intent = Intent(context, CallSetupService::class.java).apply {
                putExtra(EXTRA_BOOKING_ID, bookingId)
                putExtra(EXTRA_ACTION, ACTION_START_SETUP)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopCallSetup(context: Context) {
            val intent = Intent(context, CallSetupService::class.java).apply {
                putExtra(EXTRA_ACTION, ACTION_END_SETUP)
            }
            context.startService(intent)
        }

        fun getInstance(): CallSetupService? = instance
    }

    // Service coroutine scope - survives Activity lifecycle
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentJob: Job? = null

    // State management
    private val _setupState = MutableStateFlow<CallSetupState>(CallSetupState.Idle)
    val setupState: StateFlow<CallSetupState> = _setupState.asStateFlow()

    // Track if foreground has been started
    private var foregroundStarted = false

    // Dependencies
    private lateinit var agoraRepository: AgoraRepository
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "CallSetupService created")

        // Initialize dependencies
        agoraRepository = AgoraRepository()

        // Create notification channel (but don't start foreground yet)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start foreground service (only once)
        if (!foregroundStarted) {
            startForeground(NOTIFICATION_ID, createSetupNotification())
            foregroundStarted = true
            Log.d(TAG, "Foreground service started")
        }

        val action = intent?.getStringExtra(EXTRA_ACTION)
        val bookingId = intent?.getStringExtra(EXTRA_BOOKING_ID)

        Log.d(TAG, "Received action: $action, bookingId: $bookingId")

        when (action) {
            ACTION_START_SETUP -> {
                bookingId?.let { startCallSetup(it) }
            }
            ACTION_END_SETUP -> {
                endCallSetup()
            }
        }

        return START_NOT_STICKY // Don't restart if killed
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Start call setup for a booking
     */
    private fun startCallSetup(bookingId: String) {
        // Cancel any existing setup
        currentJob?.cancel()

        currentJob = serviceScope.launch {
            try {
                Log.d(TAG, "🚀 STARTING CALL SETUP - Booking: $bookingId")
                _setupState.value = CallSetupState.ValidatingPermission(bookingId)

                // Step 1: Validate call permission
                validateCallPermission(bookingId)

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.w(TAG, "Call setup was cancelled for booking: $bookingId")
                } else {
                    Log.e(TAG, "Call setup failed for booking: $bookingId", e)
                    _setupState.value = CallSetupState.Error("Setup failed: ${e.message}")
                }
            }
        }
    }

    /**
     * Resolve the user mobile number by finding which user document contains the booking
     */
    private suspend fun resolveUserMobileForBooking(bookingId: String): String? {
        return try {
            Log.d(TAG, "🔍 RESOLVING USER MOBILE for booking: $bookingId")

            // Query all Bookings documents to find the one containing this bookingId
            val querySnapshot = db.collection("Bookings").get().await()
            Log.d(TAG, "📊 Querying ${querySnapshot.documents.size} user documents")

            // Search through all user documents
            for (userDoc in querySnapshot.documents) {
                val bookings = userDoc.get("bookings") as? List<Map<String, Any>> ?: continue

                // Look for the specific booking in this user's bookings
                val booking = bookings.find { it["bookingId"] == bookingId }
                if (booking != null) {
                    val userMobile = userDoc.id
                    Log.d(TAG, "✅ FOUND booking under USER: $userMobile")
                    return userMobile
                }
            }

            Log.w(TAG, "❌ Booking not found in any user documents: $bookingId")
            null

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error resolving user mobile for booking: $bookingId", e)
            null
        }
    }

    /**
     * Validate call permission and get booking details
     */
    private suspend fun validateCallPermission(bookingId: String) {
        Log.d(TAG, "📋 VALIDATING PERMISSION - Booking: $bookingId")

        // Step 1: Resolve userMobile by finding the booking across all user documents
        val userMobile = resolveUserMobileForBooking(bookingId)
        if (userMobile == null) {
            Log.e(TAG, "❌ FAILED TO RESOLVE USER MOBILE - Booking: $bookingId")
            _setupState.value = CallSetupState.Error("Could not find booking owner")
            return
        }

        Log.d(TAG, "✅ RESOLVED USER MOBILE: $userMobile for booking: $bookingId")

        // Step 2: Call Cloud Function with resolved userMobile
        agoraRepository.validateCallPermission(bookingId, userMobile, "PROVIDER").fold(
            onSuccess = { permission ->
                if (permission.allowed) {
                    Log.d(TAG, "✅ PERMISSION GRANTED - Booking: $bookingId, Status: ${permission.bookingStatus}")
                    _setupState.value = CallSetupState.PermissionGranted(
                        bookingId = bookingId,
                        customerName = permission.customerName,
                        serviceName = permission.serviceName,
                        bookingStatus = permission.bookingStatus
                    )
                    // Proceed to token generation
                    generateAgoraToken(bookingId, userMobile)
                } else {
                    Log.w(TAG, "❌ PERMISSION DENIED - Booking: $bookingId, Status: ${permission.bookingStatus}")
                    _setupState.value = CallSetupState.PermissionDenied(
                        bookingId = bookingId,
                        reason = "Cannot call with status: ${permission.bookingStatus}"
                    )
                }
            },
            onFailure = { error ->
                // If validation function doesn't exist, proceed anyway
                if (error.message?.contains("NOT_FOUND") == true) {
                    Log.w(TAG, "⚠️ VALIDATION FUNCTION MISSING - Proceeding with defaults for booking: $bookingId")
                    _setupState.value = CallSetupState.PermissionGranted(
                        bookingId = bookingId,
                        customerName = "Customer",
                        serviceName = "Service Call",
                        bookingStatus = "unknown"
                    )
                    // Proceed to token generation
                    generateAgoraToken(bookingId, userMobile)
                } else {
                    Log.e(TAG, "❌ PERMISSION VALIDATION FAILED - Booking: $bookingId", error)
                    _setupState.value = CallSetupState.Error(
                        "Permission validation failed: ${error.message}"
                    )
                }
            }
        )
    }

    /**
     * Generate Agora token and initialize engine
     */
    private suspend fun generateAgoraToken(bookingId: String, userMobile: String) {
        Log.d(TAG, "🎫 GENERATING TOKEN - Booking: $bookingId, User: $userMobile")
        _setupState.value = CallSetupState.GeneratingToken(bookingId)

        agoraRepository.generateAgoraToken(bookingId, userMobile).fold(
            onSuccess = { tokenResponse ->
                Log.d(TAG, "✅ TOKEN GENERATED - Channel: ${tokenResponse.channelName}")
                _setupState.value = CallSetupState.TokenGenerated(
                    bookingId = bookingId,
                    token = tokenResponse.token,
                    channelName = tokenResponse.channelName,
                    uid = tokenResponse.uid,
                    appId = tokenResponse.appId
                )

                // Create ActiveCalls document for outgoing call
                createActiveCallDocument(
                    bookingId = bookingId,
                    userMobile = userMobile,
                    channelName = tokenResponse.channelName,
                    token = tokenResponse.token,
                    appId = tokenResponse.appId,
                    initiatedBy = "PROVIDER"
                )

                // Proceed to Agora engine initialization
                initializeAgoraEngine(tokenResponse.appId, tokenResponse.channelName, tokenResponse.token, tokenResponse.uid)
            },
            onFailure = { error ->
                Log.e(TAG, "❌ TOKEN GENERATION FAILED - Booking: $bookingId", error)
                val errorMessage = if (error.message?.contains("NOT_FOUND") == true) {
                    "Voice calling service not available"
                } else {
                    error.message ?: "Failed to generate call token"
                }
                _setupState.value = CallSetupState.Error(errorMessage)
            }
        )
    }

    /**
     * Create ActiveCalls document for call signaling
     */
    private suspend fun createActiveCallDocument(
        bookingId: String,
        userMobile: String,
        channelName: String,
        token: String,
        appId: String,
        initiatedBy: String
    ) {
        try {
            Log.d(TAG, "📝 Creating ActiveCalls document for booking: $bookingId, initiatedBy: $initiatedBy")

            // Get provider information from current user
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val providerId = currentUser?.uid ?: ""
            val providerPhone = currentUser?.phoneNumber ?: ""

            // TODO: Get service name from booking data
            // For now, use default service name to fix compilation
            val serviceName = "Service Call"

            // Create ActiveCalls document
            val activeCallData = hashMapOf(
                "bookingId" to bookingId,
                "userMobile" to userMobile,
                "providerId" to providerId,
                "providerPhone" to providerPhone,
                "serviceName" to serviceName,
                "status" to "RINGING",
                "channelName" to channelName,
                "token" to token,
                "appId" to appId,
                "initiatedBy" to initiatedBy,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            db.collection("ActiveCalls").document(bookingId)
                .set(activeCallData)
                .await()

            Log.d(TAG, "✅ ActiveCalls document created for booking: $bookingId")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create ActiveCalls document", e)
            // Continue with call setup even if document creation fails
        }
    }

    /**
     * Initialize Agora engine and join channel
     */
    private fun initializeAgoraEngine(appId: String, channelName: String, token: String, uid: Int) {
        Log.d(TAG, "🎯 INITIALIZING AGORA ENGINE - Channel: $channelName")
        _setupState.value = CallSetupState.InitializingEngine(channelName)

        try {
            // Get the call manager instance
            val callManager = ProviderCallManager.getInstance(this)

            // Initialize engine with App ID
            callManager.initializeEngine(appId)

            // Join the channel
            callManager.joinChannel(channelName, token, uid, appId)

            Log.d(TAG, "✅ AGORA ENGINE INITIALIZED & CHANNEL JOINED")
            _setupState.value = CallSetupState.Ready

        } catch (e: Exception) {
            Log.e(TAG, "❌ AGORA ENGINE INITIALIZATION FAILED", e)
            _setupState.value = CallSetupState.Error("Engine initialization failed: ${e.message}")
        }
    }

    /**
     * Public method to end call setup and log call duration
     */
    fun endCallAndLog(durationSeconds: Long, bookingId: String? = null) {
        serviceScope.launch {
            // Update ActiveCalls document status to ENDED
            if (bookingId != null) {
                try {
                    db.collection("ActiveCalls").document(bookingId)
                        .update(
                            "status", "ENDED",
                            "durationSeconds", durationSeconds,
                            "endedAt", com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )
                        .await()
                    Log.d(TAG, "✅ ActiveCalls document updated to ENDED for booking: $bookingId")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to update ActiveCalls document", e)
                }
            }

            // Log call end to backend
            // This would call AgoraRepository.endCall but for now just log
            Log.d(TAG, "Call ended, duration: ${durationSeconds}s")
        }

        endCallSetup()
    }

    /**
     * End call setup
     */
    private fun endCallSetup() {
        Log.d(TAG, "🛑 ENDING CALL SETUP")

        // Cancel current job
        currentJob?.cancel()
        currentJob = null

        // Reset state
        _setupState.value = CallSetupState.Idle

        // Leave channel if active
        ProviderCallManager.getInstance(this).leaveChannel()

        // Stop service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Call setup in progress"
                setShowBadge(false)
                enableVibration(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createSetupNotification(): android.app.Notification {
        // Create intent for call activity (if needed)
        val intent = Intent(this, ProviderCallActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Setting up voice call")
            .setContentText("Initializing secure connection...")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "CallSetupService destroyed")

        // Clean up
        currentJob?.cancel()
        serviceScope.cancel()
        instance = null
    }
}

/**
 * States for call setup process
 */
sealed class CallSetupState {
    object Idle : CallSetupState()

    data class ValidatingPermission(val bookingId: String) : CallSetupState()
    data class PermissionGranted(
        val bookingId: String,
        val customerName: String,
        val serviceName: String,
        val bookingStatus: String
    ) : CallSetupState()
    data class PermissionDenied(val bookingId: String, val reason: String) : CallSetupState()

    data class GeneratingToken(val bookingId: String) : CallSetupState()
    data class TokenGenerated(
        val bookingId: String,
        val token: String,
        val channelName: String,
        val uid: Int,
        val appId: String
    ) : CallSetupState()

    data class InitializingEngine(val channelName: String) : CallSetupState()
    object Ready : CallSetupState()

    data class Error(val message: String) : CallSetupState()
}
