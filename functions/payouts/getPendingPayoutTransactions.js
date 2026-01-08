/**
 * ADMIN PAYOUT DASHBOARD API - Get Pending Payout Transactions
 * HTTPS Callable function to get transactions that need completion
 * 
 * Read-only function - safe to migrate
 */

const functions = require("firebase-functions");

function createGetPendingPayoutTransactionsFunction(db) {
  return functions.https.onCall(async (data, context) => {
    // TODO: Implement admin role verification
    if (!context.auth) {
      throw new functions.https.HttpsError('unauthenticated', 'Authentication required');
    }

    const { limit = 50, status = 'PENDING' } = data || {};

    try {
      const transactionsRef = db.collection('payoutTransactions')
        .where('status', '==', status)
        .orderBy('processedAt', 'desc')
        .limit(limit);

      const snapshot = await transactionsRef.get();
      const transactions = [];

      for (const doc of snapshot.docs) {
        const transactionData = doc.data();

        // Get related data
        let partner = null;
        let payoutRequest = null;

        try {
          // Get partner details
          const partnerDoc = await db.collection('partners').doc(transactionData.partnerId).get();
          if (partnerDoc.exists) {
            const partnerData = partnerDoc.data();
            partner = {
              id: partnerDoc.id,
              fullName: partnerData.personalDetails?.fullName || partnerData.fullName || 'Unknown',
              mobileNo: partnerData.personalDetails?.mobileNo || partnerData.mobileNo || '',
              email: partnerData.email || ''
            };
          }

          // Get payout request details if exists
          if (transactionData.payoutRequestId) {
            const requestDoc = await db.collection('payoutRequests').doc(transactionData.payoutRequestId).get();
            if (requestDoc.exists) {
              payoutRequest = {
                id: requestDoc.id,
                ...requestDoc.data(),
                requestedAt: requestDoc.data().requestedAt?.toDate?.() || requestDoc.data().requestedAt
              };
            }
          }
        } catch (error) {
          console.error('Error fetching related data:', error);
        }

        transactions.push({
          id: doc.id,
          ...transactionData,
          partner,
          payoutRequest,
          processedAt: transactionData.processedAt?.toDate?.() || transactionData.processedAt,
          completedAt: transactionData.completedAt?.toDate?.() || transactionData.completedAt
        });
      }

      return {
        success: true,
        transactions,
        total: transactions.length
      };

    } catch (error) {
      console.error('Error fetching pending transactions:', error);
      throw new functions.https.HttpsError('internal', 'Failed to fetch pending transactions');
    }
  });
}

module.exports = { createGetPendingPayoutTransactionsFunction };

