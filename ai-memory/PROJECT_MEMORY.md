# AI-ShieldMe — Project Memory

_Persistent context for human developers and AI coding agents. Read `CURRENT_STATE.md` next._

## Identity
- **Repository:** `nammoy/AI-ShieldMe-Frontend-android-native` (working copy: `C:\AA-Orig10`).
- **Module / package:** `:app`, `com.app.aisheildme.bytrix` (spelling "aisheildme" is the shipped
  application ID — do not "fix" it; changing it would create a different app).
- **Stack:** Kotlin 2.2, AGP 8.11.1, Gradle 8.13, Jetpack Compose (BOM 2024.12.01) + Material 3,
  Hilt, Room 2.7.1 + SQLCipher 4.6.1, Retrofit/OkHttp, ML Kit (GenAI Prompt = Gemini Nano,
  Document Scanner, Text Recognition), Firebase Messaging. minSdk 26, target/compile 35, JVM 17.
- **Origin:** native port of a Flutter app (see `MIGRATION.md`), extended in Oct 2026 into the
  four-tier architecture below.

## Product
A privacy-first personal assistant: the user's healthcare, retail and banking information lives in
an encrypted on-device vault; an AI "negotiator" understands requests and plans actions; a vendor
tier talks to outside organizations — only through verified channels and only after the user
approves exactly what will be shared.

## Four tiers (logical organization, not a sequential pipeline)
| Tier | Package | Role |
|---|---|---|
| 1 UI | `tier1_ui/*` + existing `feature/*` | Compose screens + ViewModels |
| 2 Secure Data Vault | `tier2_vault/*` | Encrypted Room DB, repositories, security |
| 3 AI Negotiator | `tier3_negotiator/*` | Orchestrator, intent router, agents, policy, workflows, Gemini Nano |
| 4 Vendor Interaction | `tier4_vendor/*` | Verification, connectors, OCR, requests/transactions |

Runtime relationships: Tier 1 → Tier 3 (orchestrator API); Tier 1 → Tier 2 (ViewModels →
repositories); Tier 3 → Tier 2 (repositories) and → Tier 4; Tier 4 → Tier 2 only through
`AuthorizedDataAccess` with the categories the policy engine allowed. The DB is never a message bus.

## Key decisions (Oct 2026, approved by the owner)
1. **Local-first:** the Room vault is authoritative. The existing backend
   (`aishield.facevitals.ai`) remains for login, documents and the legacy industry screens; it is
   to be treated as one more Tier 4 vendor for any future record sync (consent-gated).
2. **Login stays server-based; vault PIN and biometrics are verified on-device.** PINs set on the
   server before the change are adopted locally after one successful server check.
3. **Builds run in Android Studio on the owner's PC** (cloud containers cannot reach Maven/Google).
4. Sample vendors and data are synthetic and clearly labelled "(sample)".

## Non-negotiable rules
See `CODING_RULES.md`. In short: never bypass consent/policy, never give AI raw DB access, never
claim a vendor transaction succeeded without confirmation, never commit secrets or real PHI/financial
data, no destructive migrations.
