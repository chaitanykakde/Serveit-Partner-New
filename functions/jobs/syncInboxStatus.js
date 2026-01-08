/**
 * Sync Inbox Status
 * Firestore onUpdate trigger on Bookings/{phoneNumber}
 * Keeps inbox UI in sync with booking status changes
 * 
 * Utility function - medium risk
 */

const functions = require("firebase-functions");

function createSyncInboxStatusFunction(db) {
  return functions.firestore
    .document("Bookings/{phoneNumber}")
    .onUpdate(async (change, context) => {
      try {
        const afterData = change.after.data();
        const bookingsArray = afterData.bookings || [];
        
        // For each booking that has a providerId (accepted job)
        for (let i = 0; i < bookingsArray.length; i++) {
          const booking = bookingsArray[i];
          if (booking.providerId && booking.bookingId) {
            // Update inbox entry status
            const inboxRef = db
              .collection("provider_job_inbox")
              .doc(booking.providerId)
              .collection("jobs")
              .doc(booking.bookingId);
            
            await inboxRef.update({
              status: booking.status,
            }).catch((error) => {
              // Inbox entry might not exist (deleted/expired), ignore
              console.log(`Inbox entry not found for booking ${booking.bookingId}, ignoring sync`);
            });
          }
        }
        
        // Also handle single booking format (legacy)
        if (!Array.isArray(bookingsArray) && afterData.providerId && afterData.bookingId) {
          const inboxRef = db
            .collection("provider_job_inbox")
            .doc(afterData.providerId)
            .collection("jobs")
            .doc(afterData.bookingId);
          
          await inboxRef.update({
            status: afterData.status,
          }).catch((error) => {
            console.log(`Inbox entry not found for booking ${afterData.bookingId}, ignoring sync`);
          });
        }
      } catch (error) {
        console.error("Error syncing inbox status:", error);
      }
      
      return null;
    });
}

module.exports = { createSyncInboxStatusFunction };

