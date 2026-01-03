// Test script to verify the earnings fix
// This will call the recalculateSettlements function to see if it finds completed jobs

const { initializeApp } = require('firebase/app');
const { getFunctions, httpsCallable } = require('firebase/functions');

// Your Firebase config - you'll need to add your actual config
const firebaseConfig = {
  // Add your Firebase config here from google-services.json
  apiKey: "your-api-key",
  authDomain: "serveit-1f333.firebaseapp.com",
  projectId: "serveit-1f333"
};

const app = initializeApp(firebaseConfig);
const functions = getFunctions(app);

async function testEarningsFix() {
  try {
    console.log('🧪 Testing earnings fix...');
    console.log('📞 Calling recalculateSettlements function for December 2024...');

    const recalculateSettlements = httpsCallable(functions, 'recalculateSettlements');

    // Test for December 2024 (adjust based on your data)
    const result = await recalculateSettlements({
      yearMonth: '2024-12'
    });

    console.log('✅ Function call successful!');
    console.log('📊 Result:', JSON.stringify(result.data, null, 2));

    if (result.data.success) {
      console.log('🎉 SUCCESS: Settlements were recalculated!');
      console.log('💰 Check Firebase Console → Firestore → monthlySettlements collection');
      console.log('📱 Test the Android app Earnings screen - it should now show data');
    } else {
      console.log('⚠️ Function completed but may not have found jobs');
      console.log('🔍 Check Firebase Console → Bookings collection for completed jobs');
    }

  } catch (error) {
    console.error('❌ Error testing earnings fix:', error);

    if (error.code === 'functions/unauthenticated') {
      console.log('🔐 Authentication required. Please sign in to Firebase first.');
    } else if (error.code === 'functions/permission-denied') {
      console.log('🚫 Permission denied. Check Firebase security rules.');
    } else {
      console.log('🐛 Unexpected error. Check function logs in Firebase Console.');
    }
  }
}

testEarningsFix();
