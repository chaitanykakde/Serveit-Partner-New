package com.nextserve.serveitpartnernew.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent?.hasError() == true) {
            // Handle geofencing error
            return
        }

        val geofenceTransition = geofencingEvent?.geofenceTransition
        val triggeringGeofences = geofencingEvent?.triggeringGeofences

        triggeringGeofences?.forEach { geofence ->
            val jobId = geofence.requestId
            val providerId = FirebaseProvider.auth.currentUser?.uid ?: return@forEach

            // Start GeofencingService to handle the transition
            val serviceIntent = Intent(context, GeofencingService::class.java).apply {
                action = "HANDLE_TRANSITION" // Custom action for handling transitions
                putExtra("job_id", jobId)
                putExtra("provider_id", providerId)
                putExtra("transition", geofenceTransition)
            }

            context.startService(serviceIntent)

            // Alternatively, handle directly here
            handleGeofenceTransition(context, jobId, providerId, geofenceTransition ?: return@forEach)
        }
    }

    private fun handleGeofenceTransition(
        context: Context,
        jobId: String,
        providerId: String,
        transition: Int
    ) {
        val geofencingService = GeofencingService()
        geofencingService.handleGeofenceTransition(jobId, providerId, transition)
    }
}
