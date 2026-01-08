/**
 * Recalculate Settlements
 * 
 * HTTPS Callable function for manual recalculation of settlements
 * Can be called for specific partner/month or all partners in a month
 */

const functions = require("firebase-functions");
const { requireAuth } = require("../guards/authGuard");
const { recalculatePartnerSettlement, aggregateMonthlySettlementsForMonth } = require("./settlementHelpers");

function createRecalculateSettlementsFunction(db, collections) {
  return functions.https.onCall(async (data, context) => {
    // For now, allow any authenticated user (implement admin role check)
    requireAuth(context);

    const { yearMonth, partnerId } = data || {};

    try {
      if (partnerId && yearMonth) {
        // Recalculate for specific partner and month
        await recalculatePartnerSettlement(db, collections, partnerId, yearMonth);
        return { success: true, message: `Recalculated settlement for partner ${partnerId} (${yearMonth})` };
      } else if (yearMonth) {
        // Recalculate for all partners in specific month
        await aggregateMonthlySettlementsForMonth(db, collections, yearMonth);
        return { success: true, message: `Recalculated settlements for ${yearMonth}` };
      } else {
        throw new functions.https.HttpsError('invalid-argument', 'Specify yearMonth and optionally partnerId');
      }
    } catch (error) {
      console.error('Error recalculating settlements:', error);
      throw new functions.https.HttpsError('internal', 'Failed to recalculate settlements');
    }
  });
}

module.exports = { createRecalculateSettlementsFunction };

