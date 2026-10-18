# AI-ShieldMe — Change Log

Newest first. Dates are when the work was done; verify against `git log`.

## 2026-10-18 — Four-tier architecture (Phases 1–12, MVP)
### Repository hygiene
- `.gitignore` added; `app/build/`, `.gradle/`, `.idea/`, `local.properties` untracked (they had
  been committed, including a generated `BuildConfig` with the backend app token → **rotate it**).
### Added
- **Tier 2** `tier2_vault/`: Room v2 (37 tables) + AutoMigration 1→2, SQLCipher in-place
  encryption with Keystore-sealed passphrase, local PBKDF2 vault PIN with lockout, Keystore-sealed
  session token, repositories with validation/de-duplication, `AuthorizedDataAccess`, audit log.
- **Tier 3** `tier3_negotiator/`: `AIOrchestrator`, `IntentRouter`, agents (healthcare, retail,
  banking, document import, general, vendor selection), `ConsentPolicyEngine`, `WorkflowManager`,
  `GeminiNanoManager`, `AIModelRouter`, `DocumentImportService`, `SampleData`.
- **Tier 4** `tier4_vendor/`: `VendorInteractionManager`, `VendorVerification` +
  `TrustedVendorRegistry`, connectors (mock, authorized HTTPS/FHIR, manual), Request/Response
  managers, `TransactionTracker`, OCR pipeline, `FhirMapper`, `DocumentExchange`.
- **Tier 1** `tier1_ui/`: Daily AI Assistant (text + voice), consent approval sheet, Personal Vault,
  Document Import, Activity (requests / consents with revoke / history); dashboard tiles gated by
  the vault PIN; `TransactionTracker` started at app launch.
- Docs: `ai-memory/*`, `docs/*`. Android Studio run configs "ShieldMe unit tests" / "ShieldMe assemble debug" (in `.idea/`, not committed).
### Changed
- `AuthRepository` set/verify/reset PIN → local-first (server best-effort, legacy PIN adoption).
- `SessionStore` token sealing; `DataModule` no longer builds the DB (moved to `VaultModule`).
- `AppDatabase` moved to `tier2_vault` (same file name `checkin_history.db`).
### Database / Migration
- v1 → v2 additive AutoMigration; schemas exported to `app/schemas/`.
### Validation
- Tiers 2–4: 109 unit/Robolectric tests passing in Android Studio (incl. migration + 4 E2E workflows).
- Tier 1: see CURRENT_STATE for the latest build/test result.

## 2026-10-07 — AI Memory baseline (superseded)
Described a different, smaller snapshot (`com.aishieldme.app`); replaced by the files above.
