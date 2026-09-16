# Local-first data protection

Routive keeps Room as the on-device store. Accounts must remain optional; daily tracking must work offline.

## Phase 1 — File backup and restore

Implemented in the app under **My Routines → 백업 · 복원**:

- User-selected JSON export/import through Android's Storage Access Framework; no broad storage permission.
- Format version `1`, separate from Room schema version `12`.
- Import/export share a 20 MiB file limit, 100,000 rows per table and 100,000 characters per text field.
- All five user-data tables plus the original history start date.
- Consistent snapshot and transactional replacement, strict field types, duplicate-key and relationship validation.
- Restore preview and explicit replace confirmation, including empty-backup warnings.
- Last successful local export timestamp. This is not a guarantee that a document provider finished uploading the file.
- Health Connect source records, permissions and device-specific preferences are excluded.
- Files are plaintext; users must protect them. No automatic upload, account or remote service exists yet.

Verification: existing unit tests plus `RoutineBackupTest` on an emulator cover round-trip field preservation, nullable weight, invalid formats/types/relationships, input size, repeated replacement, failed-write rollback, and the 11→12 migration. Older migration chains and Android system backup recovery still need separate release testing.

## Phase 2 — Optional account and cloud backup

Current progress: Supabase public client configuration and email/password sign-up, sign-in, and sign-out are implemented. The app remains usable without an account. Session refresh and cloud backup upload/restore are still pending.

Before implementation, choose a backend owned by the app developer and configure its authentication project. Do not ship placeholders that imply working cloud protection.

- Guest use first; account connection only when users enable cloud backup.
- Explicitly explain the data being uploaded and obtain the user's choice.
- Store versioned backup snapshots scoped to the authenticated user; enforce ownership on the server, not only in the UI.
- Reuse the versioned backup format and import validation, with preview before replacement on a new phone.
- Show last successful backup, in-progress, offline/retry and error states accurately.
- Retain a previous good snapshot; interrupted uploads must not replace it.
- Configure transport/storage protection, retention and account/data deletion, with privacy disclosures.
- Test account switching, expiry, interrupted network access, restoration on another device and deletion end to end.
- Decide release application ID/signing and authentication fingerprints before public distribution.

This phase is backup/restore, not simultaneous multi-device editing. Never automatically overwrite local guest records after login.

## Phase 3 — Multi-device synchronization, only if needed

Define stable cross-device IDs, an outbox, revision/conflict rules and deletion tombstones before introducing bidirectional synchronization. Existing local numeric IDs are insufficient for merging independent devices. File restore will continue to mean explicit replacement, not merge.
