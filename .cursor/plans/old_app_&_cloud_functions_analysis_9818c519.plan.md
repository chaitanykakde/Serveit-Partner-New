---
name: Old App & Cloud Functions Analysis
overview: Comprehensive analysis of the old Servit Partner Android app and Firebase Cloud Functions to extract backend logic, database structure, and identify reusable components for migration to the new Jetpack Compose project.
todos: []
---

# Ol

d App & Cloud Functions Analysis Plan

## Overview

Analyze the old Servit Partner Android project (`C:\Users\Chaitany Kakde\StudioProjects\Servit-Partner`) and its Firebase backend to extract:

- Database structure and collection schemas

- Cloud Functions logic and triggers

- Request/job flow and status management

- Reusable components for new project migration

## Analysis Tasks

### Phase 1: Database Structure Analysis

#### 1.1 Firestore Collections Documentation

**Location**: Review existing `ANALYSIS.md` and code references**Collections to Document**:

1. **`Bookings/{phoneNumber}`**

- Structure: Single booking OR array of bookings (`bookings[]`)
- Key Fields:

    - `bookingId`, `serviceName`, `status`, `bookingStatus`

    - `notifiedProviderIds[]`, `providerId`, `providerName`

    - `totalPrice`, `userName`, `address`, `mobileNumber`

    - `jobCoordinates: {latitude, longitude}`

    - Timestamps: `createdAt`, `acceptedAt`, `arrivedAt`, `serviceStartedAt`, `completedAt`

- Status Flow: `pending` → `accepted` → `arrived` → `in_progress` → `payment_pending` → `completed`

- **Issue**: Inconsistent structure (single vs array)

2. **`partners/{providerId}`**

- Provider profile data

- Subcollections: `notifications/{notificationId}`

- Key Fields:

    - `personalDetails: {fullName, phoneNumber, gender}`
    - `locationDetails: {latitude, longitude, address}`

    - `services: []` (array of service names)

    - `verificationDetails: {verified, rejected, rejectionReason}`

    - `fcmToken`, `isOnline`

    - `serviceRadius`

3. **`serveit_users/{phoneNumber}`**

- Customer data

- Key Fields: `latitude`, `longitude`, `fcmToken`

- Used by Cloud Functions to get job coordinates

4. **`service_providers/{userId}`** (Optional)

- Earnings summary: `totalEarnings`, `totalJobs`, `completedJobs`

5. **`jobRequests/{jobId}`** (Legacy - may not be used)

- Legacy job request structure

#### 1.2 Data Model Analysis

**Files to Review**:

- `app/src/main/java/com/nextserve/servitpartner/data/model/Booking.kt`

- `app/src/main/java/com/nextserve/servitpartner/data/model/Job.kt`

- `app/src/main/java/com/nextserve/servitpartner/data/model/Provider.kt`

**Document**:

- Field mappings between Firestore and Kotlin models

- Status enum values and transitions

- Helper properties and computed fields

### Phase 2: Cloud Functions Analysis

#### 2.1 Function Inventory

**File**: `functions/index.js`

**Functions to Document** (11 total):

**EXISTING PRODUCTION FUNCTIONS (6)**:

1. **`dispatchJobToProviders`**

- **Trigger**: Firestore `onUpdate` on `Bookings/{phoneNumber}`

- **Purpose**: Find nearby providers and notify them of new jobs

- **Flow**:

    1. Detects new booking in array

    1. Gets job coordinates from `serveit_users`

    1. Finds providers using geo-query + Distance Matrix API

    1. Updates booking with `notifiedProviderIds[]`

    1. Sends FCM push notifications

- **Reads From**: `serveit_users`, `partners`

- **Writes To**: `Bookings`

- **Reusability**: ✅ Reusable (may need minor adjustments)

2. **`acceptJobRequest`**

- **Trigger**: HTTPS Callable

- **Purpose**: Accept job via server-side transaction

- **Note**: Currently NOT used by app (app uses client-side transaction)

- **Reusability**: 🟡 Optional - can be removed or kept as backup

3. **`sendVerificationNotification`**

- **Trigger**: Firestore `onUpdate` on `partners/{partnerId}`

- **Purpose**: Send notification when verification status changes

- **Reads From**: `partners`

- **Writes To**: `partners/{userId}/notifications`, FCM

- **Reusability**: ✅ Reusable

4. **`sendJobNotification`**

- **Trigger**: Firestore `onCreate` on `jobRequests/{jobId}`

- **Purpose**: Send notification for new job requests

- **Note**: May be legacy (not actively used)

- **Reusability**: ❌ Needs verification if still used

5. **`sendDailyEarningsSummary`**

- **Trigger**: Pub/Sub Scheduled (8 PM daily, Asia/Kolkata)

- **Purpose**: Send daily earnings summary to providers

- **Reads From**: `earnings`, `partners`

- **Writes To**: `partners/{userId}/notifications`, FCM

- **Reusability**: ✅ Reusable

6. **`notifyCustomerOnStatusChange`**

- **Trigger**: Firestore `onUpdate` on `Bookings/{phoneNumber}`

- **Purpose**: Notify customer when provider updates job status

- **Reads From**: `Bookings`, `serveit_users`

- **Writes To**: `serveit_users/{phoneNumber}/notifications`, FCM

- **Reusability**: ✅ Reusable

**NEW AGORA VOICE CALLING FUNCTIONS (5)**:

- `generateAgoraToken` - Generate Agora RTC token

- `endCall` - Log call end event

- `checkBooking` - Validate booking for voice calling
- `acceptBooking` - Accept booking (voice calling context)

- `updateBookingProvider` - Update provider in booking

- **Reusability**: 🟡 Depends on voice calling feature requirements

#### 2.2 Helper Functions Analysis

**Functions to Document**:

- `processNewBooking()` - Core job dispatch logic

- `findProvidersWithGeoQuery()` - Geo-query implementation

- `getRoadDistances()` - Distance Matrix API integration
- `calculateDistance()` - Haversine formula

- `sendCustomerStatusNotification()` - Customer notification helper

### Phase 3: Android App Data Flow Analysis

#### 3.1 Job Request Flow

**Files**:

- `app/src/main/java/com/nextserve/servitpartner/ui/jobs/JobRequestsViewModel.kt`

- `app/src/main/java/com/nextserve/servitpartner/ui/jobs/NewRequestsFragment.kt`

**Flow**:

1. App sets up Firestore listener on `Bookings` collection (entire collection)

2. Filters in-memory: `status == "pending"` AND `notifiedProviderIds.contains(currentUserId)`

3. Displays filtered jobs in UI

4. **Issue**: Inefficient - queries entire collection, filters in-memory

**Accept Flow**:

1. Finds booking document (searches all documents - inefficient)

2. Executes Firestore transaction:

- Verifies `status == "pending"`

- Verifies provider in `notifiedProviderIds`

- Updates: `status="accepted"`, `providerId`, `providerName`, `acceptedAt`

3. Removes from local list

4. Navigates to job details

**Reject Flow**:

1. Only removes from local list

2. **Issue**: Does NOT update Firestore - rejected jobs can reappear

#### 3.2 Active Jobs Flow

**Files**:

- `app/src/main/java/com/nextserve/servitpartner/ui/schedule/MyScheduleViewModel.kt`

- `app/src/main/java/com/nextserve/servitpartner/ui/schedule/OrderDetailsFragment.kt`

**Flow**:

1. Queries `Bookings` where `providerId == currentUserId` AND `status != "pending"`

2. Real-time listener for status updates

3. Provider can update status: `arrived` → `in_progress` → `payment_pending` → `completed`

4. Each status change triggers `notifyCustomerOnStatusChange` Cloud Function

#### 3.3 Repository Pattern

**Files**:

- `app/src/main/java/com/nextserve/servitpartner/data/repository/JobRepository.kt`

- `app/src/main/java/com/nextserve/servitpartner/data/repository/ProviderRepository.kt`

- `app/src/main/java/com/nextserve/servitpartner/data/repository/FirebaseFunctionsRepository.kt`

**Document**:

- How repositories abstract Firestore operations

- Listener management and cleanup

- Error handling patterns

### Phase 4: Status Flow Documentation

#### 4.1 Booking Status Lifecycle

```javascript
pending → accepted → arrived → in_progress → payment_pending → completed
   ↓
cancelled (can occur at any stage)
```



**Status Definitions**:

- `pending`: Job created, waiting for provider acceptance

- `accepted`: Provider accepted the job

- `arrived`: Provider arrived at customer location

- `in_progress`: Service work started

- `payment_pending`: Service completed, waiting for payment

- `completed`: Payment collected, job fully completed

- `cancelled`: Job cancelled (by customer or provider)

**Timestamps**:

- `createdAt`: Job creation time
- `acceptedAt`: When provider accepted

- `arrivedAt`: When provider marked as arrived

- `serviceStartedAt`: When service started

- `paymentCollectedAt`: When payment collected

- `completedAt`: When job completed

### Phase 5: Migration Assessment

#### 5.1 Reusability Matrix

**Cloud Functions**:

- ✅ **Reusable as-is**: `dispatchJobToProviders`, `sendVerificationNotification`, `sendDailyEarningsSummary`, `notifyCustomerOnStatusChange`

- 🟡 **Reusable with changes**: `acceptJobRequest` (if needed), Agora functions (if voice calling needed)

- ❌ **Remove or redesign**: `sendJobNotification` (if legacy), inefficient query patterns

**Database Structure**:

- ✅ **Keep**: `Bookings`, `partners`, `serveit_users` collections

- 🟡 **Standardize**: Booking structure (choose single OR array, not both)

- ❌ **Remove**: Legacy `jobRequests` collection (if not used)

**Android Code Patterns**:

- ✅ **Reuse Logic**: Status flow, transaction patterns, notification handling

- 🟡 **Improve**: Query efficiency (use Firestore queries instead of in-memory filtering)

- ❌ **Fix**: Rejected jobs not persisted, hardcoded coordinates, no timeout handling

#### 5.2 Required Improvements

**Critical**:

1. **Standardize Booking Structure**: Choose array-only or single-only structure

2. **Fix Query Efficiency**: Use Firestore `whereArrayContains` and `whereEqualTo` instead of full collection query

3. **Persist Rejected Jobs**: Update Firestore when provider declines

4. **Add Order Timeout**: Cloud Function scheduled job to auto-cancel stale pending orders

**Important**:

5. **Fix Hardcoded Coordinates**: Use actual location from LocationService

6. **Add Retry Logic**: For failed transactions

7. **Improve Error Handling**: User-friendly error messages

8. **Add Offline Support**: Local caching and sync queue

### Phase 6: Documentation Output

#### 6.1 Create Analysis Document

**File**: `MIGRATION_ANALYSIS.md` (in new project)

**Sections**:

1. **Database Schema**: Complete Firestore collection structures

2. **Cloud Functions Reference**: All functions with triggers, inputs, outputs

3. **Data Flow Diagrams**: Request creation → dispatch → acceptance → completion

4. **Status Flow Diagram**: Visual representation of status transitions

5. **Reusability Assessment**: What can be reused, what needs changes

6. **Migration Checklist**: Step-by-step migration tasks

#### 6.2 Code Reference Guide

**File**: `CODE_REFERENCE.md`**Sections**:

1. **Key Files**: List of important files with purposes

2. **Model Mappings**: Firestore field → Kotlin model mappings
3. **Repository Patterns**: How to structure data access

4. **Listener Management**: Best practices for Firestore listeners

### Phase 7: Integration Plan for New Project

#### 7.1 Home Screen Data

- Query `Bookings` with filters: `whereArrayContains("notifiedProviderIds", currentUserId).whereEqualTo("status", "pending")`

- Real-time listener for new job requests

- Display job cards with service name, distance, price

#### 7.2 Jobs Screen Data

- Query `Bookings` where `providerId == currentUserId` AND `status IN ["accepted", "arrived", "in_progress", "payment_pending"]`

- Real-time listener for status updates

- Group by status or show timeline

#### 7.3 Real-time Updates

- Use Firestore `addSnapshotListener` with proper query filters

- Handle listener lifecycle (attach/detach on screen lifecycle)

- Update UI state reactively

#### 7.4 Notifications

- FCM push notifications for new jobs (handled by Cloud Function)

- In-app notifications from `partners/{userId}/notifications` subcollection

- Real-time listener for notification updates

## Deliverables

1. **`MIGRATION_ANALYSIS.md`**: Complete analysis document

2. **`CLOUD_FUNCTIONS_REFERENCE.md`**: Detailed Cloud Functions documentation

3. **`DATABASE_SCHEMA.md`**: Complete database structure documentation

4. **`REUSABILITY_ASSESSMENT.md`**: What can be reused and what needs changes

5. **Code snippets**: Key code patterns to reuse in new project

## Constraints

- ⚠️ **Read-only analysis** - Do NOT modify old project

- ⚠️ **Do NOT deploy changes** - Analysis only

- ⚠️ **Verify from code** - Don't guess, check actual implementation

- ⚠️ **Preserve production logic** - Document current behavior accurately

## Success Criteria

After analysis, we should clearly know:

- ✅ Where requests are stored (`Bookings` collection)

- ✅ How providers receive jobs (Cloud Function + FCM + Firestore listener)

- ✅ How Cloud Functions control flow (6 production functions)

- ✅ What can be reused safely (4-5 Cloud Functions, database structure)