/**
 * Aggregate Monthly Settlements
 * 
 * Pub/Sub Scheduled function (runs monthly at 2 AM on 1st, Asia/Kolkata)
 * Aggregates completed bookings into monthly settlements for all partners
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { processPartnerSettlement } = require("./settlementHelpers");

function createAggregateMonthlySettlementsFunction(db, config, collections) {
  return functions
    .runWith({
      timeoutSeconds: 540, // 9 minutes (max allowed)
      memory: '1GB'
    })
    .pubsub.schedule('0 2 1 * *') // Run at 2 AM on the 1st of every month
    .timeZone(config.getTimezone())
    .onRun(async (context) => {
      const batch = db.batch();

      try {
        // Calculate target month (previous month)
        const now = new Date();
        const targetDate = new Date(now.getFullYear(), now.getMonth() - 1, 1);
        const yearMonth = `${targetDate.getFullYear()}-${String(targetDate.getMonth() + 1).padStart(2, '0')}`;

        console.log(`Starting monthly settlement aggregation for ${yearMonth}`);

        // Get all completed bookings for the target month
        const startOfMonth = new Date(targetDate.getFullYear(), targetDate.getMonth(), 1);
        const endOfMonth = new Date(targetDate.getFullYear(), targetDate.getMonth() + 1, 0, 23, 59, 59);

        const bookingsQuery = db.collection(collections.bookings)
          .where('status', '==', 'COMPLETED')
          .where('completedAt', '>=', admin.firestore.Timestamp.fromDate(startOfMonth))
          .where('completedAt', '<=', admin.firestore.Timestamp.fromDate(endOfMonth));

        const bookingsSnapshot = await bookingsQuery.get();

        if (bookingsSnapshot.empty) {
          console.log(`No completed bookings found for ${yearMonth}`);
          return null;
        }

        // Group bookings by partner
        const partnerBookings = new Map();

        bookingsSnapshot.forEach(doc => {
          const booking = { bookingId: doc.id, ...doc.data() };
          const partnerId = booking.partnerId;

          if (!partnerBookings.has(partnerId)) {
            partnerBookings.set(partnerId, []);
          }
          partnerBookings.get(partnerId).push(booking);
        });

        console.log(`Processing settlements for ${partnerBookings.size} partners`);

        // Process each partner's bookings
        const settlementPromises = Array.from(partnerBookings.entries()).map(async ([partnerId, bookings]) => {
          try {
            await processPartnerSettlement(db, collections, partnerId, bookings, yearMonth, batch);
          } catch (error) {
            console.error(`Error processing settlement for partner ${partnerId}:`, error);
          }
        });

        await Promise.all(settlementPromises);

        // Commit all batch writes
        await batch.commit();

        console.log(`Successfully processed monthly settlements for ${yearMonth}`);
        return { success: true, partnersProcessed: partnerBookings.size, yearMonth };

      } catch (error) {
        console.error('Error in monthly settlement aggregation:', error);
        throw error;
      }
    });
}

module.exports = { createAggregateMonthlySettlementsFunction };

