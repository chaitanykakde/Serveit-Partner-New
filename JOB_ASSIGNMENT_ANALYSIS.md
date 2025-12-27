# Serveit Partner - Job Assignment System Analysis

**Date**: December 27, 2025  
**Purpose**: Factual analysis of existing production Firebase backend job assignment mechanism  
**Status**: READ-ONLY ANALYSIS - NO CODE MODIFICATIONS

---

## 1. HIGH-LEVEL ARCHITECTURE DIAGRAM

```
┌─────────────────────────────────────────────────────────────────┐
│                    CUSTOMER APP (Customer Side)                 │
│  Creates booking → Bookings/{phoneNumber}                       │
└────────────────────────────┬────────────────────────────────────┘
                              │
                              │ onUpdate trigger
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│              Cloud Function: dispatchJobToProviders             │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ 1. Read: serveit_users/{phoneNumber} (get coordinates)   │  │
│  │ 2. Query: partners (find verified providers)             │  │
│  │ 3. Filter: distance, service match                       │  │
│  │ 4. Update: Bookings/{phoneNumber}                        │  │
│  │    - notifiedProviderIds[]                               │  │
│  │    - status: "pending"                                  │  │
│  │ 5. Send: FCM notifications to providers                 │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────────┘
                              │
                              │ FCM Notification
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    PROVIDER APP (Provider Side)                  │
│  Receives notification → Queries Bookings collection           │
│  Finds jobs where notifiedProviderIds includes providerId       │
└────────────────────────────┬────────────────────────────────────┘
                              │
                              │ Provider accepts job
                              │ HTTPS Callable: acceptJobRequest
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│              Cloud Function: acceptJobRequest                    │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ 1. Transaction: Find booking by bookingId                │  │
│  │ 2. Verify: status === "pending"                          │  │
│  │ 3. Verify: providerId in notifiedProviderIds[]          │  │
│  │ 4. Update: Bookings/{phoneNumber}                        │  │
│  │    - providerId                                          │  │
│  │    - status: "accepted"                                  │  │
│  │    - acceptedByProviderId                                │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────────┘
                              │
                              │ Status updates
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│         Cloud Function: notifyCustomerOnStatusChange             │
│  Monitors status changes → Sends customer notifications          │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. CLOUD FUNCTIONS SUMMARY TABLE

| Function Name | Trigger | Purpose | Input | Output/Side Effects |
|--------------|---------|---------|-------|---------------------|
| **dispatchJobToProviders** | `onUpdate` on `Bookings/{phoneNumber}` | Dispatch new jobs to nearby providers | Detects new booking in array | Updates `Bookings/{phoneNumber}` with `notifiedProviderIds[]`, sends FCM notifications |
| **acceptJobRequest** | HTTPS Callable | Provider accepts a job | `{bookingId, providerId}` | Updates `Bookings/{phoneNumber}` with provider info, sets status to "accepted" |
| **sendJobNotification** | `onCreate` on `jobRequests/{jobId}` | Send notification for assigned job | `jobRequests` document with `assignedPartnerId` | Stores notification in `partners/{id}/notifications/`, sends FCM |
| **notifyCustomerOnStatusChange** | `onUpdate` on `Bookings/{phoneNumber}` | Notify customer of status changes | Status change detected | Stores notification in `serveit_users/{phone}/notifications/`, sends FCM |
| **sendVerificationNotification** | `onUpdate` on `partners/{partnerId}` | Notify provider of verification status | Verification status change | Stores notification in `partners/{id}/notifications/`, sends FCM |
| **sendDailyEarningsSummary** | Pub/Sub Scheduled (8 PM IST) | Daily earnings summary | None (scheduled) | Stores notification in `partners/{id}/notifications/`, sends FCM |

### 2.1 Firestore Paths Read/Written

#### dispatchJobToProviders
- **READS**:
  - `serveit_users/{phoneNumber}` - Get customer coordinates
  - `partners` collection - Query verified providers with `isVerified == true`
- **WRITES**:
  - `Bookings/{phoneNumber}` - Updates `notifiedProviderIds[]`, `status`, `jobCoordinates`
- **SIDE EFFECTS**:
  - Sends FCM notifications to qualified providers

#### acceptJobRequest
- **READS**:
  - `partners/{providerId}` - Get provider details
  - `Bookings` collection - Search all documents for `bookingId` (⚠️ INEFFICIENT - scans all)
- **WRITES**:
  - `Bookings/{phoneNumber}` - Updates `providerId`, `providerName`, `status: "accepted"`, `acceptedByProviderId`, `acceptedAt`
- **TRANSACTION**: Uses Firestore transaction to prevent race conditions

---

## 3. DATABASE STRUCTURE MAP

### 3.1 Bookings Collection

**Collection**: `Bookings`  
**Document ID**: `{phoneNumber}` (customer phone number)

#### Document Structure (DUAL FORMAT - Inconsistent)

**Format A: Array Structure** (Current/Primary)
```javascript
Bookings/{phoneNumber} {
  bookings: [
    {
      bookingId: "uuid-string",
      serviceName: "Electrical",
      status: "pending" | "accepted" | "arrived" | "in_progress" | "payment_pending" | "completed",
      bookingStatus: "Pending",  // ⚠️ Duplicate field
      notifiedProviderIds: ["providerId1", "providerId2", ...],
      providerId: "providerId",  // Set when accepted
      providerName: "Provider Name",
      providerMobileNo: "+91...",
      acceptedByProviderId: "providerId",
      jobCoordinates: {
        latitude: 19.866424,
        longitude: 75.3247485
      },
      totalPrice: 300,
      userName: "Customer Name",
      subServicesSelected: {...},
      createdAt: Timestamp,
      acceptedAt: Timestamp,
      arrivedAt: Timestamp,  // ⚠️ UNKNOWN if set by app
      serviceStartedAt: Timestamp,  // ⚠️ UNKNOWN if set by app
      completedAt: Timestamp,  // ⚠️ UNKNOWN if set by app
      timestamp: Timestamp
    },
    // ... more bookings
  ],
  updatedAt: Timestamp
}
```

**Format B: Single Booking Structure** (Legacy/Fallback)
```javascript
Bookings/{phoneNumber} {
  bookingId: "uuid-string",
  serviceName: "Electrical",
  status: "pending" | "accepted" | ...,
  notifiedProviderIds: ["providerId1", "providerId2"],
  providerId: "providerId",
  // ... same fields as array item
}
```

**⚠️ CRITICAL**: Code handles both formats, but array format is primary.

### 3.2 Partners Collection

**Collection**: `partners`  
**Document ID**: `{providerId}` (UID)

#### Document Structure
```javascript
partners/{providerId} {
  // Verification
  isVerified: boolean,  // Root level - PRIMARY CHECK
  verificationDetails: {
    verified: boolean,  // Nested - FALLBACK CHECK
    rejected: boolean,
    rejectionReason: string
  },
  
  // Location (NESTED)
  locationDetails: {
    latitude: number,  // REQUIRED for job dispatch
    longitude: number,  // REQUIRED for job dispatch
    address: string
  },
  
  // Personal Info (NESTED)
  personalDetails: {
    fullName: string,
    phoneNumber: string,
    mobileNo: string,  // Alternative field name
    gender: string
  },
  
  // Services
  services: ["Electrical", "Plumbing", ...],  // Array at root - REQUIRED
  selectedMainService: string,  // Alternative check
  
  // FCM & Status
  fcmToken: string,  // REQUIRED for notifications
  isOnline: boolean,
  serviceRadius: number,
  
  // Timestamps
  createdAt: Timestamp,
  lastLoginAt: Timestamp,
  updatedAt: Timestamp
}
```

**Subcollection**: `partners/{providerId}/notifications/{notificationId}`
- Stores notification history
- Fields: `title`, `message`, `type`, `timestamp`, `isRead`, `userId`, `relatedData`

### 3.3 Serveit Users Collection

**Collection**: `serveit_users`  
**Document ID**: `{phoneNumber}`

#### Document Structure
```javascript
serveit_users/{phoneNumber} {
  latitude: number,  // REQUIRED for job dispatch
  longitude: number,  // REQUIRED for job dispatch
  fcmToken: string,  // For customer notifications
  // ... other customer fields
}
```

**Subcollection**: `serveit_users/{phoneNumber}/notifications/{notificationId}`
- Stores customer notification history

### 3.4 Job Requests Collection

**Collection**: `jobRequests`  
**Document ID**: `{jobId}`

#### Document Structure
```javascript
jobRequests/{jobId} {
  assignedPartnerId: string,  // Provider ID
  serviceType: string,
  customerName: string,
  // ... other job fields
}
```

**⚠️ NOTE**: This collection appears to be for a different job assignment flow (direct assignment, not broadcast). May be legacy or alternative flow.

---

## 4. JOB ASSIGNMENT FLOW (Step-by-Step)

### Phase 1: Job Creation & Dispatch

1. **Customer creates booking**
   - Customer app writes to `Bookings/{phoneNumber}`
   - Adds new booking to `bookings[]` array OR creates single booking document
   - Triggers `onUpdate` on `Bookings/{phoneNumber}`

2. **dispatchJobToProviders triggered**
   - Detects new booking: `afterBookings.length > beforeBookings.length`
   - Gets latest booking from array (last item)

3. **Get job coordinates**
   - Reads `serveit_users/{phoneNumber}` to get `latitude`, `longitude`
   - Falls back to `FALLBACK_COORDINATES` if not found

4. **Find eligible providers**
   - Queries `partners` collection: `.where("isVerified", "==", true)`
   - Filters by:
     - Distance: Within 200km radius (Haversine + Google Maps API)
     - Service match: `services[]` array contains requested service (case-insensitive)
     - Location: Must have `locationDetails.latitude` and `locationDetails.longitude`
   - Creates `qualifiedProviders[]` array

5. **Update booking document**
   - Updates `Bookings/{phoneNumber}`:
     - Sets `notifiedProviderIds[]` = array of qualified provider IDs
     - Sets `status: "pending"`
     - Sets `jobCoordinates: {latitude, longitude}`
   - Handles both array and single booking structures

6. **Send FCM notifications**
   - Iterates through `qualifiedProviders[]`
   - Sends FCM notification to each provider's `fcmToken`
   - Notification data: `{type: "new_job_alert", bookingId: "..."}`

### Phase 2: Provider Acceptance

1. **Provider receives notification**
   - Provider app receives FCM notification
   - Notification contains `bookingId`

2. **Provider queries for available jobs**
   - ⚠️ **UNKNOWN**: How provider app queries jobs
   - **POSSIBLE METHODS**:
     - Query `Bookings` collection where `notifiedProviderIds` array contains provider's ID
     - Query `Bookings` collection where `status == "pending"`
     - Listen to `Bookings` collection and filter client-side

3. **Provider calls acceptJobRequest**
   - HTTPS Callable function
   - Parameters: `{bookingId, providerId}`
   - Requires authentication

4. **Transaction execution**
   - Searches ALL `Bookings` documents for matching `bookingId` (⚠️ INEFFICIENT)
   - Verifies:
     - `status === "pending"` (job still available)
     - `providerId` in `notifiedProviderIds[]` (provider was notified)
   - Updates booking:
     - `providerId: providerId`
     - `providerName: providerData.personalDetails?.fullName || providerData.fullName`
     - `providerMobileNo: providerData.personalDetails?.mobileNo || providerData.mobileNo`
     - `status: "accepted"`
     - `acceptedByProviderId: providerId`
     - `acceptedAt: serverTimestamp()`
   - Transaction prevents race conditions (only one provider can accept)

5. **Status change triggers notification**
   - `notifyCustomerOnStatusChange` detects status change
   - Sends customer notification: "Order Accepted!"

### Phase 3: Job Status Updates

1. **Provider updates job status**
   - ⚠️ **UNKNOWN**: How provider app updates status
   - **POSSIBLE**: Direct write to `Bookings/{phoneNumber}` or separate Cloud Function

2. **Status transitions**
   - `pending` → `accepted` (via acceptJobRequest)
   - `accepted` → `arrived` (⚠️ UNKNOWN mechanism)
   - `arrived` → `in_progress` (⚠️ UNKNOWN mechanism)
   - `in_progress` → `payment_pending` (⚠️ UNKNOWN mechanism)
   - `payment_pending` → `completed` (⚠️ UNKNOWN mechanism)

3. **Customer notifications**
   - `notifyCustomerOnStatusChange` monitors status changes
   - Sends FCM notification for each status change
   - Stores notification in `serveit_users/{phoneNumber}/notifications/`

---

## 5. JOB ASSIGNMENT MECHANISM DETAILS

### 5.1 Assignment Strategy

**Type**: **BROADCAST TO MULTIPLE PROVIDERS** (Not single assignment)

- Jobs are **NOT** copied per provider
- Jobs are **NOT** stored in provider-specific collections
- Jobs remain in **centralized** `Bookings` collection
- Multiple providers are notified via `notifiedProviderIds[]` array
- First provider to accept wins (transaction-based locking)

### 5.2 Provider Eligibility Criteria

1. **Verification**: `isVerified == true` OR `verificationDetails.verified == true`
2. **Location**: Must have `locationDetails.latitude` and `locationDetails.longitude`
3. **Distance**: Within 200km of job location (Haversine + Google Maps API)
4. **Service Match**: `services[]` array contains requested service (case-insensitive) OR `selectedMainService` matches

### 5.3 Fan-Out Strategy

**NO FAN-OUT**: Jobs are NOT duplicated per provider.

- Single job document in `Bookings/{phoneNumber}`
- `notifiedProviderIds[]` array tracks which providers were notified
- Providers must query `Bookings` collection to find jobs
- No provider-specific job collections exist

### 5.4 Race Condition Prevention

**Mechanism**: Firestore Transaction in `acceptJobRequest`

- Transaction reads current booking status
- Verifies `status === "pending"` atomically
- Updates to `status: "accepted"` atomically
- Only one provider can successfully accept (others get `failed-precondition` error)

---

## 6. PROVIDER APP READ STRATEGY

### 6.1 Current Implementation

⚠️ **UNKNOWN**: No provider app code found that reads jobs.

### 6.2 Recommended Approaches (Based on System Design)

#### Option A: Query Bookings Collection (RECOMMENDED)

```javascript
// Find jobs where provider was notified
db.collection("Bookings")
  .where("notifiedProviderIds", "array-contains", providerId)
  .where("status", "==", "pending")
  .get()
```

**Pros**:
- Direct query on Firestore
- Efficient with composite index
- Matches system design

**Cons**:
- Requires composite index: `notifiedProviderIds` + `status`
- May return many results (all customers' bookings)

#### Option B: Query by Provider ID (For Accepted Jobs)

```javascript
// Find jobs accepted by this provider
db.collection("Bookings")
  .where("providerId", "==", providerId)
  .where("status", "in", ["accepted", "arrived", "in_progress", "payment_pending"])
  .get()
```

**Pros**:
- Efficient for active jobs
- Direct provider-specific query

**Cons**:
- Only works for accepted jobs
- Cannot find pending jobs this way

#### Option C: Listen to All Bookings (NOT RECOMMENDED)

```javascript
// Listen to all bookings and filter client-side
db.collection("Bookings").onSnapshot((snapshot) => {
  snapshot.docs.forEach((doc) => {
    const data = doc.data();
    if (data.notifiedProviderIds?.includes(providerId) && data.status === "pending") {
      // Show job
    }
  });
});
```

**Pros**:
- Real-time updates

**Cons**:
- ⚠️ **INEFFICIENT**: Downloads all bookings for all customers
- High read costs
- Poor performance

### 6.3 Safest Approach

**RECOMMENDED**: Use Option A (Query with `array-contains`)

1. **For Pending Jobs**:
   ```javascript
   db.collection("Bookings")
     .where("notifiedProviderIds", "array-contains", providerId)
     .where("status", "==", "pending")
   ```

2. **For Active Jobs**:
   ```javascript
   db.collection("Bookings")
     .where("providerId", "==", providerId)
     .where("status", "in", ["accepted", "arrived", "in_progress", "payment_pending"])
   ```

3. **For Completed Jobs** (if needed):
   ```javascript
   db.collection("Bookings")
     .where("providerId", "==", providerId)
     .where("status", "==", "completed")
   ```

### 6.4 Guaranteed Fields

When reading jobs from `Bookings` collection, these fields are **GUARANTEED** to exist:

**For Pending Jobs** (notified but not accepted):
- `bookingId` ✅
- `serviceName` ✅
- `status: "pending"` ✅
- `notifiedProviderIds[]` ✅ (array contains providerId)
- `jobCoordinates: {latitude, longitude}` ✅
- `totalPrice` ✅
- `userName` ✅
- `createdAt` ✅

**For Accepted Jobs**:
- All above fields ✅
- `providerId` ✅ (equals provider's ID)
- `providerName` ✅
- `providerMobileNo` ✅
- `status: "accepted" | "arrived" | "in_progress" | "payment_pending" | "completed"` ✅
- `acceptedByProviderId` ✅
- `acceptedAt` ✅

---

## 7. JOB STATUS FLOW

### 7.1 Status Enumeration

**Found Statuses** (from code analysis):
1. `"pending"` - Job created, providers notified, waiting for acceptance
2. `"accepted"` - Provider accepted the job
3. `"arrived"` - Provider arrived at location (⚠️ UNKNOWN if set by app or function)
4. `"in_progress"` - Service work started (⚠️ UNKNOWN if set by app or function)
5. `"payment_pending"` - Service completed, waiting for payment (⚠️ UNKNOWN if set by app or function)
6. `"completed"` - Job fully completed (⚠️ UNKNOWN if set by app or function)

**Additional Field**: `bookingStatus: "Pending"` (⚠️ Appears to be duplicate/legacy field)

### 7.2 Valid Transitions

```
pending → accepted → arrived → in_progress → payment_pending → completed
  │
  └─ (timeout/rejection) → ❓ UNKNOWN
```

**Confirmed Transitions**:
- `pending` → `accepted` ✅ (via `acceptJobRequest` Cloud Function)

**⚠️ UNKNOWN Transitions**:
- `accepted` → `arrived` (mechanism unknown)
- `arrived` → `in_progress` (mechanism unknown)
- `in_progress` → `payment_pending` (mechanism unknown)
- `payment_pending` → `completed` (mechanism unknown)

### 7.3 Status Change Notifications

**Function**: `notifyCustomerOnStatusChange`

**Triggers**: Any status change on `Bookings/{phoneNumber}`

**Conditions**:
- Status must change (`beforeStatus !== afterStatus`)
- Must have `acceptedByProviderId` (provider assigned)

**Notifications Sent**:
- `accepted` → "✅ Order Accepted!"
- `arrived` → "📍 Provider Arrived"
- `in_progress` → "🔧 Service Started"
- `payment_pending` → "💳 Payment Due"
- `completed` → "🎉 Order Completed!"

### 7.4 Locking Mechanism

**Type**: Transaction-based optimistic locking

**Location**: `acceptJobRequest` Cloud Function

**Mechanism**:
1. Transaction reads current booking status
2. Verifies `status === "pending"` atomically
3. Updates to `status: "accepted"` atomically
4. If another provider accepted first, transaction fails with `failed-precondition`

**Result**: Only one provider can accept a job (first-come-first-served)

**⚠️ UNKNOWN**: Whether "only one ongoing job" is enforced (no code found)

---

## 8. RISKS & CONSTRAINTS

### 8.1 Race Condition Risks

#### ✅ MITIGATED
- **Job Acceptance**: Transaction in `acceptJobRequest` prevents multiple providers accepting same job

#### ⚠️ POTENTIAL RISKS
- **Status Updates**: No transaction found for status updates (`arrived`, `in_progress`, etc.)
  - Risk: Provider app could update status while customer cancels
  - Risk: Multiple status updates could conflict

- **Booking Array Updates**: When adding new booking to array, no transaction found
  - Risk: Concurrent bookings could cause array corruption
  - Mitigation: `onUpdate` trigger only fires on document change

### 8.2 What MUST NOT Be Changed

#### Critical Fields (Used by Cloud Functions)

**Bookings Collection**:
- ❌ **DO NOT REMOVE**: `notifiedProviderIds[]` - Used by `acceptJobRequest` to verify eligibility
- ❌ **DO NOT REMOVE**: `status` field - Used by `acceptJobRequest` and `notifyCustomerOnStatusChange`
- ❌ **DO NOT REMOVE**: `bookingId` - Used to find bookings in `acceptJobRequest`
- ❌ **DO NOT REMOVE**: `acceptedByProviderId` - Used by `notifyCustomerOnStatusChange` to verify provider assigned
- ❌ **DO NOT CHANGE**: `status: "pending"` check in `acceptJobRequest` - Prevents double acceptance

**Partners Collection**:
- ❌ **DO NOT REMOVE**: `isVerified` - Used by `dispatchJobToProviders` query
- ❌ **DO NOT REMOVE**: `locationDetails.latitude` / `locationDetails.longitude` - Required for job dispatch
- ❌ **DO NOT REMOVE**: `services[]` array - Required for service matching
- ❌ **DO NOT REMOVE**: `fcmToken` - Required for notifications
- ❌ **DO NOT REMOVE**: `personalDetails.fullName` / `personalDetails.mobileNo` - Used by `acceptJobRequest`

**Serveit Users Collection**:
- ❌ **DO NOT REMOVE**: `latitude` / `longitude` - Required for job dispatch (fallback exists but not ideal)

### 8.3 Read-Only vs Writable Fields (From Provider App)

#### ✅ SAFE TO READ (Provider App)
- All fields in `Bookings/{phoneNumber}` documents
- `partners/{providerId}` document (own profile)
- `partners/{providerId}/notifications/` subcollection

#### ⚠️ WRITE WITH CAUTION (Provider App)
- `Bookings/{phoneNumber}` - Status updates
  - **SAFE**: Update `status` field (with proper validation)
  - **SAFE**: Update timestamp fields (`arrivedAt`, `serviceStartedAt`, `completedAt`)
  - **UNSAFE**: Modify `notifiedProviderIds[]` (breaks eligibility check)
  - **UNSAFE**: Modify `providerId` after acceptance (breaks ownership)
  - **UNSAFE**: Modify `bookingId` (breaks job lookup)

#### ❌ DO NOT WRITE (Provider App)
- `Bookings/{phoneNumber}` - `notifiedProviderIds[]` (system-managed)
- `Bookings/{phoneNumber}` - `providerId` (set by `acceptJobRequest` only)
- `Bookings/{phoneNumber}` - `acceptedByProviderId` (set by `acceptJobRequest` only)
- `Bookings/{phoneNumber}` - `status: "pending"` → `"accepted"` (use `acceptJobRequest` function)

### 8.4 Performance Concerns

#### ⚠️ INEFFICIENT OPERATIONS

1. **acceptJobRequest - Booking Lookup**
   - **Issue**: Scans ALL `Bookings` documents to find `bookingId`
   - **Code**: `bookingsSnapshot = await db.collection("Bookings").get()`
   - **Impact**: O(n) complexity, slow for large number of bookings
   - **Risk**: High read costs, timeout for large datasets
   - **Recommendation**: ⚠️ Consider adding index or changing document structure

2. **dispatchJobToProviders - Provider Query**
   - **Issue**: Queries ALL verified providers, filters in-memory
   - **Code**: `.where("isVerified", "==", true).get()` then filters by distance
   - **Impact**: Downloads all verified providers, filters client-side
   - **Risk**: High read costs for many providers
   - **Mitigation**: Uses geo-bounds filtering (longitude bounds) before distance check

### 8.5 Data Structure Inconsistencies

#### ⚠️ DUAL FORMAT SUPPORT

**Bookings Collection** supports two formats:
1. Array format: `{bookings: [{...}, {...}]}`
2. Single format: `{bookingId: "...", ...}`

**Risk**: Code must handle both, increases complexity
**Impact**: Provider app must handle both formats when reading

#### ⚠️ FIELD NAME INCONSISTENCIES

- `status` vs `bookingStatus` (both exist, `status` is primary)
- `personalDetails.mobileNo` vs `personalDetails.phoneNumber` (fallback logic exists)
- `personalDetails.fullName` vs `fullName` (fallback logic exists)

---

## 9. UNKNOWN / NEED CONFIRMATION

### 9.1 Provider App Job Access

⚠️ **UNKNOWN**: How provider app currently queries/reads jobs
- No provider app code found that reads from `Bookings` collection
- Need to confirm: Does provider app query `Bookings` or use different mechanism?

### 9.2 Status Update Mechanism

⚠️ **UNKNOWN**: How provider app updates job status (`arrived`, `in_progress`, etc.)
- No Cloud Function found for status updates
- **POSSIBLE**: Direct write to `Bookings/{phoneNumber}` from provider app
- **POSSIBLE**: Separate Cloud Function not in current codebase

### 9.3 Job Timeout/Expiry

⚠️ **UNKNOWN**: What happens if no provider accepts a job?
- No timeout mechanism found
- No expiry logic found
- Jobs may remain in `pending` status indefinitely

### 9.4 Multiple Ongoing Jobs

⚠️ **UNKNOWN**: Can a provider have multiple ongoing jobs?
- No enforcement found in code
- No `isOnline` check in `acceptJobRequest`
- Provider could theoretically accept multiple jobs

### 9.5 Job Cancellation

⚠️ **UNKNOWN**: How are jobs cancelled?
- No cancellation mechanism found
- No cancellation status found
- Customer or provider cancellation flow unknown

### 9.6 jobRequests Collection

⚠️ **UNKNOWN**: Purpose of `jobRequests` collection
- Separate from `Bookings` collection
- Has `assignedPartnerId` (direct assignment, not broadcast)
- May be legacy or alternative flow
- `sendJobNotification` function watches this collection

---

## 10. SUMMARY

### 10.1 Key Findings

1. **Assignment Strategy**: Broadcast to multiple providers, first-accept wins
2. **Storage**: Centralized in `Bookings` collection, no provider-specific collections
3. **Locking**: Transaction-based for acceptance, no locking for status updates
4. **Query Pattern**: Provider must query `Bookings` with `array-contains` on `notifiedProviderIds`
5. **Status Flow**: `pending` → `accepted` → `arrived` → `in_progress` → `payment_pending` → `completed`

### 10.2 Critical Constraints

- **MUST NOT** modify `notifiedProviderIds[]` from provider app
- **MUST NOT** change `status: "pending"` to `"accepted"` directly (use `acceptJobRequest`)
- **MUST** query `Bookings` collection to find jobs (no provider-specific collections)
- **MUST** handle both array and single booking formats

### 10.3 Recommended Provider App Implementation

1. **Query pending jobs**: `Bookings.where("notifiedProviderIds", "array-contains", providerId).where("status", "==", "pending")`
2. **Query active jobs**: `Bookings.where("providerId", "==", providerId).where("status", "in", ["accepted", "arrived", "in_progress", "payment_pending"])`
3. **Accept job**: Call `acceptJobRequest` Cloud Function (HTTPS Callable)
4. **Update status**: Direct write to `Bookings/{phoneNumber}` status field (with validation)

---

**END OF ANALYSIS**

