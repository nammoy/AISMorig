# AI-ShieldMe — Database

- **Class:** `tier2_vault.AppDatabase` — Room 2.7.1, **schema version 2**, `exportSchema = true`,
  schemas in `app/schemas/com.app.aisheildme.bytrix.tier2_vault.AppDatabase/{1,2}.json`.
- **File:** `checkin_history.db` (name kept from v1 so existing data is preserved).
- **Encryption:** SQLCipher (`net.zetetic:sqlcipher-android:4.6.1`) via `SupportOpenHelperFactory`.
  Passphrase = 32 random bytes (hex), sealed with an AndroidKeyStore AES-256-GCM key
  (`aishieldme.vault.db`) and stored in private prefs `aishieldme_vault_keys`. On first launch the old
  plaintext file is converted in place with `sqlcipher_export` (`DatabaseEncryption`); on failure
  the original is left untouched and the error propagates.
- **Integrity:** `PRAGMA foreign_keys = ON` on every open (`VaultModule.ForeignKeysOn`).
- **Backups:** `android:allowBackup="false"` (unchanged). The Keystore key is device-bound, so a
  copied DB file is unreadable elsewhere. A user-initiated encrypted export is future work.

## Migrations
| From → To | How | Notes |
|---|---|---|
| 1 → 2 | `@AutoMigration(from = 1, to = 2)` | Additive only: creates the vault tables, keeps `checkin_ai_summaries`. Tested by `VaultMigrationTest`. |

Rules: never `fallbackToDestructiveMigration`; every schema change bumps the version, exports a
schema JSON and adds a migration test.

## Tables (v2)
Conventions: `INTEGER` UTC epoch-ms timestamps; money in minor units (`*_minor`); enums stored by name.

**Identity & security** — `users` (unique email, external_id) · `user_profiles` (1:1) ·
`user_settings` (cloud AI off by default) · `authentication_metadata` (PBKDF2 PIN hash, salt,
iterations, failed_attempts, locked_until — never the PIN) · `consent_authorizations`
(user, vendor, purpose, categories, status, expiry) · `access_policies` (unique domain+category+purpose;
seeded from `DefaultPolicies`) · `audit_events` (sanitized; no PHI/secrets).

**Healthcare** — `patients` (1 per user, fhir_id) · `healthcare_organizations` (→ vendors, unique
name+address) · `providers` (unique NPI) · `appointments` (unique patient+provider+time, check_in_status)
· `medications` · `medical_documents` (→ vendor_documents) · `insurance_policies` (unique
patient+payer+member_id).

**Retail** — `merchants` (unique name, → vendors) · `products` (unique merchant+sku) · `purchases`
(unique merchant+order_number) · `purchase_items` · `receipts` (1 per purchase, → vendor_documents)
· `warranties` (1 per item) · `return_requests` (→ interaction_requests).

**Banking** — `financial_institutions` (unique name, dispute_channel) · `financial_accounts`
(last4 only; unique institution+type+last4) · `financial_transactions` (unique account+fingerprint
for de-duplication) · `bank_statements` (unique account+period) · `transaction_disputes`.

**Vendor interaction** — `vendors` (unique name+domain; shared by all domains) · `vendor_services` ·
`vendor_connections` (endpoint, auth type, Keystore credential alias, cert pin) · `vendor_documents`
(unique content_hash) · `document_fields` (unique document+key, user_confirmed) ·
`interaction_requests` (unique request_uuid and idempotency_key) · `interaction_responses` ·
`interaction_transactions` (1 per request: state, attempts, confirmed_at) · `interaction_audits` ·
`workflows`.

Legacy: `checkin_ai_summaries` (v1, unchanged).

## Access rules
UI → ViewModel → repository. Agents and Tier 4 never get DAOs; Tier 4 reads personal data only via
`AuthorizedDataAccess.release(userId, authorizedCategories, subject)`, which is audited.
