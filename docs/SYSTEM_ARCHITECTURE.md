# System Architecture

AI-ShieldMe is organized in four logical tiers (see `ai-memory/ARCHITECTURE.md` for the package map).

## Tier responsibilities
| Tier | Responsibility | Talks to |
|---|---|---|
| 1 User Interface | Compose screens, ViewModels, voice/text input, consent UI | Tier 3 (orchestrator API), Tier 2 (repositories via ViewModels) |
| 2 Secure Data Vault | Encrypted storage, validation, de-duplication, audit | — (no network) |
| 3 AI Negotiator | Intent, agents, policy/consent, workflows, on-device AI | Tier 2 repositories, Tier 4 |
| 4 Vendor Interaction | Vendor verification, OCR, request/response, transactions | Tier 2 via `AuthorizedDataAccess`; outside organizations via authorized channels |

## Workflow A — Healthcare check-in
User: "help me check in for my doctor appointment" → `IntentRouter` (rules) → `HealthcareAgent`
picks the next appointment and its clinic → `VendorVerification` (registered host) →
`ConsentPolicyEngine` (purpose = APPOINTMENT_CHECK_IN, minimum-necessary categories) →
`VendorInteractionManager.prepare` releases only those fields and creates an `AWAITING_APPROVAL`
request → `ConsentApprovalSheet` shows vendor, purpose, destination, operation and every field →
user approves → consent recorded (24 h) → connector submits (FHIR/REST, or mock) →
`ResponseManager` records `CONFIRMED` with the vendor's code → appointment `check_in_status =
CHECKED_IN`. If the clinic has no authorized channel, the manual connector returns guided steps
and the user enters the clinic's confirmation number.

## Workflow B — Retail return
`RetailAgent` finds the most recent purchase (or the named merchant), checks the return window,
proposes a return with the user's stated reason → same verify/policy/approve path → mock vendor
replies "accepted" (`SUBMITTED`, RMA reference) → status check → `CONFIRMED`; a
`return_requests` row tracks the state.

## Workflow C — Banking charge investigation / dispute
Investigation is answered locally (matching transactions, how often the merchant appears) —
nothing is sent. "Dispute this charge" → `BankingAgent` prepares a dispute with account last-4,
amount, date and reason only → approval → submission (or guided steps through the bank's official
channel) → `transaction_disputes` row tracks state.

## Workflow D — Document import
Scan (ML Kit Document Scanner) or photo → `MLKitOCRManager` (on-device text recognition) →
`DocumentClassifier` → `FormFieldExtractor` (rules; Gemini Nano fills gaps when available) →
`FormValidator` → user edits/approves fields and sees the proposed vault changes → Tier 2 stores
the document (content-hash de-duplication) and applies changes through repositories (purchase,
insurance, medications, statement + de-duplicated transactions).

## Offline behaviour
Works offline: vault, all local lookups, rule-based intent recognition, on-device OCR, Gemini Nano
(when the model is present), draft preparation and consent review. Requests that need the network
are queued in `APPROVED` and resumed by `TransactionTracker` when connectivity returns; the UI says
"Queued · not sent". Nothing is ever reported as submitted while offline.
