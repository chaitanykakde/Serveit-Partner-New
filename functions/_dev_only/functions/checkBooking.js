/**
 * Debug Function: Check Booking Setup
 *
 * Helps debug booking configuration
 * Call this to verify your booking structure
 *
 * Usage: checkBooking({ bookingId: "your_booking_id" })
 * 
 * ⚠️ DEV ONLY - Not exported in production
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");

// Note: db should be passed or imported from parent
// For now, we'll require it to be passed as parameter or use admin.firestore()
function createCheckBookingFunction(db) {
  return functions.https.onCall(async (data, context) => {
    try {
      if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Must be authenticated");
      }

      const { bookingId } = data;
      const userPhone = context.auth.token.phone_number;

      if (!userPhone) {
        return {
          success: false,
          error: "No phone number in auth token",
          hint: "Make sure you're logged in with phone auth"
        };
      }

      // Format phone number exactly like the app
      const formattedPhone = userPhone.startsWith("+91") ? userPhone : `+91${userPhone}`;
      console.log(`🔍 Checking booking ${bookingId || "ALL"} for user ${formattedPhone}`);

      const userBookingDoc = await db.collection("Bookings")
        .doc(formattedPhone)
        .get();

      if (!userBookingDoc.exists) {
        return {
          success: false,
          error: "No bookings document found",
          phone: formattedPhone,
          hint: `Create a document in Firestore: Bookings/${formattedPhone}`
        };
      }

      const bookingsArray = userBookingDoc.data().bookings || [];

      // If no bookingId provided, return all bookings
      if (!bookingId) {
        return {
          success: true,
          message: `Found ${bookingsArray.length} bookings`,
          phone: formattedPhone,
          totalBookings: bookingsArray.length,
          bookings: bookingsArray.map(b => ({
            bookingId: b.bookingId,
            serviceName: b.serviceName,
            status: b.bookingStatus || "Unknown",
            providerName: b.providerName || "Not set",
            canMakeCall: ["accepted", "arrived", "in_progress", "payment_pending"].includes(
              (b.bookingStatus || "").toLowerCase()
            )
          }))
        };
      }

      // Find specific booking
      const booking = bookingsArray.find(b => b.bookingId === bookingId);

      if (!booking) {
        return {
          success: false,
          error: "Booking not found",
          phone: formattedPhone,
          requestedBookingId: bookingId,
          totalBookings: bookingsArray.length,
          availableBookingIds: bookingsArray.map(b => b.bookingId),
          hint: "Check if the bookingId matches exactly (case-sensitive)"
        };
      }

      const bookingStatus = (booking.bookingStatus || "pending").toLowerCase();
      const canMakeCall = ["accepted", "arrived", "in_progress", "payment_pending"].includes(bookingStatus);

      return {
        success: true,
        message: "✅ Booking found!",
        phone: formattedPhone,
        booking: {
          bookingId: booking.bookingId,
          serviceName: booking.serviceName,
          status: booking.bookingStatus || "Unknown",
          providerName: booking.providerName || "Not set",
          providerPhone: booking.providerMobile || "9322067937",
          totalPrice: booking.totalPrice || 0,
          createdAt: booking.createdAt || null,
          canMakeCall: canMakeCall,
          callAllowedStatuses: ["accepted", "arrived", "in_progress", "payment_pending"],
          currentStatus: bookingStatus
        },
        verdict: canMakeCall
          ? "✅ This booking CAN make calls"
          : `❌ Cannot call with status "${bookingStatus}". Change status to: accepted/arrived/in_progress`
      };

    } catch (error) {
      console.error("❌ Error checking booking:", error);
      return {
        success: false,
        error: error.message,
        stack: error.stack
      };
    }
  });
}

module.exports = { createCheckBookingFunction };

