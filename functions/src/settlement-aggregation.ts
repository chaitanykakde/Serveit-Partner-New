import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import { Timestamp } from 'firebase-admin/firestore';

interface Booking {
  bookingId: string;
  partnerId: string;
  serviceName: string;
  amount: number;
  platformFee: number;
  partnerEarning?: number;
  status: string;
  completedAt: Timestamp;
}

interface MonthlySettlement {
  settlementId: string;
  partnerId: string;
  yearMonth: string; // Format: "2024-01"
  totalEarnings: number;
  platformFees: number;
  partnerShare: number;
  completedJobs: number;
  paidAmount: number;
  pendingAmount: number;
  settlementStatus: 'PENDING' | 'READY' | 'REQUESTED' | 'PROCESSING' | 'SETTLED' | 'FAILED';
  createdAt: Timestamp;
  updatedAt: Timestamp;
}

/**
 * Cloud Function to aggregate monthly settlements from completed bookings
 * Runs periodically (e.g., 1st of each month) to calculate partner earnings
 */
export const aggregateMonthlySettlements = functions
  .runWith({
    timeoutSeconds: 540, // 9 minutes (max allowed)
    memory: '1GB'
  })
  .pubsub.schedule('0 2 1 * *') // Run at 2 AM on the 1st of every month
  .timeZone('Asia/Kolkata')
  .onRun(async (context) => {
    const db = admin.firestore();
    const batch = db.batch();

    try {
      // Calculate target month (previous month)
      const now = new Date();
      const targetDate = new Date(now.getFullYear(), now.getMonth() - 1, 1);
      const yearMonth = `${targetDate.getFullYear()}-${String(targetDate.getMonth() + 1).padStart(2, '0')}`;

      console.log(`Starting monthly settlement aggregation for ${yearMonth}`);

      // Get all Bookings documents (jobs are stored in nested bookings[] arrays)
      const bookingsSnapshot = await db.collection('Bookings').get();

      if (bookingsSnapshot.empty) {
        console.log(`No booking documents found`);
        return null;
      }

      // Extract all completed jobs from nested arrays
      const completedJobs: Booking[] = [];

      bookingsSnapshot.forEach(doc => {
        const docData = doc.data();

        // Handle both single booking and array format
        let bookingsArray: any[] = [];
        if (docData.bookings && Array.isArray(docData.bookings)) {
          bookingsArray = docData.bookings;
        } else if (docData.bookingId) {
          // Single booking format
          bookingsArray = [docData];
        }

        // Filter for completed jobs in the target month
        bookingsArray.forEach((booking: any) => {
          if (booking.status === 'completed' && booking.acceptedByProviderId) {
            const completedAt = booking.acceptedAt || booking.completedAt;
            if (completedAt) {
              let completionDate: Date;

              // Handle both Timestamp and Date formats
              if (completedAt.toDate) {
                completionDate = completedAt.toDate();
              } else if (completedAt instanceof Date) {
                completionDate = completedAt;
              } else {
                return; // Skip if no valid date
              }

              // Check if job was completed in target month
              const jobYearMonth = `${completionDate.getFullYear()}-${String(completionDate.getMonth() + 1).padStart(2, '0')}`;
              if (jobYearMonth === yearMonth) {
                completedJobs.push({
                  bookingId: booking.bookingId || doc.id,
                  partnerId: booking.acceptedByProviderId,
                  serviceName: booking.serviceName || 'Service',
                  amount: booking.totalPrice || booking.price || 0,
                  platformFee: booking.platformFee || (booking.totalPrice ? booking.totalPrice * 0.1 : 0), // 10% platform fee
                  partnerEarning: booking.partnerEarning || (booking.totalPrice ? booking.totalPrice * 0.9 : 0), // 90% partner share
                  status: 'COMPLETED',
                  completedAt: Timestamp.fromDate(completionDate)
                });
              }
            }
          }
        });
      });

      if (completedJobs.length === 0) {
        console.log(`No completed jobs found for ${yearMonth}`);
        return null;
      }

      // Group bookings by partner
      const partnerBookings = new Map<string, Booking[]>();

      completedJobs.forEach(booking => {
        const partnerId = booking.partnerId;

        if (!partnerBookings.has(partnerId)) {
          partnerBookings.set(partnerId, []);
        }
        partnerBookings.get(partnerId)!.push(booking);
      });

      console.log(`Processing settlements for ${partnerBookings.size} partners`);

      // Process each partner's bookings
      const settlementPromises = Array.from(partnerBookings.entries()).map(async ([partnerId, bookings]) => {
        try {
          await processPartnerSettlement(partnerId, bookings, yearMonth, batch);
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

/**
 * Process settlement for a single partner
 */
async function processPartnerSettlement(
  partnerId: string,
  bookings: Booking[],
  yearMonth: string,
  batch: admin.firestore.WriteBatch
) {
  const db = admin.firestore();

  // Calculate settlement totals
  const totalEarnings = bookings.reduce((sum, booking) => sum + booking.amount, 0);
  const platformFees = bookings.reduce((sum, booking) => sum + (booking.platformFee || 0), 0);
  const partnerShare = bookings.reduce((sum, booking) => {
    // Use partnerEarning if available, otherwise calculate from amount - platformFee
    return sum + (booking.partnerEarning || (booking.amount - (booking.platformFee || 0)));
  }, 0);

  const completedJobs = bookings.length;

  // Check if settlement already exists
  const existingSettlementQuery = db.collection('monthlySettlements')
    .where('partnerId', '==', partnerId)
    .where('yearMonth', '==', yearMonth)
    .limit(1);

  const existingSettlementSnapshot = await existingSettlementQuery.get();

  const settlementData: Omit<MonthlySettlement, 'settlementId'> = {
    partnerId,
    yearMonth,
    totalEarnings,
    platformFees,
    partnerShare,
    completedJobs,
    paidAmount: 0, // Initially 0, updated when payouts are made
    pendingAmount: partnerShare, // Initially equals partner share
    settlementStatus: 'READY', // Ready for payout requests
    createdAt: Timestamp.now(),
    updatedAt: Timestamp.now()
  };

  if (!existingSettlementSnapshot.empty) {
    // Update existing settlement
    const existingDoc = existingSettlementSnapshot.docs[0];
    const existingData = existingDoc.data() as MonthlySettlement;

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
    const newSettlementRef = db.collection('monthlySettlements').doc();
    batch.set(newSettlementRef, {
      settlementId: newSettlementRef.id,
      ...settlementData
    });
    console.log(`Created settlement for partner ${partnerId} (${yearMonth}): ₹${partnerShare}`);
  }
}

/**
 * Manual trigger function for testing/admin purposes
 * Can be called via HTTP to recalculate settlements for specific periods
 */
export const recalculateSettlements = functions
  .runWith({
    timeoutSeconds: 300,
    memory: '512MB'
  })
  .https.onCall(async (data, context) => {
    // Verify admin access (implement your admin verification logic)
    if (!context.auth) {
      throw new functions.https.HttpsError('unauthenticated', 'Authentication required');
    }

    // For now, allow any authenticated user (implement admin role check)
    const db = admin.firestore();
    const { yearMonth, partnerId } = data || {};

    try {
      if (partnerId && yearMonth) {
        // Recalculate for specific partner and month
        await recalculatePartnerSettlement(partnerId, yearMonth);
        return { success: true, message: `Recalculated settlement for partner ${partnerId} (${yearMonth})` };
      } else if (yearMonth) {
        // Recalculate for all partners in specific month
        await aggregateMonthlySettlementsForMonth(yearMonth);
        return { success: true, message: `Recalculated settlements for ${yearMonth}` };
      } else {
        throw new functions.https.HttpsError('invalid-argument', 'Specify yearMonth and optionally partnerId');
      }
    } catch (error) {
      console.error('Error recalculating settlements:', error);
      throw new functions.https.HttpsError('internal', 'Failed to recalculate settlements');
    }
  });

/**
 * Recalculate settlement for a specific partner and month
 */
async function recalculatePartnerSettlement(partnerId: string, yearMonth: string) {
  const db = admin.firestore();

  // Parse yearMonth (format: "2024-01")
  const [year, month] = yearMonth.split('-').map(Number);

  // Get all Bookings documents and filter for this partner's completed jobs
  const bookingsSnapshot = await db.collection('Bookings').get();
  const partnerBookings: Booking[] = [];

  bookingsSnapshot.forEach(doc => {
    const docData = doc.data();

    // Handle both single booking and array format
    let bookingsArray: any[] = [];
    if (docData.bookings && Array.isArray(docData.bookings)) {
      bookingsArray = docData.bookings;
    } else if (docData.bookingId) {
      bookingsArray = [docData];
    }

    // Filter for this partner's completed jobs in the target month
    bookingsArray.forEach((booking: any) => {
      if (booking.status === 'completed' && booking.acceptedByProviderId === partnerId) {
        const completedAt = booking.acceptedAt || booking.completedAt;
        if (completedAt) {
          let completionDate: Date;

          if (completedAt.toDate) {
            completionDate = completedAt.toDate();
          } else if (completedAt instanceof Date) {
            completionDate = completedAt;
          } else {
            return;
          }

          const jobYearMonth = `${completionDate.getFullYear()}-${String(completionDate.getMonth() + 1).padStart(2, '0')}`;
          if (jobYearMonth === yearMonth) {
            partnerBookings.push({
              bookingId: booking.bookingId || doc.id,
              partnerId: booking.acceptedByProviderId,
              serviceName: booking.serviceName || 'Service',
              amount: booking.totalPrice || booking.price || 0,
              platformFee: booking.platformFee || (booking.totalPrice ? booking.totalPrice * 0.1 : 0),
              partnerEarning: booking.partnerEarning || (booking.totalPrice ? booking.totalPrice * 0.9 : 0),
              status: 'COMPLETED',
              completedAt: Timestamp.fromDate(completionDate)
            });
          }
        }
      }
    });
  });

  if (partnerBookings.length > 0) {
    const batch = db.batch();
    await processPartnerSettlement(partnerId, partnerBookings, yearMonth, batch);
    await batch.commit();
  }
}

/**
 * Aggregate settlements for all partners in a specific month
 */
async function aggregateMonthlySettlementsForMonth(yearMonth: string) {
  const db = admin.firestore();

  // Parse yearMonth
  const [year, month] = yearMonth.split('-').map(Number);

  // Get all Bookings documents and extract completed jobs for the month
  const bookingsSnapshot = await db.collection('Bookings').get();
  const partnerBookings = new Map<string, Booking[]>();

  bookingsSnapshot.forEach(doc => {
    const docData = doc.data();

    // Handle both single booking and array format
    let bookingsArray: any[] = [];
    if (docData.bookings && Array.isArray(docData.bookings)) {
      bookingsArray = docData.bookings;
    } else if (docData.bookingId) {
      bookingsArray = [docData];
    }

    // Process completed jobs for target month
    bookingsArray.forEach((booking: any) => {
      if (booking.status === 'completed' && booking.acceptedByProviderId) {
        const completedAt = booking.acceptedAt || booking.completedAt;
        if (completedAt) {
          let completionDate: Date;

          if (completedAt.toDate) {
            completionDate = completedAt.toDate();
          } else if (completedAt instanceof Date) {
            completionDate = completedAt;
          } else {
            return;
          }

          const jobYearMonth = `${completionDate.getFullYear()}-${String(completionDate.getMonth() + 1).padStart(2, '0')}`;
          if (jobYearMonth === yearMonth) {
            const partnerId = booking.acceptedByProviderId;
            if (!partnerBookings.has(partnerId)) {
              partnerBookings.set(partnerId, []);
            }

            partnerBookings.get(partnerId)!.push({
              bookingId: booking.bookingId || doc.id,
              partnerId: partnerId,
              serviceName: booking.serviceName || 'Service',
              amount: booking.totalPrice || booking.price || 0,
              platformFee: booking.platformFee || (booking.totalPrice ? booking.totalPrice * 0.1 : 0),
              partnerEarning: booking.partnerEarning || (booking.totalPrice ? booking.totalPrice * 0.9 : 0),
              status: 'COMPLETED',
              completedAt: Timestamp.fromDate(completionDate)
            });
          }
        }
      }
    });
  });

  if (partnerBookings.size > 0) {
    const batch = db.batch();
    const settlementPromises = Array.from(partnerBookings.entries()).map(async ([partnerId, bookings]) => {
      await processPartnerSettlement(partnerId, bookings, yearMonth, batch);
    });

    await Promise.all(settlementPromises);
    await batch.commit();
  }
}
