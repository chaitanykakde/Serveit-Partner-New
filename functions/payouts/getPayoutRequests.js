/**
 * ADMIN PAYOUT DASHBOARD API - Get Payout Requests
 * HTTPS Callable function for admin dashboard to fetch payout requests
 * 
 * Read-only function - safe to migrate
 */

const functions = require("firebase-functions");

function createGetPayoutRequestsFunction(db) {
  return functions.https.onCall(async (data, context) => {
    // TODO: Implement admin role verification
    if (!context.auth) {
      throw new functions.https.HttpsError('unauthenticated', 'Authentication required');
    }

    const { status, limit = 50, offset } = data || {};

    try {
      let query = db.collection('payoutRequests')
        .orderBy('requestedAt', 'desc')
        .limit(limit);

      if (status) {
        query = query.where('requestStatus', '==', status);
      }

      if (offset) {
        // For pagination, you'd typically use a document ID as offset
        query = query.startAfter(offset);
      }

      const snapshot = await query.get();
      const requests = [];

      for (const doc of snapshot.docs) {
        const requestData = doc.data();

        // Get bank account details
        let bankAccount = null;
        if (requestData.bankAccountId) {
          try {
            const bankAccountDoc = await db.collection('bankAccounts')
              .doc(requestData.bankAccountId)
              .get();
            if (bankAccountDoc.exists) {
              bankAccount = { id: bankAccountDoc.id, ...bankAccountDoc.data() };
            }
          } catch (error) {
            console.error('Error fetching bank account:', error);
          }
        }

        // Get settlement details
        let settlement = null;
        if (requestData.settlementId) {
          try {
            const settlementDoc = await db.collection('monthlySettlements')
              .doc(requestData.settlementId)
              .get();
            if (settlementDoc.exists) {
              settlement = { id: settlementDoc.id, ...settlementDoc.data() };
            }
          } catch (error) {
            console.error('Error fetching settlement:', error);
          }
        }

        // Get partner details
        let partner = null;
        try {
          const partnerDoc = await db.collection('partners')
            .doc(requestData.partnerId)
            .get();
          if (partnerDoc.exists) {
            const partnerData = partnerDoc.data();
            partner = {
              id: partnerDoc.id,
              fullName: partnerData.personalDetails?.fullName || partnerData.fullName || 'Unknown',
              mobileNo: partnerData.personalDetails?.mobileNo || partnerData.mobileNo || '',
              email: partnerData.email || ''
            };
          }
        } catch (error) {
          console.error('Error fetching partner:', error);
        }

        requests.push({
          id: doc.id,
          ...requestData,
          bankAccount,
          settlement,
          partner,
          requestedAt: requestData.requestedAt?.toDate?.() || requestData.requestedAt,
          processedAt: requestData.processedAt?.toDate?.() || requestData.processedAt
        });
      }

      return {
        success: true,
        requests,
        total: requests.length
      };
    } catch (error) {
      console.error('Error fetching payout requests:', error);
      throw new functions.https.HttpsError('internal', 'Failed to fetch payout requests');
    }
  });
}

module.exports = { createGetPayoutRequestsFunction };

