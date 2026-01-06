package com.nextserve.serveitpartnernew.ui.screen.call

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.data.repository.AgoraRepository
import com.nextserve.serveitpartnernew.data.service.CallState
import com.nextserve.serveitpartnernew.ui.theme.ServeitPartnerNewTheme
import com.nextserve.serveitpartnernew.ui.viewmodel.ProviderCallViewModel
import kotlin.time.Duration.Companion.seconds

/**
 * Full-screen voice call activity
 */
class ProviderCallActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_BOOKING_ID = "booking_id"
        private const val EXTRA_CHANNEL_NAME = "channel_name"
        private const val EXTRA_TOKEN = "token"
        private const val EXTRA_APP_ID = "app_id"
        private const val EXTRA_IS_INCOMING = "is_incoming"

        fun createIntent(context: Context, bookingId: String): Intent {
            return Intent(context, ProviderCallActivity::class.java).apply {
                putExtra(EXTRA_BOOKING_ID, bookingId)
                putExtra(EXTRA_IS_INCOMING, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }

        fun createIncomingCallIntent(
            context: Context,
            bookingId: String,
            channelName: String,
            token: String,
            appId: String
        ): Intent {
            return Intent(context, ProviderCallActivity::class.java).apply {
                putExtra(EXTRA_BOOKING_ID, bookingId)
                putExtra(EXTRA_CHANNEL_NAME, channelName)
                putExtra(EXTRA_TOKEN, token)
                putExtra(EXTRA_APP_ID, appId)
                putExtra(EXTRA_IS_INCOMING, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lock orientation and keep screen on
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        enableEdgeToEdge()

        val bookingId = intent.getStringExtra(EXTRA_BOOKING_ID) ?: ""
        val isIncoming = intent.getBooleanExtra(EXTRA_IS_INCOMING, false)

        setContent {
            ServeitPartnerNewTheme {
                ProviderCallScreen(
                    bookingId = bookingId,
                    isIncoming = isIncoming,
                    channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: "",
                    token = intent.getStringExtra(EXTRA_TOKEN) ?: "",
                    appId = intent.getStringExtra(EXTRA_APP_ID) ?: "",
                    onCallEnded = { finish() }
                )
            }
        }
    }

    override fun onBackPressed() {
        // Get the current call state from the ViewModel
        val viewModel = (application as? android.app.Application)?.let { app ->
            // We can't easily access the ViewModel from here, so check via intent extra or other means
            // For now, prevent back press during calls and let the UI handle it
        }

        // For safety, always prevent back press during calls
        // The user should use the End Call button to exit
        // Only allow back press if there's a clear error state
        val bookingId = intent.getStringExtra(EXTRA_BOOKING_ID)
        if (bookingId.isNullOrEmpty()) {
            // No valid booking, allow back press
            super.onBackPressed()
        }
        // Otherwise, block back press - user should end call properly
    }
}

@Composable
fun ProviderCallScreen(
    bookingId: String,
    isIncoming: Boolean = false,
    channelName: String = "",
    token: String = "",
    appId: String = "",
    onCallEnded: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        ProviderCallViewModel(context = context)
    }
    val uiState by viewModel.uiState.collectAsState()

    // Permission state
    var hasRequestedPermission by remember { mutableStateOf(false) }

    // Permission launcher for RECORD_AUDIO
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.d("ProviderCallActivity", "✅ MICROPHONE PERMISSION GRANTED")
            // Permission granted, now start the call
            // Don't finish activity here - let the call initialization handle it
            if (bookingId.isNotEmpty()) {
                viewModel.initializeCall(bookingId)
            }
        } else {
            android.util.Log.w("ProviderCallActivity", "❌ MICROPHONE PERMISSION DENIED")
            // Permission denied - show error message
            // Don't finish activity immediately, let user retry
        }
    }

    // Handle call initialization (outgoing) or incoming call handling
    LaunchedEffect(bookingId, isIncoming, hasRequestedPermission) {
        if (bookingId.isNotEmpty()) {
            if (isIncoming) {
                // Incoming call - directly handle it (permissions should already be granted)
                android.util.Log.d("ProviderCallActivity", "📞 HANDLING INCOMING CALL for booking: $bookingId")
                viewModel.handleIncomingCall(bookingId, channelName, token, appId)
            } else {
                // Outgoing call - check permissions first
                if (!hasRequestedPermission) {
                    android.util.Log.d("ProviderCallActivity", "🔄 CHECKING MICROPHONE PERMISSION for booking: $bookingId")

                    // Check if running on emulator
                    val isEmulator = isRunningOnEmulator()
                    if (isEmulator) {
                        android.util.Log.w("ProviderCallActivity", "🤖 RUNNING ON EMULATOR - Microphone may not be fully functional")
                    }

                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        android.util.Log.d("ProviderCallActivity", "✅ MICROPHONE PERMISSION ALREADY GRANTED")
                        // Permission already granted, start the call
                        viewModel.initializeCall(bookingId)
                    } else {
                        android.util.Log.d("ProviderCallActivity", "📞 REQUESTING MICROPHONE PERMISSION")
                        // Request permission
                        hasRequestedPermission = true
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            }
        }
    }

    // Handle call end - only trigger when call is truly finished
    // Add guard to prevent premature activity finish during initialization
    var callInitializationStarted by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.callState, callInitializationStarted) {
        when (uiState.callState) {
            CallState.ENDED -> {
                // Call ended normally
                android.util.Log.d("ProviderCallActivity", "Call ended, finishing activity")
                onCallEnded()
            }
            CallState.IDLE -> {
                // Only end activity if call initialization has started and we're not loading
                // This prevents premature finish during permission checks
                if (callInitializationStarted && !uiState.isLoading && uiState.errorMessage == null) {
                    android.util.Log.d("ProviderCallActivity", "Call idle after initialization, finishing activity")
                    onCallEnded()
                } else if (!callInitializationStarted && !uiState.isLoading) {
                    // Initial idle state before any call setup - don't finish
                    android.util.Log.d("ProviderCallActivity", "Initial idle state, waiting for call initialization")
                }
            }
            CallState.ERROR -> {
                // Call failed with error - only finish if we have an error message
                if (uiState.errorMessage != null && callInitializationStarted) {
                    android.util.Log.d("ProviderCallActivity", "Call error after initialization, finishing activity: ${uiState.errorMessage}")
                    onCallEnded()
                }
            }
            CallState.CONNECTING, CallState.WAITING_FOR_USER -> {
                // Mark that call initialization has started
                callInitializationStarted = true
                android.util.Log.d("ProviderCallActivity", "Call initialization started, state: ${uiState.callState}")
            }
            else -> {
                // Keep activity alive during other states
                if (!callInitializationStarted && (uiState.callState != CallState.IDLE)) {
                    callInitializationStarted = true
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Main call content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section - Customer and service info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Customer avatar placeholder
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.customerName.firstOrNull()?.toString() ?: "?",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Customer name
                Text(
                    text = uiState.customerName.ifEmpty { "Customer" },
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                // Service name
                Text(
                    text = uiState.serviceName.ifEmpty { "Service Call" },
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                // Call status
                Text(
                    text = getCallStatusText(uiState.callState),
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                // Call duration
                if (uiState.callState == CallState.CONNECTED && uiState.callDuration > 0) {
                    Text(
                        text = formatDuration(uiState.callDuration),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bottom section - Call controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute button
                CallControlButton(
                    icon = if (uiState.isMuted) R.drawable.ic_mic_off else R.drawable.ic_mic,
                    contentDescription = if (uiState.isMuted) "Unmute" else "Mute",
                    isActive = uiState.isMuted,
                    onClick = { viewModel.toggleMute() }
                )

                // Speaker button
                CallControlButton(
                    icon = if (uiState.isSpeakerOn) R.drawable.ic_volume_up else R.drawable.ic_volume_down,
                    contentDescription = if (uiState.isSpeakerOn) "Speaker off" else "Speaker on",
                    isActive = uiState.isSpeakerOn,
                    onClick = { viewModel.toggleSpeaker() }
                )

                // End call button
                CallControlButton(
                    icon = R.drawable.ic_call_end,
                    contentDescription = "End call",
                    backgroundColor = Color.Red,
                    onClick = { viewModel.endCall() }
                )
            }
        }

        // Loading overlay
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        // Error message with retry option
        uiState.errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    // Show retry button for permission errors
                    if (error.contains("permission", ignoreCase = true) ||
                        error.contains("microphone", ignoreCase = true)) {
                        TextButton(
                            onClick = {
                                // Reset error and try again
                                viewModel.resetError()
                                hasRequestedPermission = false
                                // Trigger permission check again
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    viewModel.initializeCall(bookingId)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        ) {
                            Text("RETRY", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            ) {
                Text(text = error)
            }
        }
    }
}

@Composable
private fun CallControlButton(
    icon: Int,
    contentDescription: String,
    isActive: Boolean = false,
    backgroundColor: Color = Color.White.copy(alpha = 0.2f),
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(backgroundColor)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = contentDescription,
            tint = if (isActive) Color.Yellow else Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

private fun getCallStatusText(callState: CallState): String {
    return when (callState) {
        CallState.IDLE -> "Call ended"
        CallState.CONNECTING -> "Connecting..."
        CallState.WAITING_FOR_USER -> "Waiting for customer..."
        CallState.CONNECTED -> "Connected"
        CallState.ENDED -> "Call ended"
        CallState.ERROR -> "Call error"
    }
}

private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

/**
 * Check if the app is running on an Android emulator
 */
private fun isRunningOnEmulator(): Boolean {
    return (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
            || android.os.Build.FINGERPRINT.startsWith("generic")
            || android.os.Build.FINGERPRINT.startsWith("unknown")
            || android.os.Build.HARDWARE.contains("goldfish")
            || android.os.Build.HARDWARE.contains("ranchu")
            || android.os.Build.MODEL.contains("google_sdk")
            || android.os.Build.MODEL.contains("Emulator")
            || android.os.Build.MODEL.contains("Android SDK built for x86")
            || android.os.Build.MANUFACTURER.contains("Genymotion")
            || (android.os.Build.BRAND.startsWith("generic") && android.os.Build.PRODUCT.startsWith("sdk"))
            || android.os.Build.PRODUCT == "google_sdk"
            || android.os.Build.PRODUCT.contains("sdk")
}
