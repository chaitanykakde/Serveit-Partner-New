/**
 * Send Verification Notification
 * Firestore onUpdate trigger on partners/{partnerId}
 * 
 * Notification function - medium risk
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");

function createSendVerificationNotificationFunction(db, messaging) {
  return functions.firestore
    .document("partners/{partnerId}")
    .onUpdate(async (change, context) => {
      const before = change.before.data();
      const after = change.after.data();
      const partnerId = context.params.partnerId;

      console.log(`Function triggered for partner: ${partnerId}`);
      console.log("Before data:", JSON.stringify(before, null, 2));
      console.log("After data:", JSON.stringify(after, null, 2));

      // Check if verification status changed - check both isVerified and verificationDetails.verified
      const beforeVerified = before.isVerified || before.verificationDetails?.verified || false;
      const afterVerified = after.isVerified || after.verificationDetails?.verified || false;

      // Check if rejection reason changed (indicates rejection)
      const beforeRejection = before.verificationDetails?.rejectionReason;
      const afterRejection = after.verificationDetails?.rejectionReason;

      // Check if rejected status changed
      const beforeRejected = before.verificationDetails?.rejected || false;
      const afterRejected = after.verificationDetails?.rejected || false;

      console.log(`Verification status: ${beforeVerified} -> ${afterVerified}`);
      console.log(`Rejected status: ${beforeRejected} -> ${afterRejected}`);
      console.log(`Rejection reason: ${beforeRejection} -> ${afterRejection}`);

      let notificationType = null;
      let title = "";
      let body = "";

      // Determine notification type based on current verification state
      if (!beforeRejected && afterRejected) {
        // Application rejected
        notificationType = "VERIFICATION_REJECTED";
        title = "❌ Application Rejected";
        body = `Your application has been rejected. ${afterRejection ? `Reason: ${afterRejection}` : "Please check your documents and resubmit."}`;
        console.log("Detected application rejection");
      } else if (!beforeVerified && afterVerified) {
        // Verification successful
        notificationType = "VERIFICATION_APPROVED";
        title = "🎉 Verification Successful!";
        body = "Congratulations! Your account has been verified successfully. You can now start accepting jobs and providing services.";
        console.log("Detected verification success");
      } else if (beforeVerified && !afterVerified && !afterRejected) {
        // Under review (verified changed from true to false, but not rejected)
        notificationType = "VERIFICATION_PENDING";
        title = "⏳ Application Under Review";
        body = "Your application is currently under review. We will notify you once the verification process is complete.";
        console.log("Detected application under review");
      } else {
        console.log("No significant verification status change detected");
      }

      // Send notification if status changed
      if (notificationType) {
        try {
          console.log(`Attempting to send ${notificationType} notification`);

          // Get user's FCM token from partners collection
          const userDoc = await db
              .collection("partners")
              .doc(partnerId)
              .get();

          const userData = userDoc.data();
          const fcmToken = userData?.fcmToken;

          console.log(`FCM Token found: ${fcmToken ? "Yes" : "No"}`);

          // Store notification in user-specific Firestore path FIRST
          const notificationData = {
            title: title,
            message: body,
            type: notificationType,
            timestamp: Date.now(),
            isRead: false,
            userId: partnerId,
            relatedData: {
              type: notificationType,
              partnerId: partnerId,
              timestamp: Date.now().toString(),
            },
          };

          // Store in partners/{userId}/notifications/
          await db
              .collection("partners")
              .doc(partnerId)
              .collection("notifications")
              .add(notificationData);

          console.log(`Notification stored in Firestore for user ${partnerId}`);

          // Send FCM notification if token exists
          if (fcmToken) {
            const message = {
              token: fcmToken,
              notification: {
                title: title,
                body: body,
              },
              data: {
                type: notificationType,
                partnerId: partnerId,
                timestamp: Date.now().toString(),
              },
              android: {
                notification: {
                  icon: notificationType === "verification_approved" ? "ic_check_circle" : "ic_error",
                  color: notificationType === "verification_approved" ? "#4CAF50" : "#F44336",
                  channelId: "verification_notifications",
                  priority: "high",
                  defaultSound: true,
                  defaultVibrateTimings: true,
                },
              },
            };

            const result = await messaging.send(message);
            console.log(`FCM notification sent successfully to partner ${partnerId}: ${notificationType}`, result);
          } else {
            console.log(`No FCM token found for partner ${partnerId}, but notification stored in Firestore`);
          }
        } catch (error) {
          console.error("Error sending/storing notification:", error);
        }
      }

      return null;
    });
}

module.exports = { createSendVerificationNotificationFunction };

