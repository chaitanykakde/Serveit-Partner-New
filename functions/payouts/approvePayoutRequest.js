/**
 * Approve Payout Request
 * 
 * HTTPS Callable function for admin to approve payout requests
 * Creates payout transaction and updates settlement
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { requireAuth } = require("../guards/authGuard");
const { sendPayoutStatusNotification } = require("./notificationHelpers");

function createApprovePayoutRequestFunction(db, messaging, collections) {
  return functions.https.onCall(async (data, context) => {
    // TODO: Implement admin role verification
    requireAuth(context);

    const { requestId, notes } = data;

    if (!requestId) {
      throw new functions.https.HttpsError('invalid-argument', 'requestId is required');
    }

    try {
      await db.runTransaction(async (transaction) => {
        // Get the payout request
        const requestRef = db.collection(collections.payoutRequests).doc(requestId);
        const requestDoc = await transaction.get(requestRef);

        if (!requestDoc.exists) {
          throw new functions.https.HttpsError('not-found', 'Payout request not found');
        }

        const requestData = requestDoc.data();

        if (requestData.requestStatus !== 'PENDING') {
          throw new functions.https.HttpsError('failed-precondition', 'Request is not in PENDING status');
        }

        // Update payout request status
        transaction.update(requestRef, {
          requestStatus: 'APPROVED',
          processedAt: admin.firestore.FieldValue.serverTimestamp(),
          notes: notes || '',
          processedBy: context.auth.uid
        });

        // Update settlement paid amount
        if (requestData.settlementId) {
          const settlementRef = db.collection(collections.monthlySettlements).doc(requestData.settlementId);
          const settlementDoc = await transaction.get(settlementRef);

          if (settlementDoc.exists) {
            const settlementData = settlementDoc.data();
            const newPaidAmount = (settlementData.paidAmount || 0) + requestData.requestedAmount;
            const newPendingAmount = settlementData.partnerShare - newPaidAmount;

            transaction.update(settlementRef, {
              paidAmount: newPaidAmount,
              pendingAmount: newPendingAmount,
              updatedAt: admin.firestore.FieldValue.serverTimestamp()
            });
          }
        }

        // Create payout transaction record
        const transactionRef = db.collection(collections.payoutTransactions).doc();
        transaction.set(transactionRef, {
          transactionId: transactionRef.id,
          partnerId: requestData.partnerId,
          payoutRequestId: requestId,
          amount: requestData.requestedAmount,
          bankAccountId: requestData.bankAccountId,
          paymentMethod: 'BANK_TRANSFER',
          status: 'PENDING', // Will be updated by payment processor
          processedAt: admin.firestore.FieldValue.serverTimestamp(),
          fees: 0, // Bank transfer fees
          notes: notes || 'Approved by admin'
        });
      });

      // Send notification to partner
      await sendPayoutStatusNotification(db, messaging, collections, requestId, 'APPROVED');

      return { success: true, message: 'Payout request approved successfully' };
    } catch (error) {
      console.error('Error approving payout request:', error);
      if (error instanceof functions.https.HttpsError) {
        throw error;
      }
      throw new functions.https.HttpsError('internal', 'Failed to approve payout request');
    }
  });
}

module.exports = { createApprovePayoutRequestFunction };

