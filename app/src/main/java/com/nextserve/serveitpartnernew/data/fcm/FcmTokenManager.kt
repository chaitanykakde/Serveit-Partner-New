package com.nextserve.serveitpartnernew.data.fcm

import android.content.Context
import android.provider.Settings
import com.google.firebase.firestore.FieldValue
import com.google.firebase.messaging.FirebaseMessaging
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

object FcmTokenManager {

    private const val PREFS_NAME = "fcm_prefs"
    private const val KEY_CURRENT_TOKEN = "current_token"
    private const val KEY_DEVICE_ID = "device_id"

    /**
     * Get FCM token and save to Firestore ONLY after successful login.
     * This prevents race conditions and ensures tokens are saved with proper user context.
     */
    suspend fun getAndSaveToken(uid: String): Result<String> {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            saveTokenToFirestore(uid, token, getDeviceId())
            Result.success(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Save FCM token to Firestore with proper multi-device support.
     * Uses device ID to allow multiple devices per user.
     */
    private suspend fun saveTokenToFirestore(uid: String, token: String, deviceId: String) {
        try {
            val partnerRef = FirebaseProvider.firestore
                .collection("partners")
                .document(uid)

            // Save token with device-specific information
            val tokenData = mapOf(
                "token" to token,
                "deviceId" to deviceId,
                "platform" to "android",
                "updatedAt" to FieldValue.serverTimestamp(),
                "deviceModel" to android.os.Build.MODEL,
                "appVersion" to getAppVersion()
            )

            // Use deviceId as document ID for easy management
            partnerRef.collection("fcmTokens")
                .document(deviceId)
                .set(tokenData)
                .await()

            // Update the main document with the latest token (for backward compatibility)
            partnerRef.update(mapOf(
                "fcmToken" to token,
                "fcmTokenUpdatedAt" to FieldValue.serverTimestamp()
            )).await()

            // Store locally for comparison
            storeCurrentTokenLocally(token, deviceId)

        } catch (e: Exception) {
            android.util.Log.e("FcmTokenManager", "Failed to save FCM token", e)
            throw e
        }
    }

    /**
     * Refresh FCM token when Firebase provides a new one.
     * Only updates if the user is currently logged in.
     */
    fun refreshToken(uid: String) {
        if (uid.isEmpty()) return

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val newToken = task.result
                if (newToken != null) {
                    val currentToken = getStoredToken()
                    val deviceId = getStoredDeviceId()

                    // Only update if token has actually changed
                    if (newToken != currentToken && deviceId.isNotEmpty()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                saveTokenToFirestore(uid, newToken, deviceId)
                            } catch (e: Exception) {
                                android.util.Log.e("FcmTokenManager", "Failed to refresh FCM token", e)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Remove FCM token for this device when user logs out.
     */
    suspend fun removeToken(uid: String) {
        try {
            val deviceId = getStoredDeviceId()
            if (deviceId.isNotEmpty()) {
                FirebaseProvider.firestore
                    .collection("partners")
                    .document(uid)
                    .collection("fcmTokens")
                    .document(deviceId)
                    .delete()
                    .await()
            }

            // Clear local storage
            clearLocalTokenData()

        } catch (e: Exception) {
            android.util.Log.e("FcmTokenManager", "Failed to remove FCM token", e)
        }
    }

    /**
     * Get a unique device ID for this installation.
     */
    private fun getDeviceId(): String {
        // Try to get stored device ID first
        val storedId = getStoredDeviceId()
        if (storedId.isNotEmpty()) {
            return storedId
        }

        // Generate new device ID
        val newDeviceId = UUID.randomUUID().toString()
        storeDeviceId(newDeviceId)
        return newDeviceId
    }

    /**
     * Get app version for tracking.
     */
    private fun getAppVersion(): String {
        return try {
            val context = FirebaseProvider.firestore.app.applicationContext
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.longVersionCode})"
        } catch (e: Exception) {
            "unknown"
        }
    }

    // Local storage helpers
    private fun getContext() = FirebaseProvider.firestore.app.applicationContext

    private fun getStoredToken(): String? {
        return getContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .getString(KEY_CURRENT_TOKEN, null)
    }

    private fun getStoredDeviceId(): String {
        return getContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .getString(KEY_DEVICE_ID, "") ?: ""
    }

    private fun storeCurrentTokenLocally(token: String, deviceId: String) {
        getContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CURRENT_TOKEN, token)
            .putString(KEY_DEVICE_ID, deviceId)
            .apply()
    }

    private fun storeDeviceId(deviceId: String) {
        getContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DEVICE_ID, deviceId)
            .apply()
    }

    private fun clearLocalTokenData() {
        getContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}

