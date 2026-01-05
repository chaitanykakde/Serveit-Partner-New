package com.nextserve.serveitpartnernew

import android.app.Application
import com.google.firebase.FirebaseApp
// import com.google.firebase.appcheck.FirebaseAppCheck
// import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.nextserve.serveitpartnernew.di.AppContainer
import com.nextserve.serveitpartnernew.utils.LanguageManager

class ServeitApp : Application() {

    // Dependency injection container
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize Firebase App Check with debug provider (for development)
        // This prevents "App verification failed" errors during development
        // TODO: Re-enable Firebase App Check when import issues are resolved
        /*
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )
        */

        // Initialize dependency injection container
        container = AppContainer.getInstance(this)

        // Apply saved language before UI loads
        LanguageManager.applySavedLanguage(this)

        // Setup crash reporting (placeholder for Firebase Crashlytics)
        setupCrashReporting()

        // Setup analytics (placeholder for Firebase Analytics)
        setupAnalytics()
    }

    private fun setupCrashReporting() {
        // TODO: Initialize Firebase Crashlytics in production
        // FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        // For now, just set up basic uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Log crash for debugging
            android.util.Log.e("ServeitApp", "Uncaught exception in thread ${thread.name}", throwable)

            // In production, send to crash reporting service
            // FirebaseCrashlytics.getInstance().recordException(throwable)
        }
    }

    private fun setupAnalytics() {
        // TODO: Configure Firebase Analytics settings
        // FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(!BuildConfig.DEBUG)
    }
}

