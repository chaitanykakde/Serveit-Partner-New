/**
 * Send Job Notification
 * Firestore onCreate trigger on jobRequests/{jobId}
 * 
 * Notification function - medium risk
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");

function createSendJobNotificationFunction(db, messaging) {
  return functions.firestore
    .document("jobRequests/{jobId}")
    .onCreate(async (snap, context) => {
      const jobData = snap.data();
      const jobId = context.params.jobId;

      // Get assigned partner's FCM token
      const partnerId = jobData.assignedPartnerId;
      if (!partnerId) return null;

      try {
        const partnerDoc = await db
            .collection("partners")
            .doc(partnerId)
            .get();

        const partnerData = partnerDoc.data();
        const fcmToken = partnerData?.fcmToken;

        // Store notification in Firestore first
        const notificationData = {
          title: "🔔 New Job Request",
          message: `You have a new ${jobData.serviceType} request from ${jobData.customerName}`,
          type: "INFO",
          timestamp: Date.now(),
          isRead: false,
          userId: partnerId,
          relatedData: {
            type: "job_request",
            jobId: jobId,
            serviceType: jobData.serviceType,
            customerName: jobData.customerName,
            timestamp: Date.now().toString(),
          },
        };

        // Store in partners/{userId}/notifications/
        await db
            .collection("partners")
            .doc(partnerId)
            .collection("notifications")
            .add(notificationData);

        console.log(`Job notification stored in Firestore for user ${partnerId}`);

        if (fcmToken) {
          const message = {
            token: fcmToken,
            notification: {
              title: "🔔 New Job Request",
              body: `You have a new ${jobData.serviceType} request from ${jobData.customerName}`,
            },
            data: {
              type: "job_request",
              jobId: jobId,
              serviceType: jobData.serviceType,
              timestamp: Date.now().toString(),
            },
            android: {
              notification: {
                icon: "ic_work",
                color: "#2196F3",
                channelId: "job_notifications",
                priority: "high",
                defaultSound: true,
                defaultVibrateTimings: true,
              },
            },
          };

          await messaging.send(message);
          console.log(`Job FCM notification sent to partner ${partnerId}`);
        } else {
          console.log(`No FCM token for partner ${partnerId}, but notification stored in Firestore`);
        }
      } catch (error) {
        console.error("Error sending job notification:", error);
      }

      return null;
    });
}

module.exports = { createSendJobNotificationFunction };

