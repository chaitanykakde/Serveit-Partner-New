/**
 * Serveit Firebase Cloud Functions
 * 
 * PRODUCTION-READY BACKEND ROUTER
 * 
 * This file serves as a router only - all business logic is in domain modules.
 * 
 * Structure:
 * - env/          : Environment configuration
 * - guards/       : Authentication and authorization guards
 * - jobs/         : Job dispatch and acceptance
 * - payouts/      : Financial operations and settlements
 * - calls/        : Voice calling (Agora)
 * - notifications/: FCM notifications
 * - providers/    : Provider profile management
 * - shared/       : Shared utilities
 * - _dev_only/    : Debug/test functions (not exported in production)
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");

// Initialize Firebase Admin SDK (ONCE)
admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

// Load environment configuration
const config = require("./env/config");
const collections = config.getCollectionNames();

// Environment flag for conditional exports
const isProduction = config.isProduction();

// ═══════════════════════════════════════════════════════════
// JOBS DOMAIN
// ═══════════════════════════════════════════════════════════

const { createDispatchJobToProvidersFunction } = require("./jobs/dispatchJobToProviders");
const { createAcceptJobRequestFunction } = require("./jobs/acceptJobRequest");
const { createSyncInboxStatusFunction } = require("./jobs/syncInboxStatus");
const { createNotifyCustomerOnStatusChangeFunction } = require("./jobs/notifyCustomerOnStatusChange");
const { createSendJobNotificationFunction } = require("./jobs/sendJobNotification");

exports.dispatchJobToProviders = createDispatchJobToProvidersFunction(db, messaging, config, collections);
exports.acceptJobRequest = createAcceptJobRequestFunction(db, collections);
exports.syncInboxStatus = createSyncInboxStatusFunction(db);
exports.notifyCustomerOnStatusChange = createNotifyCustomerOnStatusChangeFunction(db, messaging);
exports.sendJobNotification = createSendJobNotificationFunction(db, messaging);

// ═══════════════════════════════════════════════════════════
// PAYOUTS DOMAIN
// ═══════════════════════════════════════════════════════════

const { createGetPayoutStatisticsFunction } = require("./payouts/getPayoutStatistics");
const { createGetTransactionDetailsFunction } = require("./payouts/getTransactionDetails");
const { createGetPayoutRequestsFunction } = require("./payouts/getPayoutRequests");
const { createGetPendingPayoutTransactionsFunction } = require("./payouts/getPendingPayoutTransactions");
const { createAggregateMonthlySettlementsFunction } = require("./payouts/aggregateMonthlySettlements");
const { createRecalculateSettlementsFunction } = require("./payouts/recalculateSettlements");
const { createApprovePayoutRequestFunction } = require("./payouts/approvePayoutRequest");
const { createRejectPayoutRequestFunction } = require("./payouts/rejectPayoutRequest");
const { createCompletePayoutFunction } = require("./payouts/completePayout");
const { createGeneratePaymentReceiptFunction } = require("./payouts/receipts");
const { createSendDailyEarningsSummaryFunction } = require("./payouts/sendDailyEarningsSummary");

exports.getPayoutStatistics = createGetPayoutStatisticsFunction(db);
exports.getTransactionDetails = createGetTransactionDetailsFunction(db);
exports.getPayoutRequests = createGetPayoutRequestsFunction(db);
exports.getPendingPayoutTransactions = createGetPendingPayoutTransactionsFunction(db);
exports.aggregateMonthlySettlements = createAggregateMonthlySettlementsFunction(db, config, collections);
exports.recalculateSettlements = createRecalculateSettlementsFunction(db, collections);
exports.approvePayoutRequest = createApprovePayoutRequestFunction(db, messaging, collections);
exports.rejectPayoutRequest = createRejectPayoutRequestFunction(db, messaging, collections);
exports.completePayout = createCompletePayoutFunction(db, messaging, collections);
exports.generatePaymentReceipt = createGeneratePaymentReceiptFunction(db, collections);
exports.sendDailyEarningsSummary = createSendDailyEarningsSummaryFunction(db, messaging);

// ═══════════════════════════════════════════════════════════
// CALLS DOMAIN
// ═══════════════════════════════════════════════════════════

const { createGenerateAgoraTokenFunction } = require("./calls/generateAgoraToken");
const { createValidateCallPermissionFunction } = require("./calls/validateCallPermission");
const { createEndCallFunction } = require("./calls/endCall");

exports.generateAgoraToken = createGenerateAgoraTokenFunction(db, config, collections);
exports.validateCallPermission = createValidateCallPermissionFunction(db, messaging, collections);
exports.endCall = createEndCallFunction(db);

// ═══════════════════════════════════════════════════════════
// NOTIFICATIONS DOMAIN
// ═══════════════════════════════════════════════════════════

const { createSendCustomNotificationFunction } = require("./notifications/sendCustomNotification");

exports.sendCustomNotification = createSendCustomNotificationFunction(db, messaging);

// ═══════════════════════════════════════════════════════════
// PROVIDERS DOMAIN
// ═══════════════════════════════════════════════════════════

const { createSendVerificationNotificationFunction } = require("./providers/sendVerificationNotification");
const { createSendProfileStatusNotificationFunction } = require("./providers/sendProfileStatusNotification");

exports.sendVerificationNotification = createSendVerificationNotificationFunction(db, messaging);
exports.sendProfileStatusNotification = createSendProfileStatusNotificationFunction(db, messaging);

// ═══════════════════════════════════════════════════════════
// DEV/TEST FUNCTIONS (Conditional Export)
// ═══════════════════════════════════════════════════════════

if (!isProduction) {
  const { createCheckBookingFunction } = require("./_dev_only/functions/checkBooking");
  const { createUpdateBookingProviderFunction } = require("./_dev_only/functions/updateBookingProvider");
  const { createAcceptBookingFunction } = require("./_dev_only/functions/acceptBooking");
  
  exports.checkBooking = createCheckBookingFunction(db);
  exports.updateBookingProvider = createUpdateBookingProviderFunction(db);
  exports.acceptBooking = createAcceptBookingFunction(db, collections);
  
  console.log("⚠️  DEV MODE: Debug/test functions exported (checkBooking, updateBookingProvider, acceptBooking)");
} else {
  console.log("✅ PRODUCTION MODE: Debug/test functions excluded");
}
