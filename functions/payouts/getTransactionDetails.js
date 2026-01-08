/**
 * Get Transaction Details
 * HTTPS Callable function to get detailed transaction information
 * 
 * Read-only function - safe to migrate
 */

const functions = require("firebase-functions");

function createGetTransactionDetailsFunction(db) {
  return functions.https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError('unauthenticated', 'Authentication required');
    }

    const { transactionId } = data;

    if (!transactionId) {
      throw new functions.https.HttpsError('invalid-argument', 'transactionId is required');
    }

    try {
      // Get transaction details
      const transactionDoc = await db.collection('payoutTransactions').doc(transactionId).get();
      if (!transactionDoc.exists) {
        throw new functions.https.HttpsError('not-found', 'Transaction not found');
      }

      const transactionData = transactionDoc.data();

      // Verify ownership
      if (transactionData.partnerId !== context.auth.uid) {
        throw new functions.https.HttpsError('permission-denied', 'Access denied');
      }

      // Get related data
      const [payoutRequestDoc, bankAccountDoc, partnerDoc] = await Promise.all([
        transactionData.payoutRequestId ? db.collection('payoutRequests').doc(transactionData.payoutRequestId).get() : Promise.resolve(null),
        transactionData.bankAccountId ? db.collection('bankAccounts').doc(transactionData.bankAccountId).get() : Promise.resolve(null),
        db.collection('partners').doc(transactionData.partnerId).get()
      ]);

      const result = {
        transaction: {
          id: transactionDoc.id,
          ...transactionData,
          processedAt: transactionData.processedAt?.toDate?.() || transactionData.processedAt
        }
      };

      if (payoutRequestDoc?.exists) {
        result.payoutRequest = {
          id: payoutRequestDoc.id,
          ...payoutRequestDoc.data(),
          requestedAt: payoutRequestDoc.data().requestedAt?.toDate?.() || payoutRequestDoc.data().requestedAt,
          processedAt: payoutRequestDoc.data().processedAt?.toDate?.() || payoutRequestDoc.data().processedAt
        };
      }

      if (bankAccountDoc?.exists) {
        result.bankAccount = {
          id: bankAccountDoc.id,
          ...bankAccountDoc.data()
        };
      }

      if (partnerDoc?.exists) {
        const partnerData = partnerDoc.data();
        result.partner = {
          id: partnerDoc.id,
          fullName: partnerData.personalDetails?.fullName || partnerData.fullName || 'Unknown',
          mobileNo: partnerData.personalDetails?.mobileNo || partnerData.mobileNo || '',
          email: partnerData.email || ''
        };
      }

      return result;

    } catch (error) {
      console.error('Error getting transaction details:', error);
      if (error instanceof functions.https.HttpsError) {
        throw error;
      }
      throw new functions.https.HttpsError('internal', 'Failed to get transaction details');
    }
  });
}

module.exports = { createGetTransactionDetailsFunction };

