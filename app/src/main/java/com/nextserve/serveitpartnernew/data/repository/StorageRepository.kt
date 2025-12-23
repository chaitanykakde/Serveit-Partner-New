package com.nextserve.serveitpartnernew.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File

class StorageRepository(
    private val storage: FirebaseStorage,
    private val context: Context
) {
    suspend fun uploadAadhaarDocument(
        uid: String,
        documentType: String, // "front" or "back"
        imageUri: Uri,
        onProgress: (Double) -> Unit
    ): Result<String> {
        return try {
            // Verify UID is not empty
            if (uid.isEmpty()) {
                return Result.failure(Exception("User not authenticated. Please login again."))
            }
            
            val compressedBitmap = compressImage(imageUri)
            val byteArray = bitmapToByteArray(compressedBitmap)
            
            val path = "providers/$uid/documents/aadhaar_$documentType.jpg"
            val storageRef = storage.reference.child(path)
            
            // Set metadata for better security
            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()
            
            val uploadTask = storageRef.putBytes(byteArray, metadata)
            
            // Monitor progress
            uploadTask.addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
                onProgress(progress)
            }
            
            val result = uploadTask.await()
            val downloadUrl = result.storage.downloadUrl.await()
            
            Result.success(downloadUrl.toString())
        } catch (e: com.google.firebase.storage.StorageException) {
            val errorMessage = when {
                e.errorCode == -13021 || e.message?.contains("403") == true || e.message?.contains("Permission denied") == true || e.message?.contains("does not have permission") == true -> {
                    "Storage permission denied. Please configure Firebase Storage rules in Firebase Console."
                }
                e.errorCode == -13020 || e.message?.contains("401") == true || e.message?.contains("Not authenticated") == true -> {
                    "Not authenticated. Please login again."
                }
                else -> {
                    "Upload failed: ${e.message ?: "Unknown error"}"
                }
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            val errorMsg = when {
                e.message?.contains("403") == true || e.message?.contains("Permission denied") == true || e.message?.contains("does not have permission") == true -> {
                    "Storage permission denied. Please configure Firebase Storage rules in Firebase Console."
                }
                else -> {
                    e.message ?: "Upload failed. Please try again."
                }
            }
            Result.failure(Exception(errorMsg))
        }
    }

    private suspend fun compressImage(uri: Uri): Bitmap {
        // Load image
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Cannot open image file")
        
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
            ?: throw Exception("Cannot decode image")
        
        inputStream.close()

        // Resize if too large (max 1920x1920)
        val maxDimension = 1920
        val width = originalBitmap.width
        val height = originalBitmap.height
        
        val scaledBitmap = if (width > maxDimension || height > maxDimension) {
            val scale = minOf(
                maxDimension.toFloat() / width,
                maxDimension.toFloat() / height
            )
            val newWidth = (width * scale).toInt()
            val newHeight = (height * scale).toInt()
            Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
        } else {
            originalBitmap
        }

        // Compress to max 1MB
        var quality = 85
        var outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        
        while (outputStream.toByteArray().size > 1024 * 1024 && quality > 20) {
            quality -= 10
            outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        }

        return scaledBitmap
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return outputStream.toByteArray()
    }

    suspend fun uploadAadhaarFromFile(
        uid: String,
        documentType: String,
        file: File,
        onProgress: (Double) -> Unit
    ): Result<String> {
        return try {
            val path = "providers/$uid/documents/aadhaar_$documentType.jpg"
            val storageRef = storage.reference.child(path)
            
            val uploadTask = storageRef.putFile(Uri.fromFile(file))
            
            uploadTask.addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
                onProgress(progress)
            }
            
            val result = uploadTask.await()
            val downloadUrl = result.storage.downloadUrl.await()
            
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

