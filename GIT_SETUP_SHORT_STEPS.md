# Git Setup - Quick Steps

## Step 1: Configure Git (Username & Email)

Open **Command Prompt** or **PowerShell** and run:

```bash
git config --global user.name "chaitanykakde"
git config --global user.email "your-email@example.com"
```

**Replace `your-email@example.com` with your GitHub email address.**

---

## Step 2: Check Current Config

Verify it's set:

```bash
git config --global user.name
git config --global user.email
```

---

## Step 3: Set Up Authentication (Choose ONE method)

### Option A: Personal Access Token (Recommended - Easiest)

1. **Create Token on GitHub**:
   - Go to: https://github.com/settings/tokens
   - Click: **"Generate new token"** → **"Generate new token (classic)"**
   - Name: `Serveit-Partner-New`
   - Select scopes: Check **`repo`** (full control of private repositories)
   - Click: **"Generate token"**
   - **COPY THE TOKEN** (you'll only see it once!)

2. **Use Token When Pushing**:
   - When Git asks for password, use the **token** instead of your GitHub password
   - Username: `chaitanykakde`
   - Password: **paste your token**

### Option B: GitHub CLI (Alternative)

```bash
# Install GitHub CLI (if not installed)
# Download from: https://cli.github.com/

# Login
gh auth login
# Follow prompts: GitHub.com → HTTPS → Login with web browser
```

---

## Step 4: Push Your Code

After authentication is set up:

```bash
# Navigate to project
cd "C:\Users\Chaitany Kakde\StudioProjects\Serveit-Partner-New"

# Check status
git status

# Initialize if needed
git init

# Add remote
git remote add origin https://github.com/chaitanykakde/Serveit-Partner-New.git

# Stage all files
git add .

# Commit
git commit -m "Fix Cloud Functions compatibility: Add mobileNo field and onboarding validation"

# Push (will prompt for username/password - use token as password)
git push -u origin main
```

---

## Quick One-Liner Setup

Run these commands one by one:

```bash
# 1. Set your email (replace with your GitHub email)
git config --global user.email "your-email@example.com"
git config --global user.name "chaitanykakde"

# 2. Check it's set
git config --global --list

# 3. Navigate to project
cd "C:\Users\Chaitany Kakde\StudioProjects\Serveit-Partner-New"

# 4. Initialize and push
git init
git remote add origin https://github.com/chaitanykakde/Serveit-Partner-New.git
git add .
git commit -m "Fix Cloud Functions compatibility"
git push -u origin main
```

**When prompted for password**: Use your **Personal Access Token** (not your GitHub password).

---

## Troubleshooting

### If "remote origin already exists":
```bash
git remote set-url origin https://github.com/chaitanykakde/Serveit-Partner-New.git
```

### If authentication fails:
- Make sure you're using **Personal Access Token**, not password
- Token must have `repo` scope
- Username should be: `chaitanykakde`

### To save credentials (Windows):
```bash
git config --global credential.helper wincred
```

---

**That's it!** After Step 3, you'll be authenticated and can push code.

