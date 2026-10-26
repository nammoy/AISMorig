# AI-ShieldMe — Architecture (as implemented)

```text
Tier 1  UI (Compose, MVVM)
  feature/*            existing screens (auth, dashboard, check-in, documents, profile, industries)
  tier1_ui/
    dailyassistant/    DailyAssistantScreen + VM (text + voice via RecognizerIntent)
    consent/           ConsentApprovalSheet (vendor, purpose, data, destination, operation, consent)
    vault/             PersonalVaultScreen + VM (local healthcare / retail / banking)
    documents/         DocumentImportScreen + VM (ML Kit scanner / photo → review → save)
    activity/          ActivityScreen + VM (requests, consents + revoke, audit history)
        │ typed calls
        ▼
Tier 3  AI Negotiator
  AIOrchestrator       handle → recognize → agent plan → verify vendor → policy → Tier 4 prepare
                       approve / cancel / refresh / confirmManually
  IntentRouter         deterministic regex rules; Gemini Nano only when rules are unsure
  AgentManager + agents/  Healthcare, Retail, Banking, DocumentImport, General, VendorSelection
  ConsentPolicyEngine  vendor verified → purpose policy (min. necessary) → consent; deterministic
  WorkflowManager      persisted workflow steps; resumable
  DocumentImportService  Workflow D coordinator
  SampleData           synthetic records + sample vendors
  ai/GeminiNanoManager availability, timeout, strict output parsing, fallback to rules
  ai/AIModelRouter     on-device vs rules (cloud disabled: CLOUD_IMPLEMENTED = false)
        │
        ▼
Tier 4  Vendor Interaction
  VendorInteractionManager  verify → channel → payload (AuthorizedDataAccess) → request → execute → response
  VendorVerification + TrustedVendorRegistry   identity = registered host, not display name
  VendorConnector      MockVendorConnector (mock://), AuthorizedApiConnector (HTTPS/FHIR, pinning,
                       Idempotency-Key, Keystore credentials), ManualProcessConnector (guided)
  transactions/        RequestManager (idempotency keys), ResponseManager (state mapping),
                       TransactionTracker (offline queue + status checks, never blind re-send)
  ocr/                 MLKitOCRManager, DocumentClassifier, FormFieldExtractor, FormMapper, FormValidator
  api/                 FhirMapper (R4 Patient/Coverage/AppointmentResponse/...), DocumentExchange (CSV/JSON)
        │
        ▼
Tier 2  Secure Data Vault
  AppDatabase (Room v2, SQLCipher)   entities/, dao/, schemas/ (1.json, 2.json)
  repository/          VaultIdentity, LocalPinVault, Healthcare/Retail/Banking/Vendor/
                       VaultDocument/Consent/AccessPolicy/Interaction/Workflow repositories,
                       AuthorizedDataAccess (scoped, audited data release)
  security/            KeystoreCipher, DatabaseKeyManager, DatabaseEncryption, PinHasher,
                       PinLockoutPolicy, AuditLogger
  model/               Domain, DataCategory, Purpose, TransactionState (+ allowed transitions)
```

## Request lifecycle (outbound)
`DRAFT → AWAITING_APPROVAL → APPROVED → PROCESSING → SUBMITTED → CONFIRMED`
with `FAILED`, `CANCELLED`, `MANUAL_ACTION_REQUIRED`. Transitions are enforced in
`TransactionState.canMove` and `InteractionRepository.transition`; every transition is audited.
Offline before sending → back to `APPROVED` (queued). Network error after sending → `SUBMITTED`
with "outcome unknown": status is checked by idempotency key, never re-sent.

## DI
`di/NetworkModule`, `di/DataModule` (existing), `di/VaultModule` (encrypted DB + DAOs),
`di/NegotiatorModule` (binds `RequestExecutor` → `VendorInteractionManager`).
`TwentyClaudeApp` starts `TransactionTracker` lazily on `Dispatchers.IO`.
