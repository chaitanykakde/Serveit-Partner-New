# Quick Guide: Check Firebase Cloud Functions Logs

## 🚀 FASTEST WAY: Firebase Console (No Installation)

**Direct Link to Logs:**
https://console.firebase.google.com/project/serveit-1f333/functions/logs

### What to Check:

1. **Filter by Function**: `dispatchJobToProviders`
   - This is the function that should trigger when bookings are created
   - Look for recent executions (last 24 hours)

2. **Look for These Patterns:**
   - ✅ **Success**: "New booking created: {bookingId}"
   - ✅ **Success**: "Found X potential providers within Xkm"
   - ✅ **Success**: "X providers qualified after distance filtering"
   - ✅ **Success**: "Created inbox entries for X providers"
   - ❌ **Error**: "Zero providers found for booking"
   - ❌ **Error**: "Zero providers qualified after distance filtering"
   - ❌ **Error**: "No new booking detected"
   - ❌ **Error**: "Provider X: Missing coordinates"
   - ❌ **Error**: "Provider X: Not verified"

3. **Check Recent Bookings:**
   - Look for logs from the last few hours
   - See if `dispatchJobToProviders` is being triggered at all
   - Check if it's finding providers but they're not qualified

---

## 🔧 CLI Method (If You Want Command Line)

### Step 1: Install Node.js
1. Download from: https://nodejs.org/
2. Install the **LTS version** (recommended)
3. **Restart your terminal/PowerShell** after installation

### Step 2: Install Firebase CLI
```powershell
npm install -g firebase-tools
```

### Step 3: Login
```powershell
firebase login
```
This will open your browser to authenticate.

### Step 4: Set Project
```powershell
cd "C:\Users\Chaitany Kakde.DESKTOP-IRCBJUJ\StudioProjects\Serveit-Partner-New"
firebase use serveit-1f333
```

### Step 5: Check Logs
```powershell
# All logs
firebase functions:log

# Specific function (most important)
firebase functions:log --only dispatchJobToProviders

# Last 50 lines
firebase functions:log --limit 50

# Last 24 hours
firebase functions:log --since 1d

# Real-time streaming
firebase functions:log --follow
```

---

## 🔍 What to Look For in Logs

### Critical Log Messages:

1. **Booking Detection:**
   ```
   ✅ "New booking created: {bookingId}"
   ❌ "No new booking detected" (means trigger didn't detect new booking)
   ```

2. **Provider Discovery:**
   ```
   ✅ "Found X potential providers within Xkm"
   ❌ "[MONITORING] Zero providers found for booking"
   ```

3. **Provider Qualification:**
   ```
   ✅ "X providers qualified after distance filtering"
   ❌ "[MONITORING] Zero providers qualified after distance filtering"
   ```

4. **Inbox Creation:**
   ```
   ✅ "Created inbox entries for X providers"
   ❌ "Error updating booking document" (inbox creation failed)
   ```

5. **Provider-Specific Issues:**
   ```
   ❌ "Provider {id}: Missing coordinates"
   ❌ "Provider {id}: Not verified"
   ❌ "Provider {id}: REJECTED - service mismatch"
   ❌ "Provider {id}: REJECTED - outside longitude bounds"
   ```

---

## 📊 Expected Flow in Logs

When a booking is created, you should see this sequence:

1. `New booking created: {bookingId}`
2. `Found user coordinates: {lat}, {lng}` OR `Using fallback coordinates`
3. `Service requested: {serviceName}`
4. `Found X potential providers within Xkm`
5. `X providers qualified after distance filtering`
6. `Updated booking {bookingId} with X notified providers`
7. `Created inbox entries for X providers`
8. `Wake-up notification sent to provider {id}` (for each provider)
9. `Job dispatch completed for booking {bookingId} - X providers notified`

**If any step is missing, that's where the issue is!**

---

## 🎯 Next Steps After Checking Logs

Based on what you find:

1. **If `dispatchJobToProviders` is NOT triggering:**
   - Check if bookings are being created in Firestore
   - Verify the trigger is set up correctly
   - Check if function is deployed

2. **If function triggers but finds ZERO providers:**
   - Check provider documents in `partners` collection
   - Verify `isVerified == true`
   - Check `locationDetails.latitude/longitude` exist
   - Verify `services[]` array matches requested service

3. **If providers found but ZERO qualified:**
   - Check distance calculations
   - Verify Google Maps API key is valid
   - Check `finalDistanceLimit` configuration

4. **If inbox entries created but Partner App doesn't show:**
   - Check Firestore security rules for `provider_job_inbox`
   - Verify Partner App is querying correct collection
   - Check if inbox entries have correct `status` and `expiresAt`

---

**Start with the Firebase Console link above - it's the fastest way to see what's happening!**
