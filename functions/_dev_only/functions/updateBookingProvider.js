/**
 * Debug Function: Update Booking with Provider Info
 *
 * For testing - adds provider details to booking
 * 
 * ⚠️ DEV ONLY - Not exported in production
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");

// Note: db should be passed or imported from parent
function createUpdateBookingProviderFunction(db) {
  return functions.https.onCall(async (data, context) => {
    try {
      if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Must be authenticated");
      }

      const { bookingId, providerPhone } = data;
      const userPhone = context.auth.token.phone_number;

      if (!userPhone || !bookingId) {
        throw new functions.https.HttpsError(
          "invalid-argument",
          "userPhone and bookingId required"
        );
      }

      const userBookingDoc = await db.collection("Bookings")
        .doc(userPhone)
        .get();

      if (!userBookingDoc.exists) {
        throw new functions.https.HttpsError("not-found", "No bookings found");
      }

      const bookingsArray = userBookingDoc.data().bookings || [];
      const bookingIndex = bookingsArray.findIndex(b => b.bookingId === bookingId);

      if (bookingIndex === -1) {
        throw new functions.https.HttpsError("not-found", "Booking not found");
      }

      // Update booking with provider info
      bookingsArray[bookingIndex].providerPhone = providerPhone || "9322067937";
      bookingsArray[bookingIndex].providerName = "Service Provider";
      bookingsArray[bookingIndex].providerRating = 4.5;
      bookingsArray[bookingIndex].updatedAt = admin.firestore.FieldValue.serverTimestamp();

      await db.collection("Bookings")
        .doc(userPhone)
        .update({
          bookings: bookingsArray,
          lastUpdated: admin.firestore.FieldValue.serverTimestamp()
        });

      console.log(`✅ Updated booking ${bookingId} with provider info`);

      return {
        success: true,
        message: "Provider info updated",
        providerPhone: providerPhone || "9322067937"
      };

    } catch (error) {
      console.error("❌ Error updating booking:", error);
      throw error instanceof functions.https.HttpsError
        ? error
        : new functions.https.HttpsError("internal", error.message);
    }
  });
}

module.exports = { createUpdateBookingProviderFunction };

