# 🔥 FIRESTORE INDEX PLANNING SHEET
## Complete Indexing Strategy for Serveit Partner App

### 📋 INDEX PRIORITY LEVELS
- 🔴 **CRITICAL** - Required for app functionality, create immediately
- 🟡 **HIGH** - Important for performance, create soon
- 🟠 **MEDIUM** - Nice to have, create when possible
- 🟢 **LOW** - Optional, create if performance issues arise

---

## 🔴 CRITICAL INDEXES (REQUIRED FOR APP FUNCTIONALITY)

### 1. Earnings Query (ALREADY IMPLEMENTED)
```
Collection: bookings
Fields: partnerId (ASC) + status (ASC) + completedAt (DESC)
Purpose: Load completed bookings for earnings calculation
Impact: Without this index, earnings screen shows FAILED_PRECONDITION error
```

**Firebase Console Path:**
```
Firestore → Indexes → Composite Indexes → Create Index
Collection ID: bookings
Fields to index:
  - partnerId (Ascending)
  - status (Ascending)
  - completedAt (Descending)
```

### 2. Payout Requests by Partner
```
Collection: payoutRequests
Fields: partnerId (ASC) + requestedAt (DESC)
Purpose: Load user's payout history
Impact: Without this index, payout history fails to load
```

### 3. Monthly Settlements by Partner
```
Collection: monthlySettlements
Fields: partnerId (ASC) + yearMonth (DESC)
Purpose: Load user's monthly earnings summaries
Impact: Without this index, settlement list fails to load
```

### 4. Bank Accounts by Partner
```
Collection: bankAccounts
Fields: partnerId (ASC)
Purpose: Load user's bank account details
Impact: Without this index, bank account section fails to load
```

---

## 🟡 HIGH PRIORITY INDEXES (PERFORMANCE OPTIMIZATION)

### 5. Completed Bookings by Date Range
```
Collection: bookings
Fields: status (ASC) + completedAt (DESC)
Purpose: Admin dashboard - filter completed bookings by date
Impact: Slow admin queries for settlement calculations
```

### 6. Payout Requests by Status
```
Collection: payoutRequests
Fields: requestStatus (ASC) + requestedAt (DESC)
Purpose: Admin dashboard - filter payout requests by status
Impact: Slow admin approval workflow
```

### 7. Settlements by Status
```
Collection: monthlySettlements
Fields: settlementStatus (ASC) + updatedAt (DESC)
Purpose: Admin dashboard - find settlements needing processing
Impact: Slow settlement status updates
```

### 8. Payout Transactions by Partner
```
Collection: payoutTransactions
Fields: partnerId (ASC) + processedAt (DESC)
Purpose: Load user's completed payout history
Impact: Slow transaction history loading
```

---

## 🟠 MEDIUM PRIORITY INDEXES (ANALYTICS & REPORTING)

### 9. Bookings by Service Type
```
Collection: bookings
Fields: serviceName (ASC) + completedAt (DESC)
Purpose: Analytics - service popularity over time
Impact: Slow reporting queries
```

### 10. Earnings by Date Range
```
Collection: monthlySettlements
Fields: yearMonth (ASC) + totalEarnings (DESC)
Purpose: Analytics - earnings trends over time
Impact: Slow financial reporting
```

### 11. Payout Requests by Amount Range
```
Collection: payoutRequests
Fields: requestedAmount (ASC) + requestedAt (DESC)
Purpose: Analytics - payout amount distribution
Impact: Slow payout analytics
```

---

## 🟢 LOW PRIORITY INDEXES (ADVANCED FEATURES)

### 12. Failed Payouts Tracking
```
Collection: payoutRequests
Fields: requestStatus (ASC) + failureReason (ASC)
Purpose: Error analysis and retry logic
Impact: Only affects error reporting
```

### 13. Bank Account Verification Status
```
Collection: bankAccounts
Fields: isVerified (ASC) + createdAt (DESC)
Purpose: Admin - find unverified accounts
Impact: Only affects admin verification workflow
```

---

## 🏗️ INDEX CREATION INSTRUCTIONS

### Method 1: Firebase Console (Recommended)
1. Go to **Firebase Console** → **Firestore** → **Indexes**
2. Click **"Create Index"**
3. Fill in **Collection ID** and **Fields to Index**
4. Click **"Create Index"**
5. Wait for **"Enabled"** status (5-10 minutes)

### Method 2: Firebase CLI
```bash
firebase firestore:indexes
# Edit firestore.indexes.json
firebase deploy --only firestore:indexes
```

### Method 3: Programmatic (Auto-Created)
Some indexes are auto-created when queries run, but this is unreliable for production.

---

## 📊 FIRESTORE.INDEXES.JSON FORMAT

```json
{
  "indexes": [
    {
      "collectionGroup": "bookings",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "partnerId", "order": "ASCENDING" },
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "completedAt", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "payoutRequests",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "partnerId", "order": "ASCENDING" },
        { "fieldPath": "requestedAt", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "monthlySettlements",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "partnerId", "order": "ASCENDING" },
        { "fieldPath": "yearMonth", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "bankAccounts",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "partnerId", "order": "ASCENDING" }
      ]
    }
  ]
}
```

---

## 🚨 PRODUCTION DEPLOYMENT CHECKLIST

### Before Going Live:
- [ ] ✅ **Critical indexes created and enabled**
- [ ] ✅ **High priority indexes created**
- [ ] ✅ **Test all queries in staging environment**
- [ ] ✅ **Monitor index build progress**
- [ ] ✅ **Verify no FAILED_PRECONDITION errors**

### Monitoring:
- [ ] 🔍 **Check Firebase Console** → **Firestore** → **Indexes** regularly
- [ ] 📊 **Monitor query performance** in Firebase Console
- [ ] 🚨 **Set up alerts** for failed index builds

### Cost Considerations:
- **Free Tier:** 200 indexes included
- **Blaze Plan:** $0.06 per index per month (after free tier)
- **Composite indexes cost more than single-field indexes**

---

## 🎯 QUERY PATTERNS COVERED

### User-Facing Queries:
1. ✅ **Earnings loading** → `bookings` by partner + status + date
2. ✅ **Payout history** → `payoutRequests` by partner + date
3. ✅ **Settlement history** → `monthlySettlements` by partner + month
4. ✅ **Bank account lookup** → `bankAccounts` by partner

### Admin Queries:
1. 🟡 **Settlement calculation** → `bookings` by status + date range
2. 🟡 **Payout approval queue** → `payoutRequests` by status + date
3. 🟡 **Settlement processing** → `monthlySettlements` by status + date

### Analytics Queries:
1. 🟠 **Service popularity** → `bookings` by service + date
2. 🟠 **Revenue trends** → `monthlySettlements` by month + earnings
3. 🟠 **Payout distribution** → `payoutRequests` by amount + date

---

## 🚨 TROUBLESHOOTING FAILED_PRECONDITION

### Error Message:
```
FAILED_PRECONDITION: The query requires an index. You can create it here: [LINK]
```

### Immediate Actions:
1. **Click the provided link** to auto-create the index
2. **Wait 5-10 minutes** for index to build
3. **Test the query again**
4. **Add the index** to your index planning document

### Prevention:
- Always test queries in staging with production-like data
- Create indexes proactively, not reactively
- Monitor for new index requirements during development

---

## 📈 INDEX PERFORMANCE OPTIMIZATION

### Index Usage Patterns:
- **Equality filters first** (partnerId, status)
- **Inequality/range filters second** (dates, amounts)
- **OrderBy fields last** (completedAt, requestedAt)

### Best Practices:
- **Limit query results** (use `.limit(100)`)
- **Avoid OR queries** (use separate queries)
- **Prefer equality over range filters**
- **Index only what you query**

### Monitoring Tools:
- **Firebase Console** → **Firestore** → **Usage** tab
- **Cloud Monitoring** for performance metrics
- **Query profiling** in development

---

## 🎉 SUCCESS CRITERIA

✅ **All critical indexes created and enabled**
✅ **No FAILED_PRECONDITION errors in production**
✅ **Query response times < 1 second**
✅ **Admin dashboard loads quickly**
✅ **User earnings load instantly**
✅ **Payout requests process smoothly**

**The indexing strategy is now complete and production-ready!** 🎯
