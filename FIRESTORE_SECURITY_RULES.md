# Firestore Security Rules for Provider Job Inbox

## Required Rule Addition

Add the following rule to your Firestore security rules (in Firebase Console or `firestore.rules` file):

```javascript
match /provider_job_inbox/{providerId}/jobs/{jobId} {
  // Providers can only read their own inbox
  allow read: if request.auth != null && request.auth.uid == providerId;
  
  // Only Cloud Functions can write (using admin SDK)
  allow write: if false; // Cloud Functions use admin SDK, so this blocks client writes
}
```

## How to Add

1. Go to Firebase Console → Firestore Database → Rules
2. Add the above rule to your existing rules
3. Publish the rules

## Important Notes

- The inbox collection is **read-only** for providers
- All writes are performed by Cloud Functions using the Admin SDK
- This ensures data integrity and prevents unauthorized modifications
- Providers can only access their own inbox entries (enforced by `providerId` match)

