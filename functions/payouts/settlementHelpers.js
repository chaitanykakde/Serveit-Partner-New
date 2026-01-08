/**
 * Settlement Helper Functions
 * 
 * Shared logic for processing partner settlements
 */

const admin = require("firebase-admin");

/**
 * Process settlement for a single partner
 */
async function processPartnerSettlement(db, collections, partnerId, bookings, yearMonth, batch) {
  // Calculate settlement totals
  const totalEarnings = bookings.reduce((sum, booking) => sum + (booking.amount || 0), 0);
  const platformFees = bookings.reduce((sum, booking) => sum + (booking.platformFee || 0), 0);
  const partnerShare = bookings.reduce((sum, booking) => {
    // Use partnerEarning if available, otherwise calculate from amount - platformFee
    return sum + (booking.partnerEarning || ((booking.amount || 0) - (booking.platformFee || 0)));
  }, 0);

  const completedJobs = bookings.length;

  // Check if settlement already exists
  const existingSettlementQuery = db.collection(collections.monthlySettlements)
    .where('partnerId', '==', partnerId)
    .where('yearMonth', '==', yearMonth)
    .limit(1);

  const existingSettlementSnapshot = await existingSettlementQuery.get();

  const settlementData = {
    partnerId,
    yearMonth,
    totalEarnings,
    platformFees,
    partnerShare,
    completedJobs,
    paidAmount: 0, // Initially 0, updated when payouts are made
    pendingAmount: partnerShare, // Initially equals partner share
    settlementStatus: 'READY', // Ready for payout requests
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  };

  if (!existingSettlementSnapshot.empty) {
    // Update existing settlement
    const existingDoc = existingSettlementSnapshot.docs[0];
    const existingData = existingDoc.data();

    // Preserve paidAmount and pendingAmount from existing settlement
    const updatedData = {
      ...settlementData,
      paidAmount: existingData.paidAmount,
      pendingAmount: settlementData.partnerShare - existingData.paidAmount,
      settlementStatus: existingData.settlementStatus, // Preserve status
      createdAt: existingData.createdAt // Preserve original creation date
    };

    batch.update(existingDoc.ref, updatedData);
    console.log(`Updated settlement for partner ${partnerId} (${yearMonth}): ₹${partnerShare}`);
  } else {
    // Create new settlement
    const newSettlementRef = db.collection(collections.monthlySettlements).doc();
    batch.set(newSettlementRef, {
      settlementId: newSettlementRef.id,
      ...settlementData
    });
    console.log(`Created settlement for partner ${partnerId} (${yearMonth}): ₹${partnerShare}`);
  }
}

/**
 * Recalculate settlement for a specific partner and month
 */
async function recalculatePartnerSettlement(db, collections, partnerId, yearMonth) {
  // Parse yearMonth (format: "2024-01")
  const [year, month] = yearMonth.split('-').map(Number);
  const startOfMonth = new Date(year, month - 1, 1);
  const endOfMonth = new Date(year, month, 0, 23, 59, 59);

  // Get all completed bookings for this partner in the month
  const bookingsQuery = db.collection(collections.bookings)
    .where('partnerId', '==', partnerId)
    .where('status', '==', 'COMPLETED')
    .where('completedAt', '>=', admin.firestore.Timestamp.fromDate(startOfMonth))
    .where('completedAt', '<=', admin.firestore.Timestamp.fromDate(endOfMonth));

  const bookingsSnapshot = await bookingsQuery.get();
  const bookings = bookingsSnapshot.docs.map(doc => ({
    bookingId: doc.id,
    ...doc.data()
  }));

  if (bookings.length > 0) {
    const batch = db.batch();
    await processPartnerSettlement(db, collections, partnerId, bookings, yearMonth, batch);
    await batch.commit();
  }
}

/**
 * Aggregate settlements for all partners in a specific month
 */
async function aggregateMonthlySettlementsForMonth(db, collections, yearMonth) {
  // Parse yearMonth
  const [year, month] = yearMonth.split('-').map(Number);
  const startOfMonth = new Date(year, month - 1, 1);
  const endOfMonth = new Date(year, month, 0, 23, 59, 59);

  // Get all completed bookings for the month
  const bookingsQuery = db.collection(collections.bookings)
    .where('status', '==', 'COMPLETED')
    .where('completedAt', '>=', admin.firestore.Timestamp.fromDate(startOfMonth))
    .where('completedAt', '<=', admin.firestore.Timestamp.fromDate(endOfMonth));

  const bookingsSnapshot = await bookingsQuery.get();
  const batch = db.batch();

  // Group by partner and process
  const partnerBookings = new Map();

  bookingsSnapshot.forEach(doc => {
    const booking = { bookingId: doc.id, ...doc.data() };
    const partnerId = booking.partnerId;

    if (!partnerBookings.has(partnerId)) {
      partnerBookings.set(partnerId, []);
    }
    partnerBookings.get(partnerId).push(booking);
  });

  const settlementPromises = Array.from(partnerBookings.entries()).map(async ([partnerId, bookings]) => {
    await processPartnerSettlement(db, collections, partnerId, bookings, yearMonth, batch);
  });

  await Promise.all(settlementPromises);
  await batch.commit();
}

module.exports = {
  processPartnerSettlement,
  recalculatePartnerSettlement,
  aggregateMonthlySettlementsForMonth
};

