# FCM Notification Debugging Guide

## Issues Found

### 1. Function Executing But No Notifications
The Cloud Function is executing (3-5ms) but likely returning early because:
- **No FCM token in Firestore** - Check if `providers/{uid}/fcmToken` exists
- **No status change detected** - Function only triggers on specific status changes
- **Token might be invalid** - Old/expired tokens won't work

### 2. Common Issues to Check

#### Check FCM Token in Firestore
1. Go to Firebase Console > Firestore Database
2. Navigate to `providers/{uid}`
3. Check if `fcmToken` field exists and has a value
4. Token should look like: `cXyZ123...` (long string)

#### Check Function Logs
```bash
firebase functions:log --only sendProfileStatusNotification
```

Look for:
- `No FCM token found for provider` - Token not saved
- `No relevant status change detected` - Status didn't change
- `Successfully sent notification` - Notification sent successfully
- Error messages about invalid tokens

#### Check Android App
1. **Notification Permissions**: Android 13+ requires runtime permission
2. **FCM Service Registered**: Check AndroidManifest.xml has the service
3. **Token Generation**: App should request token on login

### 3. Testing Steps

#### Step 1: Verify Token is Saved
1. Login to the app
2. Check Firestore: `providers/{uid}/fcmToken` should have a value
3. If empty, check app logs for "Failed to save token"

#### Step 2: Test Status Change
1. Update a provider document in Firestore:
   - Change `onboardingStatus` to `"SUBMITTED"`
   - OR change `approvalStatus` to `"APPROVED"` or `"REJECTED"`
2. Check function logs immediately
3. Should see: `Status changed to SUBMITTED/APPROVED/REJECTED`

#### Step 3: Check Notification Delivery
1. Check Firebase Console > Cloud Messaging
2. View delivery reports
3. Check device notification settings

### 4. Common Fixes

#### Fix 1: Token Not Saving
**Problem**: `fcmToken` field is empty in Firestore

**Solution**: 
- Check app logs for errors
- Verify Firebase project is correctly configured
- Ensure user is logged in when token is requested
- Check Firestore security rules allow writes

#### Fix 2: Function Not Triggering
**Problem**: Function executes but returns early

**Solution**:
- Check if status actually changed (before vs after)
- Verify field names match exactly: `onboardingStatus`, `approvalStatus`
- Check function logs for "No relevant status change"

#### Fix 3: Invalid Token
**Problem**: Token exists but notifications fail

**Solution**:
- Token might be expired (tokens can expire)
- App might need to request new token
- Check function logs for token errors
- Function automatically tries alternative tokens from subcollection

### 5. Manual Test

To manually test notifications:

1. **Get FCM Token from Firestore**:
   - Go to `providers/{uid}/fcmToken`
   - Copy the token value

2. **Send Test Notification via Firebase Console**:
   - Go to Firebase Console > Cloud Messaging
   - Click "Send test message"
   - Paste the FCM token
   - Send a test notification

3. **Check if notification appears on device**

### 6. Debug Checklist

- [ ] FCM token exists in `providers/{uid}/fcmToken`
- [ ] Function is deployed and active
- [ ] Status change is happening (check before/after values)
- [ ] Function logs show "Status changed to..."
- [ ] No errors in function logs
- [ ] Device has internet connection
- [ ] Notification permissions granted (Android 13+)
- [ ] App is not in background restrictions
- [ ] Firebase project is correctly configured
- [ ] `google-services.json` is in `app/` directory

### 7. Next Steps

1. Check Firestore for FCM token
2. Update a provider status and watch function logs
3. Check Firebase Console > Cloud Messaging for delivery status
4. Verify Android app has notification permissions

