# Firebase Cloud Functions Setup Guide

## Overview
This guide explains how to set up Firebase Cloud Functions to automatically send FCM notifications when provider profile status changes.

## Prerequisites
1. Node.js 18+ installed
2. Firebase CLI installed: `npm install -g firebase-tools`
3. Firebase project initialized

## Setup Steps

### 1. Initialize Firebase Functions (if not already done)
```bash
cd /path/to/ServeitPartnerNew
firebase init functions
```
- Select JavaScript
- Install dependencies: Yes
- Use ESLint: Yes

### 2. Install Dependencies
```bash
cd functions
npm install
```

### 3. Deploy Functions
```bash
# Deploy all functions
firebase deploy --only functions

# Or deploy specific function
firebase deploy --only functions:sendProfileStatusNotification
```

### 4. Verify Deployment
```bash
firebase functions:log
```

## How It Works

### Automatic Notifications
The `sendProfileStatusNotification` function automatically triggers when:
- **Profile Submitted**: When `onboardingStatus` changes to `"SUBMITTED"`
  - Notification: "Your profile is under review..."
  
- **Profile Approved**: When `approvalStatus` changes to `"APPROVED"`
  - Notification: "Congratulations! Your profile has been approved..."
  
- **Profile Rejected**: When `approvalStatus` changes to `"REJECTED"`
  - Notification: "Your profile has been rejected. Reason: {reason}"

### Token Management
- If the main `fcmToken` is invalid, the function automatically tries tokens from `providers/{uid}/fcmTokens/` subcollection
- Updates the main document with the latest working token

## Testing

### Test Locally (Emulator)
```bash
cd functions
npm run serve
```

### Test with Admin SDK (Alternative)
If you have a backend/admin panel, you can send notifications directly using Firebase Admin SDK:

```javascript
const admin = require('firebase-admin');
admin.initializeApp();

async function sendNotification(uid, title, body) {
  const providerDoc = await admin.firestore()
    .collection('providers')
    .doc(uid)
    .get();
  
  const fcmToken = providerDoc.data().fcmToken;
  
  await admin.messaging().send({
    notification: { title, body },
    token: fcmToken,
    android: {
      priority: 'high',
      notification: {
        channelId: 'serveit_partner_notifications',
      },
    },
  });
}
```

## Manual Notification Sending
You can also call the `sendCustomNotification` function from your admin panel:

```javascript
const sendCustomNotification = firebase.functions().httpsCallable('sendCustomNotification');
await sendCustomNotification({
  uid: 'provider_uid',
  title: 'Custom Title',
  body: 'Custom message'
});
```

## Important Notes

1. **Billing**: Cloud Functions have a free tier, but check Firebase pricing
2. **Permissions**: Ensure your Firestore security rules allow the function to read provider documents
3. **Token Updates**: The app automatically saves FCM tokens, but the function handles token refresh
4. **Error Handling**: Invalid tokens are logged but don't crash the function

## Troubleshooting

### Function not triggering?
- Check Firestore security rules allow reads
- Verify the function is deployed: `firebase functions:list`
- Check logs: `firebase functions:log`

### Notifications not received?
- Verify FCM token is saved in Firestore
- Check device has internet connection
- Verify notification channel exists (Android 8+)
- Check Firebase Console > Cloud Messaging for delivery status

### Token errors?
- The function automatically tries alternative tokens
- Check `providers/{uid}/fcmTokens/` subcollection has valid tokens
- Ensure app is saving tokens correctly

## Alternative: Backend API
If you prefer not to use Cloud Functions, you can:
1. Create a backend API endpoint
2. Use Firebase Admin SDK to send notifications
3. Call the API when admin updates profile status

The app is ready to receive notifications either way!

