package com.nextserve.serveitpartnernew.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextserve.serveitpartnernew.data.service.CallSetupService
import com.nextserve.serveitpartnernew.data.service.CallSetupState
import com.nextserve.serveitpartnernew.data.service.CallState
import com.nextserve.serveitpartnernew.data.service.ProviderCallManager
import com.nextserve.serveitpartnernew.utils.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * UI state for the call screen
 */
data class CallUiState(
    val callState: CallState = CallState.IDLE,
    val customerName: String = "",
    val serviceName: String = "",
    val bookingId: String = "",
    val callDuration: Long = 0L,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val errorMessage: String? = null,
    val isLoading: Boolean = false
)

/**
 * ViewModel for managing voice call functionality
 * Now only observes call setup state, doesn't execute network operations
 */
class ProviderCallViewModel(
    private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "ProviderCallViewModel"
    }

    private val callManager = ProviderCallManager.getInstance(context)

    private val _uiState = MutableStateFlow(CallUiState())
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    private var currentBookingId: String = ""
    private var currentToken: String = ""
    private var tokenExpiryTime: Long = 0L

    // One-time execution guard to prevent duplicate call initialization
    private var callInitStarted = false

    init {
        // Observe call manager state changes
        viewModelScope.launch {
            callManager.callState.collectLatest { callState ->
                _uiState.value = _uiState.value.copy(callState = callState)
            }
        }

        viewModelScope.launch {
            callManager.callDuration.collectLatest { duration ->
                _uiState.value = _uiState.value.copy(callDuration = duration)
            }
        }

        viewModelScope.launch {
            callManager.isMuted.collectLatest { isMuted ->
                _uiState.value = _uiState.value.copy(isMuted = isMuted)
            }
        }

        viewModelScope.launch {
            callManager.isSpeakerOn.collectLatest { isSpeakerOn ->
                _uiState.value = _uiState.value.copy(isSpeakerOn = isSpeakerOn)
            }
        }

        // Observe call setup service state
        viewModelScope.launch {
            val service = CallSetupService.getInstance()
            service?.setupState?.collectLatest { setupState ->
                handleSetupStateChange(setupState)
            }
        }
    }

    /**
     * Handle incoming call that was already accepted
     * Directly joins the channel without setup process
     */
    fun handleIncomingCall(bookingId: String, channelName: String, token: String, appId: String) {
        Log.d(TAG, "🎯 Handling incoming call for booking: $bookingId, channel: $channelName")

        currentBookingId = bookingId
        _uiState.value = _uiState.value.copy(
            bookingId = bookingId,
            callState = CallState.CONNECTING,
            isLoading = false,
            errorMessage = null
        )

        // Join channel directly with provided token
        callManager.joinChannel(channelName, token, appId = appId)
    }

    /**
     * Initialize call for a specific booking
     * Now delegates to CallSetupService instead of doing network operations directly
     */
    fun initializeCall(bookingId: String) {
        // One-time execution guard - prevent duplicate call initialization
        if (callInitStarted) {
            Log.w(TAG, "Call initialization already started for booking: $bookingId, ignoring duplicate call")
            return
        }

        callInitStarted = true
        Log.d(TAG, "🔄 CALL INIT STARTED - Starting call setup service for booking: $bookingId")

        currentBookingId = bookingId
        _uiState.value = _uiState.value.copy(
            bookingId = bookingId,
            isLoading = true,
            errorMessage = null
        )

        // Start the foreground service to handle call setup
        CallSetupService.startCallSetup(context, bookingId)
    }

    /**
     * Handle state changes from CallSetupService
     */
    private fun handleSetupStateChange(state: CallSetupState) {
        when (state) {
            is CallSetupState.Idle -> {
                // Reset loading state
                _uiState.value = _uiState.value.copy(isLoading = false)
            }

            is CallSetupState.ValidatingPermission -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            is CallSetupState.PermissionGranted -> {
                _uiState.value = _uiState.value.copy(
                    customerName = state.customerName,
                    serviceName = state.serviceName,
                    isLoading = true, // Still loading, token generation next
                    errorMessage = null
                )
            }

            is CallSetupState.PermissionDenied -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = state.reason,
                    isLoading = false
                )
                callInitStarted = false // Allow retry
            }

            is CallSetupState.GeneratingToken -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            is CallSetupState.TokenGenerated -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = true, // Still initializing engine
                    errorMessage = null
                )
            }

            is CallSetupState.InitializingEngine -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            is CallSetupState.Ready -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
                Log.d(TAG, "✅ CALL SETUP COMPLETE - Ready for voice call")
            }

            is CallSetupState.Error -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = state.message,
                    isLoading = false
                )
                callInitStarted = false // Allow retry
                Log.e(TAG, "❌ CALL SETUP FAILED - ${state.message}")
            }
        }
    }


    /**
     * Check if microphone permission is granted
     */
    fun hasMicrophonePermission(): Boolean {
        return PermissionUtils.hasMicrophonePermission(context)
    }

    /**
     * Set error message in UI state
     */
    fun setErrorMessage(message: String?) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }

    /**
     * End the current call
     */
    fun endCall() {
        val callDuration = callManager.getCallDuration()

        // Log call end through service if available
        val service = CallSetupService.getInstance()
        if (service != null) {
            // Service handles the logging in its own scope
            service.endCallAndLog(callDuration, currentBookingId.takeIf { it.isNotEmpty() })
        } else {
            // Fallback if service not available
            Log.d(TAG, "Call ended, duration: ${callDuration}s")
        }

        // Stop call setup service
        CallSetupService.stopCallSetup(context)

        // Leave the channel
        callManager.leaveChannel()

        // Reset state
        _uiState.value = CallUiState()
        currentBookingId = ""
        callInitStarted = false
    }

    /**
     * Toggle microphone mute
     */
    fun toggleMute() {
        val newMuteState = callManager.toggleMute()
        Log.d(TAG, "Microphone ${if (newMuteState) "muted" else "unmuted"}")
    }

    /**
     * Toggle speaker
     */
    fun toggleSpeaker() {
        val newSpeakerState = callManager.toggleSpeaker()
        Log.d(TAG, "Speaker ${if (newSpeakerState) "enabled" else "disabled"}")
    }

    /**
     * Check if token needs refresh (expires in < 2 minutes)
     */
    private fun shouldRefreshToken(): Boolean {
        val timeUntilExpiry = tokenExpiryTime - System.currentTimeMillis()
        return timeUntilExpiry < (2 * 60 * 1000) // Less than 2 minutes
    }

    /**
     * Check if currently in a call
     */
    fun isInCall(): Boolean = callManager.isInCall()

    /**
     * Handle back press - end call if in call
     */
    fun onBackPressed(): Boolean {
        if (isInCall()) {
            endCall()
            return true // Handled
        }
        return false // Not handled
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "🗑️ VIEWMODEL CLEARED - ProviderCallViewModel cleared for booking: $currentBookingId, callInitStarted: $callInitStarted")

        // End call if still active when ViewModel is cleared
        if (isInCall()) {
            Log.w(TAG, "⚠️ Ending active call due to ViewModel clearance")
            endCall()
        }

        // Reset the init guard for future use
        callInitStarted = false
    }
}
