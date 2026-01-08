/**
 * Generate Agora RTC Token
 * 
 * HTTPS Callable function for secure Agora token generation
 * Validates booking ownership and status before generating token
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { RtcTokenBuilder, RtcRole } = require("agora-access-token");
const { requireAuth, getUserPhone } = require("../guards/authGuard");

function createGenerateAgoraTokenFunction(db, config, collections) {
  /**
   * Generate numeric UID from phone number
   */
  function generateNumericUid(phoneNumber) {
    if (!phoneNumber) {
      return Math.floor(Math.random() * 1000000);
    }
    const numericOnly = phoneNumber.replace(/\D/g, "");
    const uid = parseInt(numericOnly.slice(-9)) || Math.floor(Math.random() * 1000000);
    return uid;
  }

  return functions.https.onCall(async (data, context) => {
    const startTime = Date.now();

    try {
      // STEP 1: Authentication Check
      requireAuth(context);
      const userId = context.auth.uid;
      const userPhone = getUserPhone(context);
      console.log(`🔐 Auth verified - User: ${userId}, Phone: ${userPhone}`);

      // STEP 2: Input Validation
      const { bookingId, userMobile } = data;

      if (!bookingId || typeof bookingId !== "string" || bookingId.trim() === "") {
        throw new functions.https.HttpsError(
          "invalid-argument",
          "bookingId is required and must be a non-empty string"
        );
      }
      if (!userMobile || typeof userMobile !== "string" || userMobile.trim() === "") {
        throw new functions.https.HttpsError(
          "invalid-argument",
          "userMobile is required and must be a non-empty string"
        );
      }

      console.log(`📋 Generating token for booking: ${bookingId}`);

      // STEP 3: Fetch Booking from Firestore
      const userBookingDoc = await db.collection(collections.bookings)
        .doc(userMobile)
        .get();

      if (!userBookingDoc.exists) {
        throw new functions.https.HttpsError(
          "not-found",
          `No bookings found. Document ID should be: ${userMobile}`
        );
      }

      const bookingsArray = userBookingDoc.data().bookings || [];
      console.log(`📊 Found ${bookingsArray.length} bookings for user`);

      if (bookingsArray.length === 0) {
        throw new functions.https.HttpsError(
          "not-found",
          "No bookings in your account"
        );
      }

      const booking = bookingsArray.find(b => b.bookingId === bookingId);

      if (!booking) {
        throw new functions.https.HttpsError(
          "not-found",
          `Booking ID "${bookingId}" not found. Available IDs: ${bookingsArray.map(b => b.bookingId).slice(0, 3).join(", ")}`
        );
      }

      console.log(`✅ Found booking: ${booking.serviceName || "Unknown Service"}`);

      // Validate booking status
      const bookingStatus = (booking.bookingStatus || "pending").toLowerCase();
      const allowedStatuses = ["accepted", "arrived", "in_progress", "payment_pending"];

      if (!allowedStatuses.includes(bookingStatus)) {
        throw new functions.https.HttpsError(
          "failed-precondition",
          `Cannot call with status "${bookingStatus}". Booking must be accepted first. Allowed: ${allowedStatuses.join(", ")}`
        );
      }

      console.log(`✅ Booking validated - Status: ${bookingStatus}`);

      // STEP 4: Generate Agora Token
      const agoraCreds = config.getAgoraCredentials();

      if (!agoraCreds.appId || !agoraCreds.appCertificate) {
        throw new functions.https.HttpsError(
          "internal",
          "Agora credentials not configured. Please contact administrator."
        );
      }

      const channelName = `serveit_booking_${bookingId}`;
      const uid = generateNumericUid(userPhone);
      const expirationTimeInSeconds = 600;
      const currentTimestamp = Math.floor(Date.now() / 1000);
      const privilegeExpiredTs = currentTimestamp + expirationTimeInSeconds;

      const token = RtcTokenBuilder.buildTokenWithUid(
        agoraCreds.appId,
        agoraCreds.appCertificate,
        channelName,
        uid,
        RtcRole.PUBLISHER,
        privilegeExpiredTs
      );

      console.log(`✅ Token generated successfully`);

      // STEP 5: Log Call Initiation
      await db.collection(collections.callLogs).add({
        bookingId: bookingId,
        userId: userId,
        userPhone: userPhone,
        channelName: channelName,
        uid: uid,
        tokenGeneratedAt: admin.firestore.FieldValue.serverTimestamp(),
        expiresAt: admin.firestore.Timestamp.fromMillis(privilegeExpiredTs * 1000),
        status: "initiated"
      });

      // STEP 5.5: Update ActiveCalls document
      try {
        await db.collection(collections.activeCalls).doc(bookingId).update({
          token: token,
          uid: uid,
          updatedAt: admin.firestore.FieldValue.serverTimestamp()
        });
        console.log(`✅ ActiveCalls document updated with token for booking: ${bookingId}`);
      } catch (updateError) {
        console.error(`⚠️ Failed to update ActiveCalls document:`, updateError);
      }

      const executionTime = Date.now() - startTime;
      console.log(`⏱️  Execution time: ${executionTime}ms`);

      return {
        success: true,
        token: token,
        channelName: channelName,
        uid: uid,
        appId: agoraCreds.appId,
        expiresIn: expirationTimeInSeconds,
        expiresAt: privilegeExpiredTs * 1000,
        message: "Token generated successfully"
      };

    } catch (error) {
      console.error("❌ Error in generateAgoraToken:", error);

      if (error instanceof functions.https.HttpsError) {
        throw error;
      }

      throw new functions.https.HttpsError(
        "internal",
        `Failed to generate token: ${error.message}`
      );
    }
  });
}

module.exports = { createGenerateAgoraTokenFunction };

