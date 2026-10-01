# Guest progress and account migration

Guest users are not stored in PostgreSQL. The frontend stores guest progress in localStorage and sends it to the backend only after registration or login succeeds.

## localStorage format

Use the key `dsa-hub.guest-progress.v1` and keep the version in the value so a future format can be migrated deliberately:

```json
{
  "version": 1,
  "syncProgressAcrossSheets": true,
  "problemProgress": {
    "lc-1": {
      "problemId": "lc-1",
      "solved": true,
      "solvedAt": "2026-10-02T10:15:00Z",
      "notes": "Review the two-pointer invariant",
      "isBookmarked": true,
      "revisionStatus": "review",
      "userDifficulty": "Medium",
      "lastAttemptedAt": "2026-10-02T10:15:00Z"
    }
  },
  "sheetProblemProgress": {}
}
```

When `syncProgressAcrossSheets` is false, keep `problemProgress` empty and store entries under `sheetProblemProgress`, keyed by sheet ID and problem ID (for example, `blind-75/lc-1`). Each entry has the same progress fields plus `sheetId` and `problemId`. Use ISO-8601 timestamps or `null`; fields `solved` and `isBookmarked` are required booleans. A new guest starts with empty maps. Save a guest progress change to localStorage immediately so closing the browser does not lose it.

## Migration request

After registration/login returns a JWT, send the version-1 data to `POST /api/progress/migrate-guest` with `Authorization: Bearer <token>`. The endpoint takes its user ID from the JWT; do not send `user_id`.

The HTTP body uses arrays (convert the localStorage maps to arrays):

```json
{
  "syncProgressAcrossSheets": true,
  "problemProgress": [
    {
      "problemId": "lc-1",
      "solved": true,
      "solvedAt": "2026-10-02T10:15:00Z",
      "notes": "Review the two-pointer invariant",
      "isBookmarked": true,
      "revisionStatus": "review",
      "userDifficulty": "Medium",
      "lastAttemptedAt": "2026-10-02T10:15:00Z"
    }
  ],
  "sheetProblemProgress": []
}
```

With sync enabled, only `problemProgress` is imported into global progress. With sync disabled, only `sheetProblemProgress` is imported into sheet-specific progress. Keep the other array empty. The request mode must match the authenticated account's existing `sync_progress_across_sheets` setting. A mismatch returns `409 Conflict` without saving anything; this avoids importing data into a storage mode the account's progress APIs will not read. For a new account, the database trigger sets this preference to true. If the user wants to retain guest sheet-specific mode, ask them to choose that preference through the normal settings flow before retrying migration. Migration never changes settings itself.

All problem IDs and sheet/problem memberships are checked before any progress is saved. A missing reference, duplicate entry, invalid field, or mismatched mode fails the request, and the frontend should retain the guest data. On successful `200 OK`, the response is:

```json
{
  "success": true,
  "migratedProblems": 1,
  "migratedSheetProblems": 0
}
```

Only clear `dsa-hub.guest-progress.v1` after receiving a successful response. Network errors, `400`, `401`, or `409` are not success; keep the data and allow the user to retry after correcting the request or authenticating. Retrying a successful migration is safe: existing meaningful account values are retained, solved/bookmarked flags are merged monotonically, latest timestamps are retained, and missing text/status/difficulty values may be filled from guest data. Attempt history and existing progress are never deleted.
