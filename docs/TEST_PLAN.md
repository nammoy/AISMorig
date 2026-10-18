# Test Plan

Run in Android Studio: run configuration **"ShieldMe unit tests"** (`:app:testDebugUnitTest`).
All tests use synthetic data. Robolectric tests run Room on plain SQLite (encryption itself is
verified on a device — see Manual).

| Area | Test class | Covers |
|---|---|---|
| Vault security | `vault.PinSecurityTest` | PBKDF2 verify, salting, lockout schedule, audit sanitizer |
| State machine | `vault.TransactionStateTest` | Allowed / forbidden transitions, terminal states |
| Room / DAO | `vault.VaultDatabaseTest` | FKs enforced, duplicates rejected (appointments, purchases, transactions, documents), validation, consent expiry/revoke, idempotent requests, transitions + audits, local PIN lockout |
| Migration | `vault.VaultMigrationTest` | v1 → v2 keeps check-in history, creates vault tables |
| Intent | `negotiator.IntentRouterTest` | Every spec example request, slots, wake-word stripping |
| Policy | `negotiator.ConsentPolicyEngineTest` | Unverified vendor denied, out-of-purpose categories blocked, consent coverage |
| OCR / forms / exchange | `vendor.OcrPipelineTest` | Classification, extraction, validation, mapping, prompt-injection inertness, AI JSON allow-list, payload minimization, CSV safety |
| End-to-end | `negotiator.WorkflowEndToEndTest` | A check-in, B return, C investigation → dispute, D document import; look-alike vendor blocked; cancel shares nothing; offline/uncertain outcomes; minimal payloads |
| Existing | `checkin.*`, `core.*`, `data.*` | Pre-existing parsing/validation tests (unchanged) |

## Manual / on-device checks (Pixel 10 Pro XL connected)
1. Fresh install of the previous build with check-in history → install this build → history still visible; DB file header is no longer `SQLite format 3`.
2. Set PIN, kill app, unlock with PIN and biometrics; 5 wrong PINs → lockout message.
3. Personal Vault → Load sample data → run workflows A–D from the Daily AI Assistant (text and voice).
4. Airplane mode: assistant answers locally; approval of a request on an HTTPS vendor shows "Queued · not sent".
5. Gemini Nano: on a supported device the status bar shows "ready"; on others "using rules".
6. Import a real-world-looking *synthetic* receipt photo; edit a field; save; import again → "Already in your vault".
