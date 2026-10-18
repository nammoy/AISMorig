# Security Design

| Concern | Control |
|---|---|
| Data at rest | SQLCipher-encrypted Room DB; passphrase sealed by AndroidKeyStore AES-256-GCM |
| Session token | Sealed with Keystore; removed from cached profile JSON; legacy plaintext migrated on first read |
| Vault PIN | PBKDF2-HMAC-SHA256 (120k iterations, 16-byte salt), constant-time compare, lockout after 5 failures (30 s doubling, max 15 min) |
| Biometrics | Existing `BiometricPrompt` unlock (unchanged) |
| Session/vault timeout | Existing in-memory unlock window (`pin_expiry_time`); app restart re-prompts |
| Authorization | Purpose → category policy matrix (`DefaultPolicies`), consent records with expiry, deterministic `ConsentPolicyEngine` |
| Minimum necessary | `AuthorizedDataAccess` releases only approved categories; `FormMapper` drops unapproved note keys |
| Vendor identity | Registry host match + HTTPS + optional cert pin; re-verified before send |
| Integrity of submissions | Idempotency keys, enforced state machine, no blind retries, audit trail per transition |
| AI safety | Rules first; AI output allow-listed and validated; OCR text treated as data; cloud AI disabled |
| Audit | `audit_events` (sanitized), `interaction_audits` |
| Backups | `allowBackup=false`; DB unreadable off-device (Keystore-bound key) |

## Known gaps / follow-ups
1. **Rotate the backend app token** — it was committed in git history (`app/build/.../BuildConfig.java`).
2. Release builds are signed with the debug keystore (pre-existing).
3. Debug builds log full HTTP bodies (`HttpLoggingInterceptor.Level.BODY`) in the legacy stack.
4. The legacy cached `User` profile in DataStore (PII, no token) is not encrypted.
5. The check-in form's existing backend submission (`completeProfile`) predates the consent
   engine; route it through Tier 4 with a consent step.
6. Encrypted user export/backup of the vault is not implemented.
