/**
 * Accept Job Request
 * 
 * HTTPS Callable function for providers to accept job requests
 * Uses inbox for O(1) lookup and transactional updates
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { requireAuth } = require("../guards/authGuard");
const { cleanupInboxForAcceptedJob } = require("../shared/cleanupUtils");

function createAcceptJobRequestFunction(db, collections) {
  return functions.https.onCall(async (data, context) => {
    try {
      // Verify authentication
      requireAuth(context);

      const {bookingId, providerId} = data;

      if (!bookingId || !providerId) {
        throw new functions.https.HttpsError("invalid-argument", "Missing required parameters");
      }

      // Get provider details
      const providerDoc = await db.collection(collections.partners)
          .doc(providerId)
          .get();

      if (!providerDoc.exists) {
        throw new functions.https.HttpsError("not-found", "Provider not found");
      }

      const providerData = providerDoc.data();

      // Execute transaction - OPTIMIZED: Use inbox for O(1) lookup
      const result = await db.runTransaction(async (transaction) => {
        // STEP 1: Read inbox entry (O(1) lookup)
        const inboxRef = db
          .collection(collections.jobInbox)
          .doc(providerId)
          .collection("jobs")
          .doc(bookingId);
        
        const inboxDoc = await transaction.get(inboxRef);
        
        if (!inboxDoc.exists) {
          throw new functions.https.HttpsError("not-found", "Job not found in your inbox");
        }
        
        const inboxData = inboxDoc.data();
        const customerPhone = inboxData.customerPhone;
        const bookingIndex = inboxData.bookingIndex;
        
        // STEP 2: Read EXACT booking document (direct access)
        const bookingRef = db.collection(collections.bookings).doc(customerPhone);
        const bookingDoc = await transaction.get(bookingRef);
        
        if (!bookingDoc.exists) {
          throw new functions.https.HttpsError("not-found", "Booking document not found");
        }
        
        const bookingData = bookingDoc.data();
        const bookingsArray = bookingData.bookings || [];
        
        // Handle both array and single booking formats
        let targetBooking;
        if (bookingsArray.length > 0 && Array.isArray(bookingsArray)) {
          // Array format
          if (bookingIndex >= bookingsArray.length) {
            throw new functions.https.HttpsError("not-found", "Booking index out of range");
          }
          targetBooking = bookingsArray[bookingIndex];
        } else {
          // Single booking format (legacy)
          targetBooking = bookingData;
        }
        
        // STEP 3: Validate (from SOURCE OF TRUTH)
        if (targetBooking.bookingId !== bookingId) {
          throw new functions.https.HttpsError("failed-precondition", "Booking ID mismatch");
        }
        
        if (targetBooking.status !== "pending") {
          throw new functions.https.HttpsError("failed-precondition", "Job has already been accepted by another provider");
        }
        
        if (!targetBooking.notifiedProviderIds || !targetBooking.notifiedProviderIds.includes(providerId)) {
          throw new functions.https.HttpsError("failed-precondition", "Provider was not notified for this job");
        }
        
        // STEP 4: Update booking (SOURCE OF TRUTH)
        const acceptedAtTimestamp = admin.firestore.Timestamp.now();
        
        // Extract provider mobile number with multiple fallbacks
        const providerMobileNo = providerData.personalDetails?.mobileNo ||
                                providerData.personalDetails?.phoneNumber ||
                                providerData.mobileNo ||
                                providerData.phoneNumber ||
                                "";

        if (bookingsArray.length > 0 && Array.isArray(bookingsArray)) {
          // Array format: Update specific booking in array
          const updatedBookings = [...bookingsArray];
          updatedBookings[bookingIndex] = {
            ...targetBooking,
            providerId: providerId,
            providerName: providerData.personalDetails?.fullName || providerData.fullName || "Unknown Provider",
            providerMobileNo: providerMobileNo,
            status: "accepted",
            bookingStatus: "accepted",
            acceptedByProviderId: providerId,
            acceptedAt: acceptedAtTimestamp,
          };
          
          transaction.update(bookingRef, {
            bookings: updatedBookings,
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          });
        } else {
          // Single booking format (legacy)
          transaction.update(bookingRef, {
            providerId: providerId,
            providerName: providerData.personalDetails?.fullName || providerData.fullName || "Unknown Provider",
            providerMobileNo: providerMobileNo,
            status: "accepted",
            bookingStatus: "accepted",
            acceptedByProviderId: providerId,
            acceptedAt: admin.firestore.FieldValue.serverTimestamp(),
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          });
        }
        
        // STEP 5: Update inbox entry
        transaction.update(inboxRef, {
          status: "accepted",
        });
        
        console.log(`Updated booking ${bookingId} in document ${collections.bookings}/${customerPhone} - accepted by provider ${providerId}`);
        return {success: true, message: "Job accepted successfully"};
      });
      
      // STEP 6: Cleanup inbox entries for other providers (outside transaction)
      try {
        await cleanupInboxForAcceptedJob(db, collections, bookingId, providerId);
      } catch (cleanupError) {
        console.error("Error cleaning up inbox entries:", cleanupError);
        // Don't fail the accept operation if cleanup fails
      }

      console.log(`Job ${bookingId} accepted by provider ${providerId}`);
      return result;
    } catch (error) {
      console.error("Error in acceptJobRequest:", error);
      throw error;
    }
  });
}

module.exports = { createAcceptJobRequestFunction };

