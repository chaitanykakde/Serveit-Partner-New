This directory contains **development-only** and **deprecated** backend utilities for the Serveit Partner Firebase Functions backend.

Do **NOT** deploy anything from here to production.

## Contents

- `functions/`
  - Backup copies of debug/test Cloud Functions that should **not** be exported in production.
- `scripts/`
  - Local Node.js scripts used for manual testing, inspection, or data fixes.

## Usage Guidelines

- These utilities are intended for:
  - Local development
  - One-off debugging
  - Manual verification of data flows
- They must **never** be wired into production exports.

## Restoring a Dev-Only Function

1. Locate the function file under `_dev_only/functions/`.
2. Copy the function implementation into a non-dev module if you need to reuse it.
3. Wire it through `index.js` with the same export name if you intentionally want it available.

## Scripts

Scripts under `_dev_only/scripts/` are meant to be run manually with Node.js, for example:

```bash
cd functions
node _dev_only/scripts/check-pending-payouts.js
```

Always verify:
- You are pointing at the **correct Firebase project**.
- You understand the side effects before running any script.


