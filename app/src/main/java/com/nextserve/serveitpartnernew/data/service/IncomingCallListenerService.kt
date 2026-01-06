package com.nextserve.serveitpartnernew.data.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.ui.screen.call.IncomingCallActivity

/**
 * Background service that listens for incoming calls from users
 * Monitors ActiveCalls collection for RINGING status calls addressed to current provider
 */
class IncomingCallListenerService : Service() {

    companion object {
        private const val TAG = "IncomingCallListener"
        private const val CHANNEL_ID = "incoming_calls"
        private const val CHANNEL_NAME = "Incoming Calls"
        private const val NOTIFICATION_ID = 3001

        // Singleton instance
        private var instance: IncomingCallListenerService? = null

        fun isRunning(): Boolean = instance != null

        fun startService(context: Context) {
            try {
                Log.d(TAG, "Attempting to start IncomingCallListenerService")
                val intent = Intent(context, IncomingCallListenerService::class.java)
                ContextCompat.startForegroundService(context, intent)
                Log.d(TAG, "IncomingCallListenerService start command sent")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start IncomingCallListenerService", e)
            }
        }

        fun stopService(context: Context) {
            try {
                Log.d(TAG, "Stopping IncomingCallListenerService")
                instance?.let {
                    it.stopSelf()
                    instance = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping IncomingCallListenerService", e)
            }
        }
    }

    private val db = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null
    private var currentProviderId: String? = null
    private var notificationUpdateHandler: Handler? = null
    private var notificationUpdateRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "IncomingCallListenerService created")
        instance = this
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "IncomingCallListenerService started with flags: $flags, startId: $startId")

        // Get current provider ID first
        currentProviderId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentProviderId == null) {
            Log.w(TAG, "No authenticated user, cannot start call listener")
            // Don't stop self immediately - wait for auth state change
            // Instead, schedule a retry
            Handler(Looper.getMainLooper()).postDelayed({
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    currentProviderId = userId
                    startListeningForIncomingCalls()
                } else {
                    Log.e(TAG, "Still no authenticated user after delay, stopping service")
                    stopSelf()
                }
            }, 3000) // Wait 3 seconds for auth to initialize
            return START_NOT_STICKY
        }

        // Start foreground with notification
        try {
            val notification = createForegroundNotification()
            startForeground(NOTIFICATION_ID, notification)
            Log.d(TAG, "Foreground notification started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground notification", e)
            stopSelf()
            return START_NOT_STICKY
        }

        // Start listening for incoming calls
        startListeningForIncomingCalls()

        // Start periodic notification updates to keep service alive
        startNotificationUpdates()

        // Return START_STICKY to ensure service restarts if killed
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.w(TAG, "App task removed, restarting service")
        // Restart the service when app is swiped away
        val restartIntent = Intent(this, IncomingCallListenerService::class.java)
        ContextCompat.startForegroundService(this, restartIntent)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        Log.d(TAG, "IncomingCallListenerService destroyed")
        stopListeningForIncomingCalls()
        stopNotificationUpdates()
        instance = null
        super.onDestroy()
    }

    private fun startListeningForIncomingCalls() {
        if (currentProviderId == null) {
            Log.w(TAG, "Cannot start listening - no provider ID")
            return
        }

        Log.d(TAG, "Starting to listen for incoming calls for provider: $currentProviderId")

        // Stop any existing listener first
        stopListeningForIncomingCalls()

        try {
            // Listen for ActiveCalls documents where:
            // - providerId matches current provider
            // - status is "RINGING"
            listenerRegistration = db.collection("ActiveCalls")
                .whereEqualTo("providerId", currentProviderId)
                .whereEqualTo("status", "RINGING")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening for incoming calls", error)
                        // Schedule reconnection after error
                        Handler(Looper.getMainLooper()).postDelayed({
                            Log.d(TAG, "Attempting to reconnect listener after error")
                            startListeningForIncomingCalls()
                        }, 5000) // Retry after 5 seconds
                        return@addSnapshotListener
                    }

                    if (snapshots == null) {
                        Log.w(TAG, "Received null snapshots")
                        return@addSnapshotListener
                    }

                    Log.d(TAG, "Received ${snapshots.documentChanges.size} document changes")

                    for (documentChange in snapshots.documentChanges) {
                        val doc = documentChange.document
                        val callData = doc.data

                        Log.d(TAG, "Document change: ${documentChange.type} for call ${doc.id}")
                        Log.d(TAG, "Call data: $callData")

                        // Check if this is a new RINGING call
                        if (documentChange.type.name == "ADDED") {
                            val initiatedBy = callData?.get("initiatedBy") as? String ?: "USER"
                            Log.d(TAG, "Processing ${documentChange.type.name} call initiated by: $initiatedBy")
                            handleIncomingCall(doc.id, callData)
                        }
                    }
                }

            Log.d(TAG, "Firestore listener successfully attached")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening for incoming calls", e)
            // Schedule retry
            Handler(Looper.getMainLooper()).postDelayed({
                startListeningForIncomingCalls()
            }, 3000)
        }
    }

    private fun stopListeningForIncomingCalls() {
        listenerRegistration?.remove()
        listenerRegistration = null
        Log.d(TAG, "Stopped listening for incoming calls")
    }

    private fun handleIncomingCall(callId: String, callData: Map<String, Any>?) {
        if (callData == null) return

        val bookingId = callData["bookingId"] as? String ?: return
        val userMobile = callData["userMobile"] as? String ?: return
        val serviceName = callData["serviceName"] as? String ?: "Service Call"
        val initiatedBy = callData["initiatedBy"] as? String ?: "USER"

        // IMPORTANT: Only handle USER-initiated calls, not PROVIDER-initiated calls
        // When a provider initiates a call, they shouldn't receive their own call
        if (initiatedBy != "USER") {
            Log.d(TAG, "Ignoring $initiatedBy-initiated call: $callId (only handling USER calls)")
            return
        }

        Log.d(TAG, "Handling incoming USER call: $callId, booking: $bookingId, from: $userMobile")

        try {
            // Launch incoming call activity
            val intent = IncomingCallActivity.createIntent(
                context = this,
                callId = callId,
                bookingId = bookingId,
                userMobile = userMobile,
                serviceName = serviceName,
                initiatedBy = initiatedBy
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            startActivity(intent)
            Log.d(TAG, "Successfully launched IncomingCallActivity for call: $callId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch IncomingCallActivity for call: $callId", e)
            // Activity failed to start - the call listener service will continue running
            // The call will remain in RINGING state until handled by another means
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors for incoming voice calls"
                setShowBadge(false)
                setSound(null, null)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundNotification(): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voice Call Service Active")
            .setContentText("Ready to receive incoming calls")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOnlyAlertOnce(true) // Don't spam with sound/vibration
            .setShowWhen(false) // Don't show timestamp
            .build()
    }

    private fun startNotificationUpdates() {
        notificationUpdateHandler = Handler(Looper.getMainLooper())
        notificationUpdateRunnable = object : Runnable {
            override fun run() {
                try {
                    // Update notification periodically to keep service alive
                    val notificationManager = getSystemService(NotificationManager::class.java)
                    val notification = createForegroundNotification()
                    notificationManager.notify(NOTIFICATION_ID, notification)

                    // Schedule next update in 5 minutes
                    notificationUpdateHandler?.postDelayed(this, 5 * 60 * 1000)
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating notification", e)
                }
            }
        }

        // Start first update after 1 minute
        notificationUpdateHandler?.postDelayed(notificationUpdateRunnable!!, 60 * 1000)
    }

    private fun stopNotificationUpdates() {
        notificationUpdateRunnable?.let { notificationUpdateHandler?.removeCallbacks(it) }
        notificationUpdateHandler = null
        notificationUpdateRunnable = null
    }
}
