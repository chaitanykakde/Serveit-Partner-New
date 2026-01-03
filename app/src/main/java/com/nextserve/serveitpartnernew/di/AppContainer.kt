package com.nextserve.serveitpartnernew.di

import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.data.repository.*
import com.nextserve.serveitpartnernew.utils.NetworkMonitor

/**
 * Simple dependency injection container
 * In production, consider using Hilt or Dagger for more complex apps
 */
class AppContainer(private val context: Context) {

    // Firebase instances
    val firestore: FirebaseFirestore by lazy { FirebaseProvider.firestore }
    val functions: FirebaseFunctions by lazy { FirebaseFunctions.getInstance() }

    // Repositories
    val firestoreRepository: FirestoreRepository by lazy {
        FirestoreRepository(firestore)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(FirebaseProvider.auth)
    }

    val jobsRepository: JobsRepository by lazy {
        JobsRepository(firestore, functions, context)
    }

    val earningsRepository: EarningsRepository by lazy {
        EarningsRepository(firestore)
    }

    val storageRepository: StorageRepository by lazy {
        StorageRepository(FirebaseProvider.storage, context)
    }

    val locationRepository: LocationRepository by lazy {
        LocationRepository(context, LocationServices.getFusedLocationProviderClient(context))
    }

    val jobManagementRepository: JobManagementRepository by lazy {
        JobManagementRepository(firestore, FirebaseProvider.storage)
    }

    val notificationsRepository: NotificationsRepository by lazy {
        NotificationsRepository(firestore)
    }

    val routeOptimizationRepository: RouteOptimizationRepository by lazy {
        RouteOptimizationRepository(locationRepository)
    }

    // Utilities
    val networkMonitor: NetworkMonitor by lazy {
        NetworkMonitor(context)
    }

    // Singleton instance
    companion object {
        @Volatile
        private var instance: AppContainer? = null

        fun getInstance(context: Context): AppContainer {
            return instance ?: synchronized(this) {
                instance ?: AppContainer(context.applicationContext).also { instance = it }
            }
        }
    }
}
