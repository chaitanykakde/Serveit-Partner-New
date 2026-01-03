// Test script to call recalculateSettlements function
const admin = require('firebase-admin');

// Initialize Firebase Admin
const serviceAccount = require('./app/google-services.json'); // You'll need to create this or use environment variables

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: 'https://serveit-1f333.firebaseio.com'
});

const functions = admin.functions();

async function testRecalculateSettlements() {
  try {
    console.log('Testing recalculateSettlements function...');

    // Test for current month (or previous month if needed)
    const testData = {
      yearMonth: '2024-01', // Adjust this to match your test data
      partnerId: null // Test for all partners first
    };

    const result = await functions.httpsCallable('recalculateSettlements')(testData);
    console.log('Result:', result);

  } catch (error) {
    console.error('Error:', error);
  }
}

testRecalculateSettlements();
