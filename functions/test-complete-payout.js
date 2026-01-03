// Test script to complete payouts and check notifications
const admin = require('firebase-admin');

// Initialize Firebase Admin SDK
if (!admin.apps.length) {
  // Use default credentials from Firebase CLI
  admin.initializeApp({
    projectId: 'serveit-1f333'
  });
}

const db = admin.firestore();

async function checkPendingPayouts() {
  try {
    console.log('🔍 Checking for pending payout transactions...');

    const pendingTransactions = await db.collection('payoutTransactions')
      .where('status', '==', 'PENDING')
      .limit(5)
      .get();

    console.log(`Found ${pendingTransactions.size} pending transactions`);

    if (pendingTransactions.size > 0) {
      pendingTransactions.forEach((doc) => {
        const data = doc.data();
        console.log(`📋 Transaction ${doc.id}:`);
        console.log(`   Partner: ${data.partnerId}`);
        console.log(`   Amount: ₹${data.amount}`);
        console.log(`   Status: ${data.status}`);
        console.log('');
      });
      return pendingTransactions.docs[0]; // Return first pending transaction
    } else {
      console.log('No pending transactions found. Let me check approved ones...');

      const approvedTransactions = await db.collection('payoutTransactions')
        .where('status', '==', 'APPROVED')
        .limit(5)
        .get();

      console.log(`Found ${approvedTransactions.size} approved transactions`);

      approvedTransactions.forEach((doc) => {
        const data = doc.data();
        console.log(`📋 Transaction ${doc.id}:`);
        console.log(`   Partner: ${data.partnerId}`);
        console.log(`   Amount: ₹${data.amount}`);
        console.log(`   Status: ${data.status}`);
        console.log('');
      });

      if (approvedTransactions.size > 0) {
        return approvedTransactions.docs[0]; // Return first approved transaction
      }
    }

    return null;

  } catch (error) {
    console.error('Error:', error);
    return null;
  }
}

async function completePayout(transactionId, partnerId, amount) {
  try {
    console.log(`💰 Completing payout ${transactionId} for partner ${partnerId} (₹${amount})...`);

    await db.runTransaction(async (transaction) => {
      // Get the payout transaction
      const transactionRef = db.collection('payoutTransactions').doc(transactionId);
      const transactionDoc = await transaction.get(transactionRef);

      if (!transactionDoc.exists) {
        throw new Error('Transaction not found');
      }

      const transactionData = transactionDoc.data();

      if (transactionData.status === 'COMPLETED') {
        throw new Error('Transaction is already completed');
      }

      // Update transaction status
      transaction.update(transactionRef, {
        status: 'COMPLETED',
        paymentMethod: 'CASH',
        completedAt: admin.firestore.FieldValue.serverTimestamp(),
        completedBy: 'admin-test-script',
        notes: `Test completion: Paid ₹${amount} in cash to partner ${partnerId}`
      });

      // Update payout request status if exists
      if (transactionData.payoutRequestId) {
        const requestRef = db.collection('payoutRequests').doc(transactionData.payoutRequestId);
        transaction.update(requestRef, {
          requestStatus: 'COMPLETED',
          processedAt: admin.firestore.FieldValue.serverTimestamp()
        });
      }

      // Update settlement paid amount
      if (transactionData.settlementId) {
        const settlementRef = db.collection('monthlySettlements').doc(transactionData.settlementId);
        const settlementDoc = await transaction.get(settlementRef);

        if (settlementDoc.exists) {
          const settlementData = settlementDoc.data();
          const newPaidAmount = (settlementData.paidAmount || 0) + transactionData.amount;
          const newPendingAmount = settlementData.partnerShare - newPaidAmount;

          transaction.update(settlementRef, {
            paidAmount: newPaidAmount,
            pendingAmount: newPendingAmount,
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
          });
        }
      }
    });

    console.log('✅ Payout completed successfully!');

    // Send notification to partner
    await sendPayoutNotification(partnerId, amount, transactionId);

    return true;

  } catch (error) {
    console.error('❌ Error completing payout:', error.message);
    return false;
  }
}

async function sendPayoutNotification(partnerId, amount, transactionId) {
  try {
    console.log(`📱 Sending notification to partner ${partnerId}...`);

    // Get partner's FCM tokens
    const tokensSnapshot = await db.collection('partners')
      .doc(partnerId)
      .collection('fcmTokens')
      .get();

    if (tokensSnapshot.empty) {
      console.log('⚠️ No FCM tokens found for partner');
      return;
    }

    const tokens = tokensSnapshot.docs.map(doc => doc.data().token);

    // Create notification message
    const message = {
      notification: {
        title: '✅ Payment Completed!',
        body: `Your payout of ₹${amount} has been completed successfully. Cash payment processed.`
      },
      data: {
        type: 'payout_status_update',
        transactionId: transactionId,
        status: 'COMPLETED',
        amount: amount.toString()
      },
      tokens: tokens
    };

    // Send notification
    const response = await admin.messaging().sendEachForMulticast(message);
    console.log(`📤 Sent notifications: ${response.successCount} success, ${response.failureCount} failed`);

    // Store notification in database
    await db.collection('partners').doc(partnerId).collection('notifications').add({
      type: 'payout_completed',
      title: 'Payment Completed',
      message: `Your payout of ₹${amount} has been completed successfully. Cash payment processed.`,
      amount: amount,
      transactionId: transactionId,
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
      read: false
    });

    console.log('💾 Notification stored in database');

  } catch (error) {
    console.error('❌ Error sending notification:', error);
  }
}

async function main() {
  console.log('🚀 Starting payout completion test...\n');

  // Check for pending payouts
  const transactionDoc = await checkPendingPayouts();

  if (!transactionDoc) {
    console.log('❌ No transactions found to complete');
    return;
  }

  const transactionData = transactionDoc.data();

  // Complete the payout
  const success = await completePayout(
    transactionDoc.id,
    transactionData.partnerId,
    transactionData.amount
  );

  if (success) {
    console.log('\n🎉 Payout completion test successful!');
    console.log('📱 Check the service provider app for notifications');
    console.log('🔍 Verify the transaction status changed to COMPLETED');
  } else {
    console.log('\n❌ Payout completion failed');
  }
}

// Run the test
main().catch(console.error);
