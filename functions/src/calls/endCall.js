/**
 * End Call and Log Duration
 * HTTPS Callable function called when call ends
 * 
 * Utility function - medium risk
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");

function createEndCallFunction(db) {
  return functions.https.onCall(async (data, context) => {
    try {
      if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Must be authenticated");
      }

      const { bookingId, duration, endReason } = data;

      console.log(`📞 Ending call for booking ${bookingId}, Duration: ${duration}s`);

      // Update call log
      const callLogsQuery = await db.collection("CallLogs")
        .where("bookingId", "==", bookingId)
        .orderBy("tokenGeneratedAt", "desc")
        .limit(1)
        .get();

      if (!callLogsQuery.empty) {
        const callLogDoc = callLogsQuery.docs[0];
        await callLogDoc.ref.update({
          status: "completed",
          endedAt: admin.firestore.FieldValue.serverTimestamp(),
          durationSeconds: duration || 0,
          endReason: endReason || "user_hangup"
        });
      }

      return { success: true };

    } catch (error) {
      console.error("❌ Error ending call:", error);
      throw error instanceof functions.https.HttpsError
        ? error
        : new functions.https.HttpsError("internal", error.message);
    }
  });
}

module.exports = { createEndCallFunction };

