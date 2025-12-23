const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

/**
 * Cloud Function: Automatically send FCM notification when provider
 * profile status changes
 * Triggers on: providers/{uid} document updates
 */
exports.sendProfileStatusNotification = functions.firestore
    .document("providers/{uid}")
    .onUpdate(async (change, context) => {
      const before = change.before.data();
      const after = change.after.data();
      const uid = context.params.uid;

      // Log the update for debugging
      console.log(`[${uid}] Document updated. Before:`, {
        onboardingStatus: before.onboardingStatus,
        approvalStatus: before.approvalStatus,
      });
      console.log(`[${uid}] After:`, {
        onboardingStatus: after.onboardingStatus,
        approvalStatus: after.approvalStatus,
        hasFcmToken: !!after.fcmToken,
      });

      // Get FCM token from the provider document
      const fcmToken = after.fcmToken;

      if (!fcmToken) {
        console.log(`[${uid}] No FCM token found for provider`);
        return null;
      }

      let notificationTitle = "";
      let notificationBody = "";
      let shouldSend = false;

      // Check if profile was just submitted
      if (before.onboardingStatus !== "SUBMITTED" &&
          after.onboardingStatus === "SUBMITTED") {
        notificationTitle = "Profile Submitted";
        notificationBody = "Your profile is under review. " +
            "We will notify you once the verification is complete.";
        shouldSend = true;
        console.log(`[${uid}] Status changed to SUBMITTED`);
      } else if (before.approvalStatus !== "APPROVED" &&
          after.approvalStatus === "APPROVED") {
        // Check if profile was approved
        notificationTitle = "Profile Approved! 🎉";
        notificationBody = "Congratulations! Your profile has been " +
            "approved. You can now start receiving service requests.";
        shouldSend = true;
        console.log(`[${uid}] Status changed to APPROVED`);
      } else if (before.approvalStatus !== "REJECTED" &&
          after.approvalStatus === "REJECTED") {
        // Check if profile was rejected
        notificationTitle = "Profile Rejected";
        const reason = after.rejectionReason ||
            "Please check your profile for details.";
        notificationBody = `Your profile has been rejected. Reason: ${reason}`;
        shouldSend = true;
        console.log(`[${uid}] Status changed to REJECTED`);
      } else {
        // If no status change, don't send notification
        console.log(`[${uid}] No relevant status change detected`);
        return null;
      }

      if (!shouldSend) {
        return null;
      }

      // Prepare notification message
      const message = {
        notification: {
          title: notificationTitle,
          body: notificationBody,
        },
        data: {
          type: "profile_status_update",
          approvalStatus: after.approvalStatus || "",
          onboardingStatus: after.onboardingStatus || "",
          uid: uid,
        },
        token: fcmToken,
        android: {
          priority: "high",
          notification: {
            channelId: "serveit_partner_notifications",
            priority: "high",
            sound: "default",
          },
        },
      };

      try {
        // Send notification
        const response = await admin.messaging().send(message);
        console.log(`Successfully sent notification to ${uid}:`, response);
        return response;
      } catch (error) {
        console.error(`Error sending notification to ${uid}:`, error);

        // If token is invalid, try to get all tokens from subcollection
        if (error.code === "messaging/invalid-registration-token" ||
            error.code === "messaging/registration-token-not-registered") {
          console.log(`Token invalid, trying to get tokens from ` +
              `subcollection for ${uid}`);

          try {
            const tokensSnapshot = await admin.firestore()
                .collection("providers")
                .document(uid)
                .collection("fcmTokens")
                .orderBy("createdAt", "desc")
                .limit(5)
                .get();

            if (!tokensSnapshot.empty) {
              // Try the most recent token
              const latestToken = tokensSnapshot.docs[0].data().token;
              message.token = latestToken;

              try {
                const response = await admin.messaging().send(message);
                console.log(`Successfully sent notification using ` +
                    `latest token for ${uid}:`, response);

                // Update main document with latest working token
                await admin.firestore()
                    .collection("providers")
                    .document(uid)
                    .update({fcmToken: latestToken});

                return response;
              } catch (retryError) {
                console.error(`Failed to send with latest token ` +
                    `for ${uid}:`, retryError);
              }
            }
          } catch (subcollectionError) {
            console.error(`Error accessing token subcollection ` +
                `for ${uid}:`, subcollectionError);
          }
        }

        return null;
      }
    });

/**
 * Cloud Function: Send notification when profile is submitted
 * (alternative trigger)
 * This can be called manually or from admin panel
 */
exports.sendCustomNotification = functions.https.onCall(
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
        const providerDoc = await admin.firestore()
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

        const response = await admin.messaging().send(message);
        return {success: true, messageId: response};
      } catch (error) {
        console.error("Error sending custom notification:", error);
        throw new functions.https.HttpsError("internal", error.message);
      }
    });
