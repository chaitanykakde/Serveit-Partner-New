package com.nextserve.serveitpartnernew.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Utility class for handling runtime permissions
 */
object PermissionUtils {

    /**
     * Check if microphone permission is granted
     */
    fun hasMicrophonePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if all audio call permissions are granted
     */
    fun hasAudioCallPermissions(context: Context): Boolean {
        return hasMicrophonePermission(context)
    }

    /**
     * Get list of required permissions for audio calls
     */
    fun getAudioCallPermissions(): Array<String> {
        return arrayOf(Manifest.permission.RECORD_AUDIO)
    }
}
