# Firebase CLI - Login and Check Cloud Functions Logs

## Step 1: Install Firebase CLI

### Option A: Install via npm (Requires Node.js)

1. **Install Node.js** (if not installed):
   - Download from: https://nodejs.org/
   - Install the LTS version
   - Restart your terminal after installation

2. **Install Firebase CLI globally**:
   ```bash
   npm install -g firebase-tools
   ```

### Option B: Install via Standalone Binary (Windows)

1. Download Firebase CLI installer:
   - Go to: https://github.com/firebase/firebase-tools/releases
   - Download latest `firebase-tools-win.exe`
   - Run the installer

---

## Step 2: Login to Firebase

Open **Command Prompt** or **PowerShell** and run:

```bash
firebase login
```

This will:
- Open your browser
- Ask you to login with your Google account (the one linked to your Firebase project)
- Authorize Firebase CLI
- Return to terminal showing "Success! Logged in as your-email@example.com"

**Alternative (non-interactive)**:
```bash
firebase login --no-localhost
```
This gives you a URL to visit and a code to enter.

---

## Step 3: Check Your Firebase Project

Navigate to your project directory:

```bash
cd "C:\Users\Chaitany Kakde\StudioProjects\Serveit-Partner-New"
```

List your Firebase projects:

```bash
firebase projects:list
```

Set the active project (if needed):

```bash
firebase use --add
# Select your project from the list
```

Or set directly:

```bash
firebase use YOUR_PROJECT_ID
```

---

## Step 4: Check Cloud Functions Logs

### View All Functions Logs

```bash
firebase functions:log
```

### View Logs for Specific Function

```bash
# Example: Check dispatchJobToProviders logs
firebase functions:log --only dispatchJobToProviders

# Check acceptJobRequest logs
firebase functions:log --only acceptJobRequest

# Check sendVerificationNotification logs
firebase functions:log --only sendVerificationNotification
```

### View Recent Logs (Last N Lines)

```bash
# Last 50 lines
firebase functions:log --limit 50

# Last 100 lines
firebase functions:log --limit 100
```

### View Logs with Filters

```bash
# Logs from last hour
firebase functions:log --since 1h

# Logs from last day
firebase functions:log --since 1d

# Logs with specific text
firebase functions:log | findstr "error"
```

### Real-time Log Streaming

```bash
# Watch logs in real-time
firebase functions:log --follow
```

---

## Step 5: List All Deployed Functions

```bash
firebase functions:list
```

---

## Quick Command Reference

```bash
# 1. Login
firebase login

# 2. Set project
cd "C:\Users\Chaitany Kakde\StudioProjects\Serveit-Partner-New"
firebase use YOUR_PROJECT_ID

# 3. View all logs
firebase functions:log

# 4. View specific function logs
firebase functions:log --only dispatchJobToProviders

# 5. View recent logs (last 50)
firebase functions:log --limit 50

# 6. Stream logs in real-time
firebase functions:log --follow
```

---

## Troubleshooting

### If "firebase: command not found":
- Make sure Firebase CLI is installed
- Restart terminal after installation
- Check if it's in PATH: `echo $PATH` (Git Bash) or `$env:PATH` (PowerShell)

### If login fails:
- Make sure you're using the Google account linked to your Firebase project
- Try: `firebase login --no-localhost`

### If "project not found":
- List projects: `firebase projects:list`
- Set project: `firebase use PROJECT_ID`

### If logs are empty:
- Make sure functions are deployed: `firebase functions:list`
- Check if functions have been triggered recently
- Try: `firebase functions:log --since 1d`

---

## Alternative: View Logs in Firebase Console

If CLI doesn't work, use the web console:

1. Go to: https://console.firebase.google.com/
2. Select your project
3. Go to: **Functions** → **Logs** tab
4. Filter by function name or time range

---

## Example: Check All Your Functions

```bash
# Login
firebase login

# Set project
firebase use YOUR_PROJECT_ID

# Check all functions
firebase functions:list

# Check logs for each function
firebase functions:log --only dispatchJobToProviders
firebase functions:log --only acceptJobRequest
firebase functions:log --only sendVerificationNotification
firebase functions:log --only sendDailyEarningsSummary
firebase functions:log --only sendJobNotification
firebase functions:log --only notifyCustomerOnStatusChange
```

---

**After installation, run these commands in order to login and check logs!**

