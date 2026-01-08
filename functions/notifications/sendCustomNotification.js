/**
 * Send Custom Notification
 * HTTPS Callable function for admin to send custom notifications
 * 
 * Notification function - medium risk
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");

function createSendCustomNotificationFunction(db, messaging) {
  return functions.https.onCall(
    async (data, context) => {
      // Only allow authenticated admin users
      if (!context.auth || !context.auth.token.admin) {
        throw new functions.https.HttpsError(
            "permission-denied",
            "Only admins can send custom notifications",
        );
      }

      const {uid, title, body} = data;

      if (!uid || !title || !body) {
        throw new functions.https.HttpsError(
            "invalid-argument",
            "uid, title, and body are required",
        );
      }

      try {
        const providerDoc = await db
            .collection("providers")
            .doc(uid)
            .get();

        if (!providerDoc.exists) {
          throw new functions.https.HttpsError(
              "not-found", "Provider not found",
          );
        }

        const providerData = providerDoc.data();
        const fcmToken = providerData.fcmToken;

        if (!fcmToken) {
          throw new functions.https.HttpsError(
              "not-found", "FCM token not found",
          );
        }

        const message = {
          notification: {
            title: title,
            body: body,
          },
          token: fcmToken,
          android: {
            priority: "high",
            notification: {
              channelId: "serveit_partner_notifications",
              priority: "high",
            },
          },
        };

        const response = await messaging.send(message);
        return {success: true, messageId: response};
      } catch (error) {
        console.error("Error sending custom notification:", error);
        throw new functions.https.HttpsError("internal", error.message);
      }
    });
}

module.exports = { createSendCustomNotificationFunction };

