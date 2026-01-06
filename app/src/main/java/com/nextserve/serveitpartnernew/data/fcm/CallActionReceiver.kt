package com.nextserve.serveitpartnernew.data.fcm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val callId = intent.getStringExtra("callId") ?: return
        val action = intent.action

        Log.d(TAG, "Call action received: $action for call: $callId")

        when (action) {
            "DECLINE_CALL" -> {
                declineCall(callId)
            }
        }
    }

    private fun declineCall(callId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirebaseFirestore.getInstance()
                    .collection("ActiveCalls")
                    .document(callId)
                    .update("status", "REJECTED")
                    .addOnSuccessListener {
                        Log.d(TAG, "Call declined: $callId")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to decline call: $callId", e)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error declining call: $callId", e)
            }
        }
    }

    companion object {
        private const val TAG = "CallActionReceiver"
    }
}
