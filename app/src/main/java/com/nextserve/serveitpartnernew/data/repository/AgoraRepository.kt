package com.nextserve.serveitpartnernew.data.repository

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Agora voice calling operations using Firebase Cloud Functions
 */
@Singleton
class AgoraRepository @Inject constructor(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
) {

    companion object {
        private const val TAG = "AgoraRepository"
    }

    /**
     * Generate Agora token for voice calling
     * @param bookingId The booking ID for the call
     * @return Result containing the token or error
     */
    suspend fun generateAgoraToken(bookingId: String, userMobile: String): Result<AgoraTokenResponse> {
        return try {
            Log.d(TAG, "Generating Agora token for booking: $bookingId, userMobile: $userMobile")

            val data = hashMapOf(
                "bookingId" to bookingId,
                "userMobile" to userMobile,
                "callType" to "voice" // Specify voice call
            )

            val httpsCallable = functions.getHttpsCallable("generateAgoraToken")
            val httpsCallableResult = httpsCallable.call(data).await()

            // Access the private data field using reflection
            val dataField = httpsCallableResult.javaClass.getDeclaredField("data")
            dataField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val responseData = dataField.get(httpsCallableResult) as? Map<String, Any>
                ?: return Result.failure(Exception("Invalid response format"))

            val token = responseData["token"] as? String
                ?: return Result.failure(Exception("Token not found in response"))

            val channelName = responseData["channelName"] as? String
                ?: return Result.failure(Exception("Channel name not found in response"))

            val uid = (responseData["uid"] as? Number)?.toInt() ?: 0

            val appId = responseData["appId"] as? String
                ?: return Result.failure(Exception("App ID not found in response"))

            val tokenResponse = AgoraTokenResponse(
                token = token,
                channelName = channelName,
                uid = uid,
                appId = appId
            )

            Log.d(TAG, "Successfully generated Agora token for channel: $channelName")
            Result.success(tokenResponse)

        } catch (e: FirebaseFunctionsException) {
            Log.e(TAG, "Firebase Functions error: ${e.code}", e)
            when (e.code) {
                FirebaseFunctionsException.Code.UNAUTHENTICATED ->
                    Result.failure(Exception("Authentication required"))
                FirebaseFunctionsException.Code.NOT_FOUND ->
                    Result.failure(Exception("Booking not found"))
                FirebaseFunctionsException.Code.FAILED_PRECONDITION ->
                    Result.failure(Exception("Invalid booking status for calling"))
                else ->
                    Result.failure(Exception("Failed to generate call token: ${e.message}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating Agora token", e)
            Result.failure(Exception("Failed to generate call token: ${e.message}"))
        }
    }

    /**
     * End a call and log call details
     * @param bookingId The booking ID
     * @param callDuration Call duration in seconds
     * @return Result indicating success or failure
     */
    suspend fun endCall(bookingId: String, callDuration: Long): Result<Unit> {
        return try {
            Log.d(TAG, "Ending call for booking: $bookingId, duration: ${callDuration}s")

            val data = hashMapOf(
                "bookingId" to bookingId,
                "callDuration" to callDuration,
                "callType" to "voice"
            )

            functions
                .getHttpsCallable("endCall")
                .call(data)
                .await()

            Log.d(TAG, "Successfully ended call for booking: $bookingId")
            Result.success(Unit)

        } catch (e: FirebaseFunctionsException) {
            Log.e(TAG, "Firebase Functions error ending call: ${e.code}", e)
            when (e.code) {
                FirebaseFunctionsException.Code.UNAUTHENTICATED ->
                    Result.failure(Exception("Authentication required"))
                FirebaseFunctionsException.Code.NOT_FOUND ->
                    Result.failure(Exception("Booking not found"))
                else ->
                    Result.failure(Exception("Failed to end call: ${e.message}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error ending call", e)
            Result.failure(Exception("Failed to end call: ${e.message}"))
        }
    }

    /**
     * Validate if a booking allows calling
     * @param bookingId The booking ID to validate
     * @return Result indicating if calling is allowed
     */
    suspend fun validateCallPermission(
        bookingId: String,
        userMobile: String,
        callerRole: String
    ): Result<CallPermissionResponse> {
        return try {
            Log.d(TAG, "Validating call permission for booking: $bookingId, user: $userMobile, role: $callerRole")

            val data = hashMapOf(
                "bookingId" to bookingId,
                "userMobile" to userMobile,
                "callerRole" to callerRole
            )

            Log.d(TAG, "📤 Calling validateCallPermission with payload: $data")

            val httpsCallable = functions.getHttpsCallable("validateCallPermission")
            val httpsCallableResult = httpsCallable.call(data).await()

            // Access the private data field using reflection
            val dataField = httpsCallableResult.javaClass.getDeclaredField("data")
            dataField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val responseData = dataField.get(httpsCallableResult) as? Map<String, Any>
                ?: return Result.failure(Exception("Invalid response format"))

            val allowed = responseData["allowed"] as? Boolean ?: false
            val bookingStatus = responseData["bookingStatus"] as? String ?: ""
            val customerName = responseData["customerName"] as? String ?: ""
            val serviceName = responseData["serviceName"] as? String ?: ""

            val permissionResponse = CallPermissionResponse(
                allowed = allowed,
                bookingStatus = bookingStatus,
                customerName = customerName,
                serviceName = serviceName
            )

            Log.d(TAG, "Call permission validated for booking: $bookingId, allowed: $allowed")
            Result.success(permissionResponse)

        } catch (e: FirebaseFunctionsException) {
            Log.e(TAG, "Firebase Functions error validating permission: ${e.code}", e)
            Result.failure(Exception("Failed to validate call permission: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "Error validating call permission", e)
            Result.failure(Exception("Failed to validate call permission: ${e.message}"))
        }
    }
}

/**
 * Response model for Agora token generation
 */
data class AgoraTokenResponse(
    val token: String,
    val channelName: String,
    val uid: Int,
    val appId: String
)

/**
 * Response model for call permission validation
 */
data class CallPermissionResponse(
    val allowed: Boolean,
    val bookingStatus: String,
    val customerName: String,
    val serviceName: String
)
