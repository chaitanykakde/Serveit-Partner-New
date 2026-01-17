package com.nextserve.serveitpartnernew.data.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nextserve.serveitpartnernew.MainActivity
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.data.repository.AuthRepository
import com.nextserve.serveitpartnernew.ui.screen.call.IncomingCallActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ServeitFirebaseMessagingService : FirebaseMessagingService() {
    
    private val authRepository = AuthRepository()
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Check if this is a call notification
        val isCall = remoteMessage.data["type"] == "incoming_call" ||
                    remoteMessage.data["callId"] != null

        if (isCall) {
            handleIncomingCallNotification(remoteMessage)
        } else {
            // Handle regular notifications
            remoteMessage.notification?.let { notification ->
                showNotification(
                    title = notification.title ?: "Serveit Partner",
                    body = notification.body ?: "",
                    data = remoteMessage.data
                )
            }
        }
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        android.util.Log.d("ServeitFirebaseMessagingService", "🔄 New FCM token received")
        
        // Get current logged-in user ID
        val uid = com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider.auth.currentUser?.uid
        
        if (uid != null) {
            // User is logged in - save token to Firestore using exact Cloud Functions structure
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = authRepository.saveFcmToken(uid)
                    result.onSuccess {
                        android.util.Log.d("ServeitFirebaseMessagingService", "✅ FCM token updated successfully in Firestore")
                    }.onFailure { exception ->
                        android.util.Log.w("ServeitFirebaseMessagingService", "⚠️ Failed to update FCM token in Firestore: ${exception.message}")
                        // Non-critical error - token will be saved on next login
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ServeitFirebaseMessagingService", "❌ Exception updating FCM token: ${e.message}", e)
                    // Non-critical error - token will be saved on next login
                }
            }
        } else {
            android.util.Log.d("ServeitFirebaseMessagingService", "ℹ️ User not logged in, token will be saved on next login")
            // User not logged in - token will be saved automatically on next login
        }
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Regular notifications channel
            val regularChannel = NotificationChannel(
                CHANNEL_ID,
                "Serveit Partner Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for profile status updates"
            }

            // Call notifications channel - MAX importance like WhatsApp
            val callChannel = NotificationChannel(
                CALL_CHANNEL_ID,
                "Incoming Calls",
                NotificationManager.IMPORTANCE_MAX
            ).apply {
                description = "Incoming voice call notifications"
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE), null)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
                setBypassDnd(true) // Bypass Do Not Disturb for calls
            }

            notificationManager.createNotificationChannel(regularChannel)
            notificationManager.createNotificationChannel(callChannel)
        }
    }
    
    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Add data to intent if needed
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.serveitpartnerlogo)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun handleIncomingCallNotification(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data

        // Extract call data
        val callId = data["callId"] ?: return
        val bookingId = data["bookingId"] ?: data["callId"] ?: return
        val userMobile = data["userMobile"] ?: return
        val serviceName = data["serviceName"] ?: "Service Call"
        val initiatedBy = data["initiatedBy"] ?: "USER"

        // Wake the device for incoming call (like WhatsApp)
        wakeDeviceForCall()

        // Launch IncomingCallActivity directly (like WhatsApp)
        val callIntent = IncomingCallActivity.createIntent(
            context = this,
            callId = callId,
            bookingId = bookingId,
            userMobile = userMobile,
            serviceName = serviceName,
            initiatedBy = initiatedBy
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                   Intent.FLAG_ACTIVITY_CLEAR_TOP or
                   Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
            // Add call-specific flags
            putExtra("from_fcm", true)
        }

        try {
            startActivity(callIntent)
            android.util.Log.d("FCM", "Successfully launched IncomingCallActivity for call: $callId")
        } catch (e: Exception) {
            android.util.Log.e("FCM", "Failed to launch IncomingCallActivity, showing notification fallback", e)
            // Fallback to notification if activity launch fails
            showCallNotification(callId, bookingId, userMobile, serviceName)
        }
    }

    private fun wakeDeviceForCall() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE,
                "ServeitCall:IncomingCallWakeLock"
            )
            wakeLock.acquire(10000) // Wake for 10 seconds
            wakeLock.release()
        } catch (e: Exception) {
            android.util.Log.e("FCM", "Failed to acquire wake lock", e)
        }
    }

    private fun showCallNotification(callId: String, bookingId: String, userMobile: String, serviceName: String) {
        // Create full-screen intent for call notification
        val callIntent = IncomingCallActivity.createIntent(
            context = this,
            callId = callId,
            bookingId = bookingId,
            userMobile = userMobile,
            serviceName = serviceName,
            initiatedBy = "USER"
        )

        val fullScreenIntent = PendingIntent.getActivity(
            this,
            callId.hashCode(),
            callIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CALL_CHANNEL_ID)
            .setContentTitle("Incoming Call")
            .setContentText("$serviceName from ${userMobile.takeLast(4)}")
            .setSmallIcon(R.drawable.serveitpartnerlogo)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(
                R.drawable.ic_call_end,
                "Decline",
                createDeclinePendingIntent(callId)
            )
            .addAction(
                android.R.drawable.ic_menu_call,
                "Answer",
                fullScreenIntent
            )
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(CALL_NOTIFICATION_ID, notification)
    }

    private fun createDeclinePendingIntent(callId: String): PendingIntent {
        val declineIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = "DECLINE_CALL"
            putExtra("callId", callId)
        }

        return PendingIntent.getBroadcast(
            this,
            callId.hashCode() + 1,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val CHANNEL_ID = "serveit_partner_notifications"
        private const val CALL_CHANNEL_ID = "incoming_calls"
        private const val CALL_NOTIFICATION_ID = 9999
    }
}

