package com.nextserve.serveitpartnernew.ui.screen.call

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.tasks.await
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.data.repository.AgoraRepository
import com.nextserve.serveitpartnernew.data.service.CallState
import com.nextserve.serveitpartnernew.data.service.ProviderCallManager
import com.nextserve.serveitpartnernew.ui.theme.ServeitPartnerNewTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen incoming call activity
 * Shows incoming call from user with accept/reject options
 */
class IncomingCallActivity : ComponentActivity() {

    companion object {
        private const val TAG = "IncomingCallActivity"
        private const val EXTRA_CALL_ID = "call_id"
        private const val EXTRA_BOOKING_ID = "booking_id"
        private const val EXTRA_USER_MOBILE = "user_mobile"
        private const val EXTRA_SERVICE_NAME = "service_name"
        private const val EXTRA_INITIATED_BY = "initiated_by"

        fun createIntent(
            context: Context,
            callId: String,
            bookingId: String,
            userMobile: String,
            serviceName: String,
            initiatedBy: String
        ): Intent {
            return Intent(context, IncomingCallActivity::class.java).apply {
                putExtra(EXTRA_CALL_ID, callId)
                putExtra(EXTRA_BOOKING_ID, bookingId)
                putExtra(EXTRA_USER_MOBILE, userMobile)
                putExtra(EXTRA_SERVICE_NAME, serviceName)
                putExtra(EXTRA_INITIATED_BY, initiatedBy)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                       Intent.FLAG_ACTIVITY_CLEAR_TOP or
                       Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        }
    }

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lock orientation and keep screen on
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        enableEdgeToEdge()

        // Extract call data
        val callId = intent.getStringExtra(EXTRA_CALL_ID) ?: ""
        val bookingId = intent.getStringExtra(EXTRA_BOOKING_ID) ?: ""
        val userMobile = intent.getStringExtra(EXTRA_USER_MOBILE) ?: ""
        val serviceName = intent.getStringExtra(EXTRA_SERVICE_NAME) ?: "Service Call"
        val initiatedBy = intent.getStringExtra(EXTRA_INITIATED_BY) ?: "USER"

        Log.d(TAG, "Incoming call: callId=$callId, bookingId=$bookingId, userMobile=$userMobile")

        // Start ringtone and vibration
        startRingtone()
        startVibration()

        setContent {
            ServeitPartnerNewTheme {
                IncomingCallScreen(
                    callId = callId,
                    bookingId = bookingId,
                    userMobile = userMobile,
                    serviceName = serviceName,
                    initiatedBy = initiatedBy,
                    onAccept = { acceptCall(callId, bookingId, userMobile) },
                    onReject = { rejectCall(callId) }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRingtone()
        stopVibration()
        Log.d(TAG, "IncomingCallActivity destroyed")
    }

    private fun startRingtone() {
        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(this, ringtoneUri)
            ringtone?.play()

            // Auto-stop ringtone after 30 seconds as safety measure
            Handler(Looper.getMainLooper()).postDelayed({
                if (ringtone?.isPlaying == true) {
                    Log.w(TAG, "Auto-stopping ringtone after timeout")
                    stopRingtone()
                }
            }, 30000)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting ringtone", e)
        }
    }

    private fun stopRingtone() {
        ringtone?.stop()
        ringtone = null
    }

    private fun startVibration() {
        try {
            // Check if vibration permission is granted
            if (checkSelfPermission(android.Manifest.permission.VIBRATE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "VIBRATE permission not granted, skipping vibration")
                return
            }

            vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
            vibrator?.let {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        it.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 500), 0))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(longArrayOf(0, 1000, 500), 0)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting vibration", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Critical error in startVibration", e)
            // Continue without vibration - don't crash the app
        }
    }

    private fun stopVibration() {
        vibrator?.cancel()
        vibrator = null
    }

    private fun acceptCall(callId: String, bookingId: String, userMobile: String) {
        Log.d(TAG, "Accepting call: $callId")

        lifecycleScope.launch {
            try {
                // Update ActiveCalls status to ACCEPTED
                db.collection("ActiveCalls").document(callId)
                    .update("status", "ACCEPTED")
                    .await()

                Log.d(TAG, "Call accepted, launching ProviderCallActivity")

                // Get call details from ActiveCalls document
                val callDoc = db.collection("ActiveCalls").document(callId).get().await()
                val callData = callDoc.data

                if (callData != null) {
                    val channelName = callData.get("channelName") as? String ?: ""
                    val token = callData.get("token") as? String ?: ""
                    val appId = callData.get("appId") as? String ?: ""

                    // Launch ProviderCallActivity with call details for incoming call
                    val intent = ProviderCallActivity.createIncomingCallIntent(
                        this@IncomingCallActivity,
                        bookingId,
                        channelName,
                        token,
                        appId
                    )
                    startActivity(intent)
                } else {
                    Log.e(TAG, "Failed to get call details for accepted call")
                    finish()
                }

                // Close incoming call screen
                finish()

            } catch (e: Exception) {
                Log.e(TAG, "Error accepting call", e)
                finish() // Close on error
            }
        }
    }

    private fun rejectCall(callId: String) {
        Log.d(TAG, "Rejecting call: $callId")

        lifecycleScope.launch {
            try {
                // Update ActiveCalls status to REJECTED
                db.collection("ActiveCalls").document(callId)
                    .update("status", "REJECTED")
                    .await()

                Log.d(TAG, "Call rejected")
                finish()

            } catch (e: Exception) {
                Log.e(TAG, "Error rejecting call", e)
                finish() // Close on error
            }
        }
    }
}

@Composable
fun IncomingCallScreen(
    callId: String,
    bookingId: String,
    userMobile: String,
    serviceName: String,
    initiatedBy: String,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Call type indicator
            Text(
                text = when (initiatedBy) {
                    "USER" -> "Incoming Call"
                    "PROVIDER" -> "Outgoing Call"
                    else -> "Call"
                },
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Service name
            Text(
                text = serviceName,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // User identifier (masked for privacy)
            Text(
                text = "Customer Call",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Accept/Reject buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reject button (red)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = onReject,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_call_end),
                            contentDescription = "Reject Call",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Reject",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }

                // Accept button (green)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = onAccept,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Green
                        )
                    ) {
                        Text(
                            text = "✓",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Accept",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
