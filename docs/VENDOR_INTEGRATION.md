# Vendor Integration

## Trust model
A vendor is trusted only if `TrustedVendorRegistry` lists it **and** its stored connection points at
the exact registered host (optionally with a certificate pin). Display names are never proof.
Verification runs again immediately before sending.

## Communication methods
| Method | Implementation | Status |
|---|---|---|
| A Authorized REST / FHIR API | `AuthorizedApiConnector` — HTTPS only, no redirects, optional pinning, `Idempotency-Key`, bearer token from Keystore (`VendorCredentialStore`), FHIR R4 bundle via `FhirMapper` | Implemented framework; **no real vendors configured** |
| B OCR | `ocr/*` (ML Kit Text Recognition, on-device) | Implemented |
| C AI-assisted form completion | `FormFieldExtractor` + Gemini Nano gap-fill + `FormValidator` + user review; `FormMapper.toVendorPayload` | Implemented (vault import + vendor payloads) |
| D Document exchange | `DocumentExchange` (CSV/JSON, formula-safe), `FhirMapper` (FHIR JSON); PDF via existing writers | Implemented |
| E Messaging | `MESSAGING` service through a verified connector only | Implemented via mock; real channels planned |
| Manual | `ManualProcessConnector` — guided official process, user enters confirmation | Implemented |
| Mock | `MockVendorConnector` (`mock://` hosts) — deterministic, idempotent sample vendors | Implemented (development/testing) |

## Adding a real vendor (checklist)
1. Written authorization / API agreement; security assessment; HIPAA/GLBA/PCI review as applicable.
2. Add a `TrustedVendor` entry (legal name, domain, exact hosts, services, official channel).
3. Insert `vendors`, `vendor_services`, `vendor_connections` (HTTPS endpoint, auth type,
   credential alias, certificate pin). Store credentials with `VendorCredentialStore`, never in Room.
4. Map the vendor's schema in `FormMapper` / `FhirMapper`; add contract tests with recorded,
   synthetic responses.
5. Verify the confirmation field the vendor returns is mapped to `ConnectorResult.Confirmed`.

## Sample vendors
`Riverside Family Clinic (sample)`, `Northwind Market (sample)`, `Harbor Community Bank (sample)` —
installed by "Load synthetic sample data" in the Personal Vault.
