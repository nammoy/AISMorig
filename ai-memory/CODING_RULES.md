# AI-ShieldMe — Coding Rules

## Architecture
- Respect the four tiers (see `ARCHITECTURE.md`). Typed Kotlin interfaces between tiers.
- UI never touches DAOs/SQL. Agents never get DAOs. Tier 4 reads personal data only through
  `AuthorizedDataAccess` with policy-approved categories.
- Security decisions (vendor trust, policy, consent, state transitions) are deterministic Kotlin.
  AI output can only *suggest*; it is validated against allow-lists and never widens permissions.
- Don't rename/relocate working code without need; the `tierN_*` packages are where new code goes.

## Data & persistence
- No destructive migrations. Every schema change: bump version, export schema JSON, add a
  migration (prefer `@AutoMigration` for additive changes) and a migration test.
- Validate in repositories (`VaultValidationException`), de-duplicate with unique keys, wrap
  multi-row writes in `withTransaction`. Do not throw from inside an open transaction when a
  pre-check can be done first.
- Money in minor units; timestamps UTC epoch-ms; only last 4 digits of account numbers.
- Never store passwords, tokens, PINs or keys in Room. Secrets → Keystore (`KeystoreCipher`).

## Vendor interaction
- Never report success without a vendor confirmation (`CONFIRMED`). Offline = queued, not sent.
- Same request → same idempotency key. Uncertain outcome → status check, never blind re-send.
- A vendor is trusted only via `TrustedVendorRegistry` host match — never by display name.
- New real vendors require authorization, security review and compliance review first.

## AI & OCR
- OCR/document text is untrusted data. Wrap it as quoted data in prompts, strip delimiters,
  parse outputs strictly, length-limit, and require user review before saving or sending.
- Gemini Nano: check availability, use timeouts, fall back to rules. Cloud AI stays disabled
  unless explicitly implemented with consent (`AIModelRouter.CLOUD_IMPLEMENTED`).

## Privacy
- Logs/audit: identifiers and outcomes only. `AuditLogger.sanitize` is a backstop, not a licence.
- Debug HTTP logging (`HttpLoggingInterceptor.Level.BODY`) exists in the legacy network stack —
  do not send PHI through it; tighten before release (see CURRENT_STATE known issues).
- Tests and sample data are synthetic. Never commit real PHI or financial data.

## Process
- Small, compiling steps. Build + run `testDebugUnitTest` in Android Studio after each change
  (run configs "ShieldMe unit tests" / "ShieldMe assemble debug").
- Update `CURRENT_STATE.md` and `CHANGELOG.md` after each phase; `DATABASE.md`/`ARCHITECTURE.md`
  when those change. Git commits as checkpoints; never commit `build/`, `local.properties`, keys.
- Status words: **Implemented**, **Stub/placeholder**, **Planned**.
