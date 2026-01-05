package com.nextserve.serveitpartnernew.data.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
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
            if (!isRunning()) {
                val intent = Intent(context, IncomingCallListenerService::class.java)
                ContextCompat.startForegroundService(context, intent)
            }
        }

        fun stopService(context: Context) {
            instance?.let {
                it.stopSelf()
                instance = null
            }
        }
    }

    private val db = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null
    private var currentProviderId: String? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "IncomingCallListenerService created")
        instance = this
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "IncomingCallListenerService started")

        // Start foreground with notification
        val notification = createForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Get current provider ID
        currentProviderId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentProviderId == null) {
            Log.w(TAG, "No authenticated user, cannot start call listener")
            stopSelf()
            return START_NOT_STICKY
        }

        // Start listening for incoming calls
        startListeningForIncomingCalls()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "IncomingCallListenerService destroyed")
        stopListeningForIncomingCalls()
        instance = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startListeningForIncomingCalls() {
        if (currentProviderId == null) return

        Log.d(TAG, "Starting to listen for incoming calls for provider: $currentProviderId")

        // Listen for ActiveCalls documents where:
        // - providerId matches current provider
        // - status is "RINGING"
        listenerRegistration = db.collection("ActiveCalls")
            .whereEqualTo("providerId", currentProviderId)
            .whereEqualTo("status", "RINGING")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for incoming calls", error)
                    return@addSnapshotListener
                }

                if (snapshots == null) return@addSnapshotListener

                for (documentChange in snapshots.documentChanges) {
                    val doc = documentChange.document
                    val callData = doc.data

                    Log.d(TAG, "Incoming call detected: ${doc.id}")
                    Log.d(TAG, "Call data: $callData")

                    // Check if this is a new RINGING call
                    if (documentChange.type.name == "ADDED") {
                        handleIncomingCall(doc.id, callData)
                    }
                }
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

        Log.d(TAG, "Handling incoming call: $callId, booking: $bookingId, from: $userMobile")

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
            .setContentTitle("Call Listener Active")
            .setContentText("Listening for incoming calls")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
