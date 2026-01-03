package com.nextserve.serveitpartnernew.data.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.data.repository.FirestoreRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LocationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val firestoreRepository = FirestoreRepository(FirebaseProvider.firestore)

    private var providerId: String? = null
    private var isTrackingForJob: Boolean = false
    private var jobId: String? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "location_service_channel"
        private const val CHANNEL_NAME = "Location Service"

        const val ACTION_START_TRACKING = "com.nextserve.serveitpartnernew.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.nextserve.serveitpartnernew.STOP_TRACKING"
        const val ACTION_START_JOB_TRACKING = "com.nextserve.serveitpartnernew.START_JOB_TRACKING"
        const val ACTION_STOP_JOB_TRACKING = "com.nextserve.serveitpartnernew.STOP_JOB_TRACKING"

        const val EXTRA_PROVIDER_ID = "provider_id"
        const val EXTRA_JOB_ID = "job_id"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        setupLocationCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> {
                providerId = intent.getStringExtra(EXTRA_PROVIDER_ID)
                startForegroundTracking()
            }
            ACTION_STOP_TRACKING -> {
                stopLocationUpdates()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_START_JOB_TRACKING -> {
                providerId = intent.getStringExtra(EXTRA_PROVIDER_ID)
                jobId = intent.getStringExtra(EXTRA_JOB_ID)
                isTrackingForJob = true
                startForegroundTracking()
            }
            ACTION_STOP_JOB_TRACKING -> {
                stopLocationUpdates()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundTracking() {
        val notification = createNotification("Tracking your location for better job matching")
        startForeground(NOTIFICATION_ID, notification)
        requestLocationUpdates()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Location tracking for job service"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): android.app.Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Tracking Active")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    handleLocationUpdate(location)
                }
            }

            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                if (!locationAvailability.isLocationAvailable) {
                    // Handle location unavailable
                }
            }
        }
    }

    private fun requestLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            30000L // 30 seconds
        ).apply {
            setMinUpdateDistanceMeters(50f) // Update every 50 meters
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            setWaitForAccurateLocation(true)
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Handle security exception
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun handleLocationUpdate(location: Location) {
        providerId?.let { id ->
            serviceScope.launch {
                try {
                    // Update location in Firestore using nested structure
                    val locationUpdate = mapOf(
                        "latitude" to location.latitude,
                        "longitude" to location.longitude,
                        "locationDetails" to mapOf(
                            "latitude" to location.latitude,
                            "longitude" to location.longitude,
                            "address" to "Current Location" // Could be reverse geocoded
                        )
                    )

                    firestoreRepository.updateProviderData(id, locationUpdate)

                    // If tracking for a specific job, also update job location
                    if (isTrackingForJob && jobId != null) {
                        // TODO: Update job-specific location tracking
                    }
                } catch (e: Exception) {
                    // Handle location update failure
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }
}
