# Script to Verify Provider

## Using Firebase CLI

Run this command to verify a provider:

```bash
firebase firestore:set partners/SAXjtjXY8vNf2IAHRrChrhvXnOt2 '{
  "approvalStatus": "APPROVED",
  "isVerified": true,
  "verificationDetails": {
    "verified": true,
    "rejected": false
  }
}' --merge
```

Replace `SAXjtjXY8vNf2IAHRrChrhvXnOt2` with the actual provider ID.

## After Verification

Once the provider is verified:
1. ✅ They will appear in the `findProvidersWithGeoQuery` results
2. ✅ They will receive job notifications via FCM
3. ✅ Jobs will be added to their `provider_job_inbox` collection
4. ✅ They can accept/reject jobs through the app

## Verify It Worked

Check the logs after creating a new booking:
```bash
firebase functions:log
```

You should see:
- `Total verified providers in database: 1` (or more)
- `Wake-up notification sent to provider {providerId}`
- `Created inbox entries for X providers`

