/**
 * Firestore Collection References
 * 
 * Centralized collection name management
 */

function createFirestoreRefs(db, collections) {
  return {
    bookings: () => db.collection(collections.bookings),
    partners: () => db.collection(collections.partners),
    providers: () => db.collection(collections.providers),
    serveitUsers: () => db.collection(collections.serveitUsers),
    jobInbox: (providerId) => db.collection(collections.jobInbox).doc(providerId).collection("jobs"),
    callLogs: () => db.collection(collections.callLogs),
    activeCalls: () => db.collection(collections.activeCalls),
    earnings: () => db.collection(collections.earnings),
    payoutRequests: () => db.collection(collections.payoutRequests),
    payoutTransactions: () => db.collection(collections.payoutTransactions),
    monthlySettlements: () => db.collection(collections.monthlySettlements),
    bankAccounts: () => db.collection(collections.bankAccounts)
  };
}

module.exports = { createFirestoreRefs };

