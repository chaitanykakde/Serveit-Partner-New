// Test the recalculateSettlements function
// Run with: node test-settlements.js

const { initializeApp } = require('firebase/app');
const { getFunctions, httpsCallable, connectFunctionsEmulator } = require('firebase/functions');

// Firebase config (replace with your actual config)
const firebaseConfig = {
  apiKey: "AIzaSyBXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
  authDomain: "serveit-1f333.firebaseapp.com",
  projectId: "serveit-1f333",
  storageBucket: "serveit-1f333.appspot.com",
  messagingSenderId: "123456789",
  appId: "1:123456789:android:XXXXXXXXXXXXXXXXXXXXXXXXXXXX"
};

const app = initializeApp(firebaseConfig);
const functions = getFunctions(app);

// Uncomment to use emulator
// connectFunctionsEmulator(functions, "localhost", 5001);

async function testRecalculateSettlements() {
  try {
    console.log('🧪 Testing recalculateSettlements function...');

    const recalculateSettlements = httpsCallable(functions, 'recalculateSettlements');

    console.log('📞 Calling recalculateSettlements for December 2024...');
    const result = await recalculateSettlements({
      yearMonth: '2024-12'
    });

    console.log('✅ Function call successful!');
    console.log('📊 Result:', JSON.stringify(result.data, null, 2));

    if (result.data && result.data.success) {
      console.log('🎉 SUCCESS: Settlements recalculated!');
      console.log('💰 Check Firebase Console → Firestore → monthlySettlements collection');
    } else {
      console.log('⚠️ Function completed but may not have found jobs');
      console.log('🔍 Check Firebase Console → Bookings collection for completed jobs');
    }

  } catch (error) {
    console.error('❌ Error:', error.message);

    if (error.code === 'functions/unauthenticated') {
      console.log('🔐 Authentication required. Please sign in to Firebase.');
      console.log('Run: firebase login');
    } else if (error.code === 'functions/permission-denied') {
      console.log('🚫 Permission denied. Check Firebase security rules.');
    }
  }
}

testRecalculateSettlements();
