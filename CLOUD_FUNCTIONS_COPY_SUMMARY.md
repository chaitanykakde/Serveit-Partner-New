# Cloud Functions Copy Summary

## ✅ Completed: Cloud Functions Copied (Excluding Agora)

### Functions Copied from Old Project

#### Core Production Functions (6):
1. ✅ **`dispatchJobToProviders`**
   - Trigger: Firestore `onUpdate` on `Bookings/{phoneNumber}`
   - Purpose: Dispatch new jobs to nearby providers
   - Status: COPIED EXACTLY AS-IS

2. ✅ **`acceptJobRequest`**
   - Trigger: HTTPS Callable
   - Purpose: Accept job request via server-side transaction
   - Status: COPIED EXACTLY AS-IS

3. ✅ **`sendVerificationNotification`**
   - Trigger: Firestore `onUpdate` on `partners/{partnerId}`
   - Purpose: Send notification when verification status changes
   - Status: COPIED EXACTLY AS-IS

4. ✅ **`sendJobNotification`**
   - Trigger: Firestore `onCreate` on `jobRequests/{jobId}`
   - Purpose: Send job notification to assigned partner
   - Status: COPIED EXACTLY AS-IS

5. ✅ **`sendDailyEarningsSummary`**
   - Trigger: Pub/Sub Scheduled (8 PM daily, Asia/Kolkata)
   - Purpose: Send daily earnings summary to partners
   - Status: COPIED EXACTLY AS-IS

6. ✅ **`notifyCustomerOnStatusChange`**
   - Trigger: Firestore `onUpdate` on `Bookings/{phoneNumber}`
   - Purpose: Notify customer when provider updates job status
   - Status: COPIED EXACTLY AS-IS

#### Helper Functions Copied:
- ✅ `calculateDistance()` - Haversine formula for distance calculation
- ✅ `getRoadDistances()` - Google Maps Distance Matrix API integration
- ✅ `findProvidersWithGeoQuery()` - Geo-query to find nearby providers
- ✅ `processNewBooking()` - Core job dispatch logic
- ✅ `sendCustomerStatusNotification()` - Customer notification helper

#### Constants Copied:
- ✅ `FALLBACK_COORDINATES` - Default coordinates
- ✅ `GEO_QUERY_RADIUS` - 200 km
- ✅ `FINAL_DISTANCE_LIMIT` - 200 km
- ✅ `GOOGLE_MAPS_API_KEY` - From Firebase config

### Functions Excluded (Agora/Voice Calling):
- ❌ `generateAgoraToken` - NOT COPIED
- ❌ `endCall` - NOT COPIED
- ❌ `checkBooking` - NOT COPIED
- ❌ `acceptBooking` - NOT COPIED
- ❌ `updateBookingProvider` - NOT COPIED
- ❌ All Agora-related imports and helpers - NOT COPIED

### New Project Functions Preserved:
- ✅ `sendProfileStatusNotification` - Watches `providers/{uid}` collection
- ✅ `sendCustomNotification` - HTTPS Callable for admin notifications

### Dependencies Updated:
- ✅ Added `axios: ^1.6.0` to `package.json` (required for Distance Matrix API)

## Current Status

### ✅ Cloud Functions Status:
- All 6 production functions copied
- All helper functions copied
- All constants copied
- New project functions preserved
- Agora functions excluded
- Dependencies updated
- No compilation errors

### ⚠️ Known Schema Mismatches (Intentionally Left Unresolved):
1. **Collection Name Mismatch**:
   - Old Cloud Functions use: `partners` collection
   - New app writes to: `providers` collection
   - Status: INTENTIONALLY LEFT UNRESOLVED (per instructions)

2. **Data Structure Mismatch**:
   - Old Cloud Functions expect nested structure:
     - `locationDetails.latitude`, `locationDetails.longitude`
     - `verificationDetails.verified`, `verificationDetails.rejected`
     - `services[]` (array)
     - `personalDetails.fullName`, etc.
   - New app writes flat structure:
     - `latitude`, `longitude` (root level)
     - `approvalStatus` (root level, string)
     - `selectedSubServices` (not `services[]`)
   - Status: INTENTIONALLY LEFT UNRESOLVED (per instructions)

## Next Steps

1. **Run onboarding flow** to verify data storage
2. **Inspect Firestore write logs** (instrumentation already added)
3. **Confirm actual data structure** written to Firestore
4. **Decide alignment strategy**:
   - Option A: Align app writes to old schema (nested structure, `partners` collection)
   - Option B: Adapt Cloud Functions to new schema (flat structure, `providers` collection)

## Files Modified

1. ✅ `functions/index.js` - Copied all production functions (excluding Agora)
2. ✅ `functions/package.json` - Added `axios` dependency

## Success Criteria Met

- ✅ All required Cloud Functions (excluding Agora) exist in new project
- ✅ Functions compile successfully
- ✅ No missing imports
- ✅ All dependencies present
- ✅ Schema mismatches intentionally left unresolved (as instructed)

