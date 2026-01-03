// Check for pending payout transactions
const admin = require('firebase-admin');
const serviceAccount = require('./service-account-key.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: 'serveit-1f333'
});

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
    }

  } catch (error) {
    console.error('Error:', error);
  }
}

checkPendingPayouts();
