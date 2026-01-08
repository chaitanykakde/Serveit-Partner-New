/**
 * ADMIN PAYOUT DASHBOARD API - Get Payout Statistics
 * HTTPS Callable function to get aggregated payout statistics
 * 
 * Read-only function - safe to migrate
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");

function createGetPayoutStatisticsFunction(db) {
  return functions.https.onCall(async (data, context) => {
    // TODO: Implement admin role verification
    if (!context.auth) {
      throw new functions.https.HttpsError('unauthenticated', 'Authentication required');
    }

    try {
      // Get counts by status
      const statusCounts = {};
      const statusSnapshot = await db.collection('payoutRequests')
        .select('requestStatus')
        .get();

      statusSnapshot.forEach(doc => {
        const status = doc.data().requestStatus;
        statusCounts[status] = (statusCounts[status] || 0) + 1;
      });

      // Get total amounts by status
      const amountStats = {};
      const amountSnapshot = await db.collection('payoutRequests').get();

      amountSnapshot.forEach(doc => {
        const data = doc.data();
        const status = data.requestStatus;
        const amount = data.requestedAmount || 0;

        if (!amountStats[status]) {
          amountStats[status] = { count: 0, total: 0 };
        }
        amountStats[status].count += 1;
        amountStats[status].total += amount;
      });

      // Get recent activity (last 30 days)
      const thirtyDaysAgo = new Date();
      thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);

      const recentSnapshot = await db.collection('payoutRequests')
        .where('requestedAt', '>=', admin.firestore.Timestamp.fromDate(thirtyDaysAgo))
        .orderBy('requestedAt', 'desc')
        .limit(10)
        .get();

      const recentActivity = recentSnapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data(),
        requestedAt: doc.data().requestedAt?.toDate?.() || doc.data().requestedAt
      }));

      return {
        success: true,
        statistics: {
          statusCounts,
          amountStats,
          recentActivity,
          totalRequests: amountSnapshot.size
        }
      };
    } catch (error) {
      console.error('Error fetching payout statistics:', error);
      throw new functions.https.HttpsError('internal', 'Failed to fetch payout statistics');
    }
  });
}

module.exports = { createGetPayoutStatisticsFunction };

