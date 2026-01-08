/**
 * Validate Call Permission
 * 
 * HTTPS Callable function to check if user can make a call for a booking
 * Returns booking details and call permission status
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { requireAuth } = require("../guards/authGuard");

function createValidateCallPermissionFunction(db, messaging, collections) {
  /**
   * Send FCM notification for incoming call (WhatsApp-style)
   */
  async function sendIncomingCallNotification(providerId, callData) {
    try {
      console.log("📱 Sending FCM notification to provider:", providerId);

      const providerDoc = await db.collection(collections.partners).doc(providerId).get();
      if (!providerDoc.exists) {
        console.log("⚠️ Provider document not found for FCM:", providerId);
        return;
      }

      const providerData = providerDoc.data();
      const fcmToken = providerData.fcmToken;

      if (!fcmToken) {
        console.log("⚠️ No FCM token found for provider:", providerId);
        return;
      }

      const message = {
        token: fcmToken,
        android: {
          priority: "high",
          ttl: 300000,
          notification: {
            title: "Incoming Call",
            body: `${callData.serviceName || 'Service Call'} from ${callData.userMobile?.slice(-4) || 'Customer'}`,
            sound: "default",
            priority: "max",
            channel_id: "incoming_calls",
            default_vibrate_timings: true,
            visibility: "public",
          },
        },
        data: {
          type: "incoming_call",
          callId: callData.bookingId || callData.callId,
          bookingId: callData.bookingId,
          userMobile: callData.userMobile,
          serviceName: callData.serviceName || 'Service Call',
          initiatedBy: callData.initiatedBy || 'USER',
          click_action: "FLUTTER_NOTIFICATION_CLICK",
        },
      };

      const response = await messaging.send(message);
      console.log("✅ FCM notification sent successfully:", response);

    } catch (error) {
      console.error("❌ Failed to send FCM notification:", error);
    }
  }

  return functions.https.onCall(async (data, context) => {
    console.log("🔍 validateCallPermission invoked");

    try {
      requireAuth(context);
      console.log("✅ AUTH SUCCESS: uid =", context.auth.uid);

      const { bookingId, userMobile, callerRole } = data;
      console.log("📥 INPUT DATA:", JSON.stringify({ bookingId, userMobile, callerRole }));

      if (!bookingId || !userMobile || !callerRole) {
        throw new functions.https.HttpsError(
          "invalid-argument",
          "bookingId, userMobile, and callerRole are required"
        );
      }

      // Step 1: Read booking document
      console.log("📂 STEP 1: Reading Firestore document");
      const bookingDoc = await db.collection(collections.bookings).doc(userMobile).get();

      if (!bookingDoc.exists) {
        throw new functions.https.HttpsError(
          "not-found",
          "User booking document not found"
        );
      }

      console.log("✅ STEP 1 SUCCESS: Document found");

      // Step 2: Find booking in array
      console.log("🔎 STEP 2: Searching for booking in array");
      const bookings = bookingDoc.data().bookings || [];
      const booking = bookings.find(b => b.bookingId === bookingId);

      if (!booking) {
        throw new functions.https.HttpsError(
          "not-found",
          "Booking not found"
        );
      }

      console.log("✅ STEP 2 SUCCESS: Booking found");

      // Step 3: Role-based validation
      console.log("🔐 STEP 3: Role-based validation");
      if (callerRole === "PROVIDER") {
        if (booking.providerId !== context.auth.uid) {
          throw new functions.https.HttpsError(
            "permission-denied",
            "Provider not assigned to this booking"
          );
        }
        console.log("✅ STEP 3 SUCCESS: Provider authorized");
      } else if (callerRole === "USER") {
        if (!booking.providerId) {
          throw new functions.https.HttpsError(
            "failed-precondition",
            "Booking must have an assigned provider to initiate calls"
          );
        }
        console.log("✅ STEP 3 SUCCESS: User authorized to call assigned provider");
      } else {
        throw new functions.https.HttpsError(
          "invalid-argument",
          "callerRole must be either 'USER' or 'PROVIDER'"
        );
      }

      // Step 4: Booking status validation
      console.log("📋 STEP 4: Booking status validation");
      let allowedStatuses;
      if (callerRole === "PROVIDER") {
        allowedStatuses = ["accepted", "arrived", "in_progress", "completed"];
      } else if (callerRole === "USER") {
        allowedStatuses = ["accepted", "arrived", "in_progress", "payment_pending"];
      }

      if (!allowedStatuses.includes(booking.bookingStatus)) {
        throw new functions.https.HttpsError(
          "failed-precondition",
          "Call not allowed for this booking status"
        );
      }

      console.log("✅ STEP 4 SUCCESS: Status validated");

      // Step 5: For USER-initiated calls, create ActiveCalls document and send FCM
      if (callerRole === "USER") {
        console.log("📱 STEP 5: Creating ActiveCalls document and sending FCM notification");

        const agoraConfig = functions.config().agora;
        if (!agoraConfig || !agoraConfig.app_id) {
          throw new functions.https.HttpsError("internal", "Call service temporarily unavailable");
        }

        const channelName = `serveit_booking_${booking.bookingId}`;

        const activeCallData = {
          bookingId: booking.bookingId,
          userMobile: userMobile,
          providerId: booking.providerId,
          providerPhone: booking.providerMobileNo || booking.providerPhone || "",
          serviceName: booking.serviceName || "Service Call",
          status: "RINGING",
          initiatedBy: "USER",
          channelName: channelName,
          appId: agoraConfig.app_id,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
          updatedAt: admin.firestore.FieldValue.serverTimestamp()
        };

        await db.collection(collections.activeCalls).doc(booking.bookingId).set(activeCallData);
        console.log("✅ ActiveCalls document created for USER call:", booking.bookingId);

        sendIncomingCallNotification(booking.providerId, {
          bookingId: booking.bookingId,
          userMobile: userMobile,
          serviceName: booking.serviceName,
          initiatedBy: "USER"
        });
      }

      console.log("🎉 FINAL RESULT: CALL PERMISSION GRANTED");

      return {
        allowed: true,
        booking
      };

    } catch (error) {
      console.error("❌ Error in validateCallPermission:", error);
      if (error instanceof functions.https.HttpsError) {
        throw error;
      }
      throw new functions.https.HttpsError("internal", error.message);
    }
  });
}

module.exports = { createValidateCallPermissionFunction };

