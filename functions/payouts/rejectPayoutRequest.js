/**
 * Reject Payout Request
 * 
 * HTTPS Callable function for admin to reject payout requests
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { requireAuth } = require("../guards/authGuard");
const { sendPayoutStatusNotification } = require("./notificationHelpers");

function createRejectPayoutRequestFunction(db, messaging, collections) {
  return functions.https.onCall(async (data, context) => {
    // TODO: Implement admin role verification
    requireAuth(context);

    const { requestId, reason, notes } = data;

    if (!requestId) {
      throw new functions.https.HttpsError('invalid-argument', 'requestId is required');
    }

    if (!reason) {
      throw new functions.https.HttpsError('invalid-argument', 'reason is required');
    }

    try {
      // Update payout request status
      await db.collection(collections.payoutRequests).doc(requestId).update({
        requestStatus: 'REJECTED',
        processedAt: admin.firestore.FieldValue.serverTimestamp(),
        failureReason: reason,
        notes: notes || '',
        processedBy: context.auth.uid
      });

      // Send notification to partner
      await sendPayoutStatusNotification(db, messaging, collections, requestId, 'REJECTED', reason);

      return { success: true, message: 'Payout request rejected successfully' };
    } catch (error) {
      console.error('Error rejecting payout request:', error);
      throw new functions.https.HttpsError('internal', 'Failed to reject payout request');
    }
  });
}

module.exports = { createRejectPayoutRequestFunction };

