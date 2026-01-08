/**
 * Payout Notification Helpers
 * 
 * Shared notification logic for payout status updates
 */

const admin = require("firebase-admin");

/**
 * Send payout status notification to partner
 */
async function sendPayoutStatusNotification(db, messaging, collections, requestId, status, reason = '', transactionId = null) {
  try {
    let requestData = null;
    let partnerId = null;

    if (transactionId) {
      // Get data from transaction
      const transactionDoc = await db.collection(collections.payoutTransactions).doc(transactionId).get();
      if (transactionDoc.exists) {
        const transactionData = transactionDoc.data();
        partnerId = transactionData.partnerId;
        requestData = { amount: transactionData.amount };
      }
    } else if (requestId) {
      // Get data from payout request
      const requestDoc = await db.collection(collections.payoutRequests).doc(requestId).get();
      if (requestDoc.exists) {
        requestData = requestDoc.data();
        partnerId = requestData.partnerId;
      }
    }

    if (!partnerId) return;

    // Get partner FCM token
    const partnerDoc = await db.collection(collections.partners).doc(partnerId).get();
    if (!partnerDoc.exists) return;

    const partnerData = partnerDoc.data();
    const fcmToken = partnerData.fcmToken;
    if (!fcmToken) return;

    let title = '';
    let body = '';

    const amount = requestData?.requestedAmount || requestData?.amount || 0;

    switch (status) {
      case 'APPROVED':
        title = '💰 Payout Approved!';
        body = `Your payout request for ₹${amount} has been approved. Payment will be processed within 3-5 business days.`;
        break;
      case 'REJECTED':
        title = '❌ Payout Rejected';
        body = `Your payout request has been rejected. Reason: ${reason}`;
        break;
      case 'COMPLETED':
        title = '✅ Payment Completed!';
        body = `Your payout of ₹${amount} has been completed successfully. Cash payment processed.`;
        break;
      case 'FAILED':
        title = '⚠️ Payment Failed';
        body = `There was an issue processing your payout. Reason: ${reason}`;
        break;
    }

    // Send FCM notification
    const message = {
      notification: {
        title: title,
        body: body
      },
      data: {
        type: 'payout_status_update',
        requestId: requestId,
        transactionId: transactionId,
        status: status,
        amount: (requestData?.requestedAmount || requestData?.amount || 0).toString()
      },
      token: fcmToken,
      android: {
        priority: 'high',
        notification: {
          channelId: 'payout_notifications',
          priority: 'high'
        }
      }
    };

    await messaging.send(message);

    // Store notification in Firestore
    await db.collection(collections.partners).doc(partnerId)
      .collection('notifications').add({
        title: title,
        message: body,
        type: 'PAYOUT_STATUS_UPDATE',
        timestamp: Date.now(),
        isRead: false,
        relatedData: {
          requestId: requestId,
          transactionId: transactionId,
          status: status,
          amount: requestData?.requestedAmount || requestData?.amount || 0
        }
      });

    console.log(`Payout notification sent to partner ${partnerId}: ${status}`);
  } catch (error) {
    console.error('Error sending payout notification:', error);
  }
}

module.exports = {
  sendPayoutStatusNotification
};

