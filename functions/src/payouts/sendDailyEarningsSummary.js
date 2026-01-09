/**
 * Send Daily Earnings Summary
 * Pub/Sub Scheduled function (8 PM daily, Asia/Kolkata)
 * 
 * Notification function - medium risk
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");

function createSendDailyEarningsSummaryFunction(db, messaging) {
  return functions.pubsub
    .schedule("0 20 * * *") // 8 PM daily
    .timeZone("Asia/Kolkata")
    .onRun(async (context) => {
      try {
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        // Get all active partners
        const partnersSnapshot = await db
            .collection("partners")
            .where("isVerified", "==", true)
            .get();

        for (const partnerDoc of partnersSnapshot.docs) {
          const partnerData = partnerDoc.data();
          const partnerId = partnerDoc.id;
          const fcmToken = partnerData.fcmToken;

          if (!fcmToken) continue;

          // Calculate today's earnings
          const earningsSnapshot = await db
              .collection("earnings")
              .where("partnerId", "==", partnerId)
              .where("date", ">=", today)
              .get();

          let totalEarnings = 0;
          let jobsCompleted = 0;

          earningsSnapshot.forEach((doc) => {
            const earning = doc.data();
            totalEarnings += earning.amount || 0;
            jobsCompleted += 1;
          });

          if (jobsCompleted > 0) {
            // Store notification in Firestore first
            const notificationData = {
              title: "💰 Daily Earnings Summary",
              message: `Today you earned ₹${totalEarnings} from ${jobsCompleted} jobs completed!`,
              type: "INFO",
              timestamp: Date.now(),
              isRead: false,
              userId: partnerId,
              relatedData: {
                type: "earnings_summary",
                amount: totalEarnings.toString(),
                jobsCount: jobsCompleted.toString(),
                date: today.toISOString(),
              },
            };

            // Store in partners/{userId}/notifications/
            await db
                .collection("partners")
                .doc(partnerId)
                .collection("notifications")
                .add(notificationData);

            const message = {
              token: fcmToken,
              notification: {
                title: "💰 Daily Earnings Summary",
                body: `Today you earned ₹${totalEarnings} from ${jobsCompleted} jobs completed!`,
              },
              data: {
                type: "earnings_summary",
                amount: totalEarnings.toString(),
                jobsCount: jobsCompleted.toString(),
                date: today.toISOString(),
              },
            };

            await messaging.send(message);
          }
        }

        console.log("Daily earnings summaries sent");
      } catch (error) {
        console.error("Error sending daily summaries:", error);
      }

      return null;
    });
}

module.exports = { createSendDailyEarningsSummaryFunction };

