/**
 * Complete Payout
 * 
 * HTTPS Callable function for admin to mark payout as completed (cash paid)
 * Updates transaction status and settlement
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { requireAuth } = require("../guards/authGuard");
const { sendPayoutStatusNotification } = require("./notificationHelpers");

function createCompletePayoutFunction(db, messaging, collections) {
  return functions.https.onCall(async (data, context) => {
    // TODO: Implement admin role verification
    requireAuth(context);

    const { transactionId, paymentMethod = 'CASH', notes } = data;

    if (!transactionId) {
      throw new functions.https.HttpsError('invalid-argument', 'transactionId is required');
    }

    try {
      await db.runTransaction(async (transaction) => {
        // Get the payout transaction
        const transactionRef = db.collection(collections.payoutTransactions).doc(transactionId);
        const transactionDoc = await transaction.get(transactionRef);

        if (!transactionDoc.exists) {
          throw new functions.https.HttpsError('not-found', 'Transaction not found');
        }

        const transactionData = transactionDoc.data();

        if (transactionData.status === 'COMPLETED') {
          throw new functions.https.HttpsError('failed-precondition', 'Transaction is already completed');
        }

        // Update transaction status
        transaction.update(transactionRef, {
          status: 'COMPLETED',
          paymentMethod: paymentMethod,
          completedAt: admin.firestore.FieldValue.serverTimestamp(),
          completedBy: context.auth.uid,
          notes: notes || 'Payment completed successfully'
        });

        // Update payout request status if exists
        if (transactionData.payoutRequestId) {
          const requestRef = db.collection(collections.payoutRequests).doc(transactionData.payoutRequestId);
          transaction.update(requestRef, {
            requestStatus: 'COMPLETED',
            processedAt: admin.firestore.FieldValue.serverTimestamp()
          });
        }

        // Update settlement paid amount
        if (transactionData.settlementId) {
          const settlementRef = db.collection(collections.monthlySettlements).doc(transactionData.settlementId);
          const settlementDoc = await transaction.get(settlementRef);

          if (settlementDoc.exists) {
            const settlementData = settlementDoc.data();
            const newPaidAmount = (settlementData.paidAmount || 0) + transactionData.amount;
            const newPendingAmount = settlementData.partnerShare - newPaidAmount;

            transaction.update(settlementRef, {
              paidAmount: newPaidAmount,
              pendingAmount: newPendingAmount,
              updatedAt: admin.firestore.FieldValue.serverTimestamp()
            });
          }
        }
      });

      // Generate receipt automatically
      try {
        // Note: Receipt generation is handled separately via generatePaymentReceipt
        // This is just a placeholder - actual receipt generation should be called separately
      } catch (receiptError) {
        console.error('Error generating receipt:', receiptError);
        // Don't fail the payout completion if receipt generation fails
      }

      // Send notification to partner
      await sendPayoutStatusNotification(db, messaging, collections, null, 'COMPLETED', '', transactionId);

      return {
        success: true,
        message: 'Payout completed successfully',
        transactionId: transactionId
      };

    } catch (error) {
      console.error('Error completing payout:', error);
      if (error instanceof functions.https.HttpsError) {
        throw error;
      }
      throw new functions.https.HttpsError('internal', 'Failed to complete payout');
    }
  });
}

module.exports = { createCompletePayoutFunction };

