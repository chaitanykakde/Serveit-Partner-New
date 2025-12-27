package com.nextserve.serveitpartnernew

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.nextserve.serveitpartnernew.data.fcm.FcmTokenManager
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.data.repository.FirestoreRepository
import com.nextserve.serveitpartnernew.ui.navigation.Screen
import com.nextserve.serveitpartnernew.ui.navigation.appNavGraph
import com.nextserve.serveitpartnernew.ui.theme.ServeitPartnerNewTheme
import com.nextserve.serveitpartnernew.ui.utils.rememberNotificationPermissionState
import com.nextserve.serveitpartnernew.utils.LanguageManager
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Apply saved language on app start
        LanguageManager.applySavedLanguage(this)
        
        // Save FCM token on app start/resume
        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_CREATE) {
                    val currentUser = FirebaseProvider.auth.currentUser
                    if (currentUser != null) {
                        FcmTokenManager.refreshToken(currentUser.uid)
                    }
                }
            }
        })
        
        setContent {
            ServeitPartnerNewTheme {
                val navController = rememberNavController()
                var startDestination by remember { mutableStateOf<String?>(null) }
                
                // Request notification permission on app start
                val (hasNotificationPermission, requestNotificationPermission) = 
                    rememberNotificationPermissionState()
                
                // Request notification permission when app opens (Android 13+)
                LaunchedEffect(Unit) {
                    if (!hasNotificationPermission) {
                        requestNotificationPermission()
                    }
                }
                
                // Check auth state and route accordingly
                LaunchedEffect(Unit) {
                    val currentUser = FirebaseProvider.auth.currentUser
                    if (currentUser != null) {
                        // Save FCM token
                        FcmTokenManager.getAndSaveToken(currentUser.uid)
                        
                        // User is logged in, check onboarding status
                        val firestoreRepository = FirestoreRepository(FirebaseProvider.firestore)
                        val result = firestoreRepository.checkOnboardingStatus(currentUser.uid)
                        result.onSuccess { providerData ->
                            startDestination = when {
                                providerData == null -> Screen.Onboarding.route
                                providerData.onboardingStatus == "SUBMITTED" && providerData.approvalStatus == "PENDING" -> Screen.Waiting.route
                                providerData.approvalStatus == "REJECTED" -> Screen.Rejection.createRoute(currentUser.uid)
                                providerData.approvalStatus == "APPROVED" -> Screen.Home.route
                                else -> Screen.Onboarding.route
                            }
                        }.onFailure {
                            startDestination = Screen.Onboarding.route
                        }
                    } else {
                        startDestination = Screen.Login.route
                    }
                }
                
                if (startDestination != null) {
                    NavHost(
                        navController = navController,
                        startDestination = startDestination!!,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        appNavGraph(navController = navController)
                    }
                }
            }
        }
    }
}