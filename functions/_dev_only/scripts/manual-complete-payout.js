// Manual payout completion script
// Run this to complete a specific payout transaction

const transactionId = process.argv[2] || 'transaction_123'; // Pass transaction ID as argument
const partnerId = process.argv[3] || 'partner_456'; // Pass partner ID as argument
const amount = parseFloat(process.argv[4]) || 500; // Pass amount as argument

console.log(`🎯 Completing payout for transaction: ${transactionId}`);
console.log(`👤 Partner: ${partnerId}`);
console.log(`💰 Amount: ₹${amount}`);
console.log('');

console.log('📋 Manual steps to complete this payout:');
console.log('');

console.log('1️⃣ Open Firebase Console:');
console.log('   https://console.firebase.google.com/project/serveit-1f333/firestore/data');
console.log('');

console.log('2️⃣ Navigate to payoutTransactions collection');
console.log('');

console.log('3️⃣ Find and click on transaction document:');
console.log(`   Document ID: ${transactionId}`);
console.log('');

console.log('4️⃣ Update the document with these fields:');
console.log(`   status: \"COMPLETED\"`);
console.log(`   paymentMethod: \"CASH\"`);
console.log(`   completedAt: ${new Date().toISOString()}`);
console.log(`   completedBy: \"admin-manual\"`);
console.log(`   notes: \"Manual completion: Paid ₹${amount} in cash to partner ${partnerId}\"`);
console.log('');

console.log('5️⃣ Update monthlySettlements collection:');
console.log('   - Find settlement document linked to this transaction');
console.log(`   - Increase paidAmount by ₹${amount}`);
console.log(`   - Decrease pendingAmount by ₹${amount}`);
console.log('');

console.log('6️⃣ Check service provider app for:');
console.log('   ✅ Push notification: \"Payment Completed!\"');
console.log('   ✅ Payout history shows COMPLETED status');
console.log('   ✅ Updated earnings summary');
console.log('');

console.log('🔄 Alternative: Use Firebase Console to run this JavaScript:');
console.log(`
db.collection('payoutTransactions').doc('${transactionId}').update({
  status: 'COMPLETED',
  paymentMethod: 'CASH',
  completedAt: new Date(),
  completedBy: 'admin-manual',
  notes: 'Manual completion: Paid ₹${amount} in cash'
});
`);
console.log('');

console.log('⚡ Ready to test payout completion manually!');


