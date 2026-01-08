/**
 * DEPRECATED: Accept Booking (Legacy Function)
 * 
 * ⚠️ This function is DEPRECATED and kept only for backward compatibility.
 * Use acceptJobRequest instead, which uses the optimized inbox-based lookup.
 * 
 * This function performs a full collection scan which is inefficient.
 * It is preserved in _dev_only for reference but should NOT be used in production.
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { requireAuth } = require("../../guards/authGuard");

function createAcceptBookingFunction(db, collections) {
  return functions.https.onCall(async (data, context) => {
    try {
      // Auth check
      requireAuth(context);

      const { bookingId, providerId } = data;

      if (!bookingId || !providerId) {
        throw new functions.https.HttpsError(
          "invalid-argument",
          "bookingId and providerId are required"
        );
      }

      console.log(`📞 [DEPRECATED] Provider ${providerId} accepting booking ${bookingId} via legacy acceptBooking function`);

      // Find booking and update status (INEFFICIENT: Full collection scan)
      const bookingsQuery = await db.collection(collections.bookings).get();

      for (const doc of bookingsQuery.docs) {
        const bookingsArray = doc.data().bookings || [];
        const bookingIndex = bookingsArray.findIndex(b => b.bookingId === bookingId);

        if (bookingIndex !== -1) {
          bookingsArray[bookingIndex].bookingStatus = "accepted";
          bookingsArray[bookingIndex].providerId = providerId;
          bookingsArray[bookingIndex].acceptedAt = admin.firestore.FieldValue.serverTimestamp();
          bookingsArray[bookingIndex].updatedAt = admin.firestore.FieldValue.serverTimestamp();

          await doc.ref.update({
            bookings: bookingsArray,
            lastUpdated: admin.firestore.FieldValue.serverTimestamp()
          });

          console.log(`✅ Booking ${bookingId} status updated to ACCEPTED (via deprecated function)`);

          return {
            success: true,
            message: "Booking accepted successfully",
            deprecated: true,
            warning: "This function is deprecated. Please use acceptJobRequest instead."
          };
        }
      }

      throw new functions.https.HttpsError("not-found", "Booking not found");

    } catch (error) {
      console.error("❌ Error accepting booking:", error);
      throw error instanceof functions.https.HttpsError
        ? error
        : new functions.https.HttpsError("internal", error.message);
    }
  });
}

module.exports = { createAcceptBookingFunction };

