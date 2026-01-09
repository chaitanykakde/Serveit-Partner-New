package com.nextserve.serveitpartnernew.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Utility class for generating UPI QR codes
 */
object QrUtils {

    /**
     * Generate UPI URI for payment
     */
    fun generateUpiUri(
        upiId: String = "9307879687-3@ybl",
        payeeName: String = "Servit Partner",
        amount: Double,
        currency: String = "INR",
        transactionNote: String
    ): String {
        val encodedNote = URLEncoder.encode(transactionNote, StandardCharsets.UTF_8.toString())
        return "upi://pay?pa=$upiId&pn=$payeeName&am=$amount&cu=$currency&tn=$encodedNote"
    }

    /**
     * Generate UPI transaction note
     */
    fun generateUpiNote(
        customerName: String,
        serviceName: String,
        providerName: String,
        bookingId: String
    ): String {
        return "Payment for $serviceName - $customerName by $providerName (ID: ${bookingId.takeLast(8)})"
    }

    /**
     * Generate QR code bitmap from text
     */
    fun generateQRCode(text: String, size: Int = 512): Bitmap? {
        return try {
            val hints = hashMapOf<EncodeHintType, Any>()
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            hints[EncodeHintType.MARGIN] = 1 // Default margin

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size, hints)

            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generate UPI QR code bitmap with payment details
     */
    fun generateUpiQRCode(
        customerName: String,
        serviceName: String,
        providerName: String,
        bookingId: String,
        amount: Double,
        size: Int = 512
    ): Bitmap? {
        val upiNote = generateUpiNote(customerName, serviceName, providerName, bookingId)
        val upiUri = generateUpiUri(amount = amount, transactionNote = upiNote)
        return generateQRCode(upiUri, size)
    }
}
