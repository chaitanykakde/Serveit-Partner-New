package com.nextserve.serveitpartnernew.data.service

import android.content.Context
import android.util.Log
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoEncoderConfiguration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Call states for voice calling
 */
enum class CallState {
    IDLE,
    CONNECTING,
    WAITING_FOR_USER,
    CONNECTED,
    ENDED,
    ERROR
}

/**
 * Singleton manager for Agora voice calling functionality
 * Handles engine initialization, channel operations, and audio controls
 */
class ProviderCallManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "ProviderCallManager"
        private var instance: ProviderCallManager? = null

        fun getInstance(context: Context): ProviderCallManager {
            return instance ?: synchronized(this) {
                instance ?: ProviderCallManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private var rtcEngine: RtcEngine? = null
    private var currentAppId: String = ""
    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _callDuration = MutableStateFlow(0L)
    val callDuration: StateFlow<Long> = _callDuration.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(true)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private var callStartTime: Long = 0L
    private var callDurationTimer: java.util.Timer? = null

    private val rtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            Log.d(TAG, "Successfully joined channel: $channel, uid: $uid")
            _callState.value = CallState.WAITING_FOR_USER
            startCallTimer()
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d(TAG, "User joined call: $uid")
            _callState.value = CallState.CONNECTED
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            Log.d(TAG, "User left call: $uid, reason: $reason")
            _callState.value = CallState.ENDED
            stopCallTimer()
        }

        override fun onError(err: Int) {
            Log.e(TAG, "Agora error: $err")
            _callState.value = CallState.ERROR
            stopCallTimer()
        }

        override fun onLeaveChannel(stats: RtcStats?) {
            Log.d(TAG, "Left channel")
            _callState.value = CallState.IDLE
            stopCallTimer()
        }
    }

    init {
        // Agora engine will be initialized lazily when joining channel
        // to ensure we have the correct App ID from Firebase Functions
    }

    private fun initializeAgoraEngine(appId: String) {
        try {
            // Defensive check
            if (appId.isBlank()) {
                Log.e(TAG, "Cannot initialize Agora engine: App ID is blank")
                _callState.value = CallState.ERROR
                return
            }

            // If engine already exists with same App ID, reuse it
            if (rtcEngine != null && currentAppId == appId) {
                Log.d(TAG, "Agora engine already initialized with correct App ID")
                return
            }

            // Destroy existing engine if App ID changed
            if (rtcEngine != null && currentAppId != appId) {
                Log.d(TAG, "App ID changed, destroying existing engine")
                destroy()
            }

            val config = RtcEngineConfig()
            config.mContext = context
            config.mAppId = appId
            config.mEventHandler = rtcEngineEventHandler

            rtcEngine = RtcEngine.create(config)
            rtcEngine?.apply {
                // Enable audio only
                enableAudio()
                setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
                setAudioProfile(Constants.AUDIO_PROFILE_DEFAULT, Constants.AUDIO_SCENARIO_DEFAULT)

                // Disable video (voice only)
                disableVideo()

                // Set speakerphone as default
                setDefaultAudioRoutetoSpeakerphone(true)
                _isSpeakerOn.value = true
            }

            currentAppId = appId
            Log.d(TAG, "Agora engine initialized successfully with App ID: ${appId.take(8)}...")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Agora engine", e)
            _callState.value = CallState.ERROR
        }
    }

    /**
     * Public method to initialize Agora engine with App ID
     * Called by CallSetupService before joining channel
     */
    fun initializeEngine(appId: String) {
        initializeAgoraEngine(appId)
    }

    /**
     * Join a voice call channel
     * @param channelName Channel name in format "serveit_booking_<bookingId>"
     * @param token Agora token generated by Firebase Functions
     * @param uid Optional user ID (0 for auto-assignment)
     * @param appId Agora App ID from Firebase Functions response
     */
    fun joinChannel(channelName: String, token: String, uid: Int = 0, appId: String) {
        // Initialize Agora engine with the correct App ID
        initializeAgoraEngine(appId)

        if (rtcEngine == null) {
            Log.e(TAG, "Cannot join channel: Agora engine failed to initialize")
            _callState.value = CallState.ERROR
            return
        }

        try {
            Log.d(TAG, "Joining channel: $channelName")
            _callState.value = CallState.CONNECTING

            val options = ChannelMediaOptions()
            options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            options.autoSubscribeAudio = true
            options.autoSubscribeVideo = false

            rtcEngine?.joinChannel(token, channelName, uid, options)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to join channel", e)
            _callState.value = CallState.ERROR
        }
    }

    /**
     * Leave the current voice call
     */
    fun leaveChannel() {
        try {
            Log.d(TAG, "Leaving channel")
            rtcEngine?.leaveChannel()
            _callState.value = CallState.IDLE
            stopCallTimer()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to leave channel", e)
        }
    }

    /**
     * Toggle microphone mute state
     */
    fun toggleMute(): Boolean {
        val newMuteState = !_isMuted.value
        try {
            rtcEngine?.muteLocalAudioStream(newMuteState)
            _isMuted.value = newMuteState
            Log.d(TAG, "Microphone ${if (newMuteState) "muted" else "unmuted"}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle mute", e)
        }
        return newMuteState
    }

    /**
     * Toggle speaker state
     */
    fun toggleSpeaker(): Boolean {
        val newSpeakerState = !_isSpeakerOn.value
        try {
            rtcEngine?.setEnableSpeakerphone(newSpeakerState)
            _isSpeakerOn.value = newSpeakerState
            Log.d(TAG, "Speaker ${if (newSpeakerState) "on" else "off"}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle speaker", e)
        }
        return newSpeakerState
    }

    private fun startCallTimer() {
        callStartTime = System.currentTimeMillis()
        callDurationTimer = java.util.Timer()
        callDurationTimer?.scheduleAtFixedRate(object : java.util.TimerTask() {
            override fun run() {
                val elapsed = (System.currentTimeMillis() - callStartTime) / 1000
                _callDuration.value = elapsed
            }
        }, 0, 1000)
    }

    private fun stopCallTimer() {
        callDurationTimer?.cancel()
        callDurationTimer = null
        _callDuration.value = 0L
    }

    /**
     * Get current call duration in seconds
     */
    fun getCallDuration(): Long {
        return if (callStartTime > 0) {
            (System.currentTimeMillis() - callStartTime) / 1000
        } else {
            0L
        }
    }

    /**
     * Check if currently in a call
     */
    fun isInCall(): Boolean {
        return callState.value in listOf(CallState.CONNECTING, CallState.WAITING_FOR_USER, CallState.CONNECTED)
    }

    /**
     * Clean up resources
     */
    fun destroy() {
        try {
            leaveChannel()
            RtcEngine.destroy()
            rtcEngine = null
            currentAppId = ""
            instance = null
            Log.d(TAG, "ProviderCallManager destroyed")
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying ProviderCallManager", e)
        }
    }
}
