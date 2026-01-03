// Simple test to call recalculateSettlements function
// Run with: node test-recalculate.js

const { initializeApp } = require('firebase/app');
const { getFunctions, httpsCallable } = require('firebase/functions');

// Your Firebase config from google-services.json
const firebaseConfig = {
  apiKey: "your-api-key",
  authDomain: "serveit-1f333.firebaseapp.com",
  projectId: "serveit-1f333",
  storageBucket: "serveit-1f333.appspot.com",
  messagingSenderId: "your-sender-id",
  appId: "your-app-id"
};

const app = initializeApp(firebaseConfig);
const functions = getFunctions(app);

async function testRecalculate() {
  try {
    console.log('Testing recalculateSettlements...');

    const recalculateSettlements = httpsCallable(functions, 'recalculateSettlements');

    // Test for December 2024 (adjust month as needed)
    const result = await recalculateSettlements({
      yearMonth: '2024-12'
    });

    console.log('Success:', result.data);

  } catch (error) {
    console.error('Error:', error);
  }
}

testRecalculate();
