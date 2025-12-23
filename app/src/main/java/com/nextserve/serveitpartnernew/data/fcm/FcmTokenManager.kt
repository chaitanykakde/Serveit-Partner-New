package com.nextserve.serveitpartnernew.data.fcm

import com.google.firebase.messaging.FirebaseMessaging
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object FcmTokenManager {
    
    suspend fun getAndSaveToken(uid: String): Result<String> {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            saveTokenToFirestore(uid, token)
            Result.success(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun saveTokenToFirestore(uid: String, token: String) {
        try {
            val tokensRef = FirebaseProvider.firestore
                .collection("providers")
                .document(uid)
                .collection("fcmTokens")
                .document(token)
            
            tokensRef.set(mapOf(
                "token" to token,
                "createdAt" to com.google.firebase.Timestamp.now(),
                "deviceInfo" to android.os.Build.MODEL
            )).await()
            
            // Also update the main provider document with latest token
            FirebaseProvider.firestore
                .collection("providers")
                .document(uid)
                .update("fcmToken", token)
                .await()
        } catch (e: Exception) {
            // Log error but don't fail
            android.util.Log.e("FcmTokenManager", "Failed to save token", e)
        }
    }
    
    fun refreshToken(uid: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                if (token != null && uid.isNotEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        saveTokenToFirestore(uid, token)
                    }
                }
            }
        }
    }
}

