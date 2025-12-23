package com.nextserve.serveitpartnernew.ui.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat

@Composable
fun rememberLocationPermissionState(): Pair<Boolean, () -> Unit> {
    val context = androidx.compose.ui.platform.LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    val requestPermission: () -> Unit = {
        launcher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    return Pair(hasPermission, requestPermission)
}

@Composable
fun rememberNotificationPermissionState(): Pair<Boolean, () -> Unit> {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // For Android 12 and below, notifications are always allowed
    val isAndroid13Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    
    var hasPermission by remember {
        mutableStateOf(
            if (isAndroid13Plus) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true // Pre-Android 13, notifications are always allowed
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    val requestPermission: () -> Unit = {
        if (isAndroid13Plus) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // For Android 12 and below, no need to request
    }

    return Pair(hasPermission, requestPermission)
}

