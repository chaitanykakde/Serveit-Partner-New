# Cloud Functions & Data Storage Verification - Issues Checklist

## Issues Found

### ❌ CRITICAL ISSUE 1: Cloud Functions NOT Copied
- **Status**: MISSING
- **Expected**: Old project has 6 production Cloud Functions
- **Current**: New project only has 2 functions (sendProfileStatusNotification, sendCustomNotification)
- **Missing Functions**:
  1. `dispatchJobToProviders` - Job dispatch to providers
  2. `sendVerificationNotification` - Verification status notifications
  3. `sendDailyEarningsSummary` - Daily earnings summary
  4. `notifyCustomerOnStatusChange` - Customer status notifications
  5. `acceptJobRequest` - Accept job request (optional)
  6. `sendJobNotification` - Job notifications

### ❌ CRITICAL ISSUE 2: Collection Name Mismatch
- **Status**: MISMATCH
- **Old Cloud Functions Expect**: `partners` collection
- **New App Writes To**: `providers` collection
- **Impact**: Cloud Functions cannot find provider data

### ❌ CRITICAL ISSUE 3: Data Structure Mismatch
- **Status**: MISMATCH
- **Old Cloud Functions Expect** (Nested Structure):
  - `locationDetails.latitude`
  - `locationDetails.longitude`
  - `locationDetails.address`
  - `verificationDetails.verified`
  - `verificationDetails.rejected`
  - `verificationDetails.rejectionReason`
  - `services[]` (array of strings)
  - `personalDetails.fullName`
  - `personalDetails.phoneNumber`
  - `personalDetails.gender`
- **New App Writes** (Flat Structure):
  - `latitude` (root level)
  - `longitude` (root level)
  - `address` (root level)
  - `approvalStatus` (root level, string: "PENDING"/"APPROVED"/"REJECTED")
  - `rejectionReason` (root level)
  - `selectedSubServices` (array, but not as `services[]`)
  - `fullName` (root level)
  - `phoneNumber` (root level)
  - `gender` (root level)

### ⚠️ ISSUE 4: Missing Dependencies
- **Status**: MISSING
- **Old Project Requires**: `axios` for Distance Matrix API
- **Current**: Not in package.json

### ⚠️ ISSUE 5: FCM Token Storage
- **Status**: VERIFY
- **Current**: Writes to `providers/{uid}.fcmToken`
- **Cloud Functions Expect**: `partners/{uid}.fcmToken` (if using old functions)

## Verification Plan

1. **Instrument Firestore writes** to log actual data structure
2. **Verify collection name** used in writes
3. **Check data format** (flat vs nested)
4. **Copy missing Cloud Functions**
5. **Fix collection name** (decide: change app to `partners` OR change CFs to `providers`)
6. **Add data transformation** (convert flat to nested if needed)
7. **Add missing dependencies**

## Next Steps

1. Add instrumentation to verify runtime data storage
2. Run onboarding flow and check logs
3. Fix issues based on evidence
4. Re-verify after fixes

