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
import com.google.android.gms.location.*
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.data.repository.JobsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GeofencingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var geofencePendingIntent: PendingIntent
    private val jobsRepository = JobsRepository(
        FirebaseProvider.firestore,
        com.google.firebase.functions.FirebaseFunctions.getInstance()
    )

    private val activeGeofences = mutableMapOf<String, Geofence>()

    companion object {
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "geofencing_channel"
        private const val CHANNEL_NAME = "Geofencing Service"

        const val ACTION_ADD_GEOFENCE = "com.nextserve.serveitpartnernew.ADD_GEOFENCE"
        const val ACTION_REMOVE_GEOFENCE = "com.nextserve.serveitpartnernew.REMOVE_GEOFENCE"
        const val ACTION_REMOVE_ALL_GEOFENCES = "com.nextserve.serveitpartnernew.REMOVE_ALL_GEOFENCES"

        const val EXTRA_JOB_ID = "job_id"
        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
        const val EXTRA_RADIUS = "radius"
        const val EXTRA_PROVIDER_ID = "provider_id"
    }

    override fun onCreate() {
        super.onCreate()
        geofencingClient = LocationServices.getGeofencingClient(this)
        createNotificationChannel()
        geofencePendingIntent = createGeofencePendingIntent()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ADD_GEOFENCE -> {
                val jobId = intent.getStringExtra(EXTRA_JOB_ID) ?: return START_NOT_STICKY
                val latitude = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0)
                val longitude = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0)
                val radius = intent.getFloatExtra(EXTRA_RADIUS, 100f)
                val providerId = intent.getStringExtra(EXTRA_PROVIDER_ID) ?: return START_NOT_STICKY

                addGeofence(jobId, latitude, longitude, radius, providerId)
                startForeground(NOTIFICATION_ID, createNotification())
            }
            ACTION_REMOVE_GEOFENCE -> {
                val jobId = intent.getStringExtra(EXTRA_JOB_ID)
                jobId?.let { removeGeofence(it) }
            }
            ACTION_REMOVE_ALL_GEOFENCES -> {
                removeAllGeofences()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Geofencing for job arrival detection"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): android.app.Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Monitoring Active")
            .setContentText("Monitoring for job location arrival")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createGeofencePendingIntent(): PendingIntent {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun addGeofence(jobId: String, latitude: Double, longitude: Double, radius: Float, providerId: String) {
        val geofence = Geofence.Builder()
            .setRequestId(jobId)
            .setCircularRegion(latitude, longitude, radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .setLoiteringDelay(30000) // 30 seconds delay for dwell
            .build()

        activeGeofences[jobId] = geofence

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        try {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                .addOnSuccessListener {
                    // Geofence added successfully
                }
                .addOnFailureListener { exception ->
                    // Handle geofence addition failure
                }
        } catch (e: SecurityException) {
            // Handle security exception
        }
    }

    private fun removeGeofence(jobId: String) {
        activeGeofences.remove(jobId)
        geofencingClient.removeGeofences(listOf(jobId))
    }

    private fun removeAllGeofences() {
        activeGeofences.clear()
        geofencingClient.removeGeofences(geofencePendingIntent)
    }

    fun handleGeofenceTransition(jobId: String, providerId: String, transition: Int) {
        serviceScope.launch {
            when (transition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    // Provider has arrived at job location
                    // Auto-update job status to "arrived" if currently "accepted"
                    try {
                        jobsRepository.updateJobStatus(jobId, providerId, "arrived", "arrivedAt")
                        // Could send notification to provider about arrival
                    } catch (e: Exception) {
                        // Handle error
                    }
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    // Provider has left job location
                    // Could be used for additional tracking
                }
                Geofence.GEOFENCE_TRANSITION_DWELL -> {
                    // Provider has been at location for loitering delay
                    // Could trigger additional actions
                }
            }
        }
    }
}
