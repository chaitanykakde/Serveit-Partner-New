/**
 * Send Verification Notification
 * Firestore onUpdate trigger on partners/{partnerId}
 * 
 * REFACTORED: Uses verificationDetails.status as primary source of truth
 * Backward compatible with old boolean fields (isVerified, verificationDetails.verified, verificationDetails.rejected)
 * 
 * Notification function - medium risk
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");

/**
 * Extract verification status from document data.
 * Uses NEW schema (verificationDetails.status) as primary source.
 * Falls back to OLD schema (boolean fields) for backward compatibility.
 * 
 * @param {Object} data - Document data
 * @returns {string} Status: "verified" | "pending" | "rejected"
 */
function extractVerificationStatus(data) {
  // PRIMARY: Check new schema first (verificationDetails.status)
  const status = data?.verificationDetails?.status;
  if (status === "verified" || status === "pending" || status === "rejected") {
    return status;
  }

  // FALLBACK: Check old boolean fields
  const isVerified = data?.isVerified || data?.verificationDetails?.verified || false;
  const isRejected = data?.verificationDetails?.rejected || false;

  if (isVerified) {
    return "verified";
  }
  if (isRejected) {
    return "rejected";
  }
  return "pending";
}

/**
 * Check if verification-related fields changed between before and after states.
 * 
 * @param {Object} before - Before document data
 * @param {Object} after - After document data
 * @returns {boolean} True if verification fields changed
 */
function didVerificationFieldsChange(before, after) {
  // Check if verificationDetails object changed
  const beforeVerificationDetails = before?.verificationDetails || {};
  const afterVerificationDetails = after?.verificationDetails || {};
  
  const verificationDetailsChanged = 
    JSON.stringify(beforeVerificationDetails) !== JSON.stringify(afterVerificationDetails);
  
  // Check if isVerified changed
  const isVerifiedChanged = before?.isVerified !== after?.isVerified;
  
  return verificationDetailsChanged || isVerifiedChanged;
}

function createSendVerificationNotificationFunction(db, messaging) {
  return functions.firestore
    .document("partners/{partnerId}")
    .onUpdate(async (change, context) => {
      const before = change.before.data();
      const after = change.after.data();
      const partnerId = context.params.partnerId;

      console.log(`[sendVerificationNotification] Function triggered for partner: ${partnerId}`);

      // PERFORMANCE: Early return if verification fields didn't change
      if (!didVerificationFieldsChange(before, after)) {
        console.log(`[sendVerificationNotification] No verification field changes detected, skipping`);
        return null;
      }

      // Extract status using new schema with fallback
      const beforeStatus = extractVerificationStatus(before);
      const afterStatus = extractVerificationStatus(after);

      // Determine which schema was used
      const beforeUsesNewSchema = before?.verificationDetails?.status !== undefined;
      const afterUsesNewSchema = after?.verificationDetails?.status !== undefined;
      const schemaUsed = afterUsesNewSchema ? "NEW" : "OLD (fallback)";

      console.log(`[sendVerificationNotification] Status change: ${beforeStatus} → ${afterStatus} (Schema: ${schemaUsed})`);
      console.log(`[sendVerificationNotification] Before verificationDetails:`, before?.verificationDetails);
      console.log(`[sendVerificationNotification] After verificationDetails:`, after?.verificationDetails);

      // Only send notification if status actually changed
      if (beforeStatus === afterStatus) {
        console.log(`[sendVerificationNotification] Status unchanged (${afterStatus}), no notification needed`);
        return null;
      }

      let notificationType = null;
      let title = "";
      let body = "";

      // Determine notification type based on status transition
      if (beforeStatus !== "verified" && afterStatus === "verified") {
        // VERIFIED: Status changed to verified
        notificationType = "VERIFIED";
        title = "🎉 Profile Verified";
        body = "Your profile has been verified. You can now start receiving jobs.";
        
        // Include verifiedBy and verifiedAt if available
        const verifiedBy = after?.verificationDetails?.verifiedBy;
        const verifiedAt = after?.verificationDetails?.verifiedAt;
        if (verifiedBy) {
          body += ` Verified by: ${verifiedBy}`;
        }
        
        console.log(`[sendVerificationNotification] Detected VERIFIED transition`);
      } else if (beforeStatus !== "rejected" && afterStatus === "rejected") {
        // REJECTED: Status changed to rejected
        notificationType = "REJECTED";
        title = "❌ Profile Rejected";
        
        const rejectedReason = after?.verificationDetails?.rejectedReason || 
                              after?.verificationDetails?.rejectionReason; // Support legacy field name
        
        if (rejectedReason) {
          body = `Your profile was rejected. Reason: ${rejectedReason}`;
        } else {
          body = "Your profile was rejected. Please review and resubmit.";
        }
        
        console.log(`[sendVerificationNotification] Detected REJECTED transition`);
      } else if (beforeStatus === "rejected" && afterStatus === "pending") {
        // PENDING (RESUBMITTED): Status changed from rejected to pending (Edit & Resubmit case)
        notificationType = "PENDING";
        title = "⏳ Profile Resubmitted";
        body = "Your updated profile has been resubmitted for verification.";
        
        console.log(`[sendVerificationNotification] Detected RESUBMITTED transition (rejected → pending)`);
      } else {
        // Other transitions (e.g., pending → pending, verified → verified) - no notification
        console.log(`[sendVerificationNotification] Status transition ${beforeStatus} → ${afterStatus} does not require notification`);
        return null;
      }

      // Send notification
      if (notificationType) {
        try {
          console.log(`[sendVerificationNotification] Attempting to send ${notificationType} notification`);

          // Get user's FCM token from partners collection
          const userDoc = await db
              .collection("partners")
              .doc(partnerId)
              .get();

          const userData = userDoc.data();
          const fcmToken = userData?.fcmToken;

          console.log(`[sendVerificationNotification] FCM Token found: ${fcmToken ? "Yes" : "No"}`);

          // Store notification in user-specific Firestore path FIRST
          const notificationData = {
            title: title,
            message: body,
            type: notificationType,
            status: afterStatus, // Include current status
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

          console.log(`[sendVerificationNotification] Notification stored in Firestore for user ${partnerId}`);

          // Send FCM notification if token exists
          if (fcmToken) {
            // Determine icon and color based on notification type
            const icon = notificationType === "VERIFIED" ? "ic_check_circle" : "ic_error";
            const color = notificationType === "VERIFIED" ? "#4CAF50" : "#F44336";
            
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
                  icon: icon,
                  color: color,
                  channelId: "verification_notifications",
                  priority: "high",
                  defaultSound: true,
                  defaultVibrateTimings: true,
                },
              },
            };

            const result = await messaging.send(message);
            console.log(`[sendVerificationNotification] FCM notification sent successfully to partner ${partnerId}: ${notificationType}`, result);
          } else {
            console.log(`[sendVerificationNotification] No FCM token found for partner ${partnerId}, but notification stored in Firestore`);
          }
        } catch (error) {
          console.error(`[sendVerificationNotification] Error sending/storing notification:`, error);
        }
      }

      return null;
    });
}

module.exports = { createSendVerificationNotificationFunction };

