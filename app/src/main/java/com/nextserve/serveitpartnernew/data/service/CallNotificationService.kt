package com.nextserve.serveitpartnernew.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.nextserve.serveitpartnernew.MainActivity
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.ui.screen.call.ProviderCallActivity

/**
 * Foreground service to handle incoming call notifications
 * Runs continuously to listen for incoming calls and show high-priority notifications
 */
class CallNotificationService : Service() {

    companion object {
        private const val CHANNEL_ID = "call_notifications"
        private const val CHANNEL_NAME = "Call Notifications"
        private const val NOTIFICATION_ID = 1001

        fun startService(context: Context) {
            val intent = Intent(context, CallNotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, CallNotificationService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createForegroundNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Service will run until explicitly stopped
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Not a bound service
    }

    /**
     * Show incoming call notification
     */
    fun showIncomingCallNotification(
        bookingId: String,
        customerName: String,
        serviceName: String
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Accept call intent
        val acceptIntent = ProviderCallActivity.createIntent(this, bookingId).apply {
            action = "ACCEPT_CALL"
        }
        val acceptPendingIntent = PendingIntent.getActivity(
            this,
            0,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Decline call intent (just dismiss notification)
        val declineIntent = Intent(this, CallNotificationService::class.java).apply {
            action = "DECLINE_CALL"
        }
        val declinePendingIntent = PendingIntent.getService(
            this,
            1,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Incoming Call")
            .setContentText("$customerName - $serviceName")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(acceptPendingIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(
                R.drawable.ic_call_end,
                "Decline",
                declinePendingIntent
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Accept",
                acceptPendingIntent
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    /**
     * Hide incoming call notification
     */
    fun hideIncomingCallNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID + 1)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for incoming voice calls"
                setShowBadge(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundNotification(): android.app.Notification {
        // Create intent for main activity
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Serveit Partner")
            .setContentText("Listening for incoming calls")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        hideIncomingCallNotification()
    }
}
