# Database Schema

The authoritative description lives in `ai-memory/DATABASE.md`; the exact generated schema is
`app/schemas/com.app.aisheildme.bytrix.tier2_vault.AppDatabase/2.json`.

## Entity relationships (simplified)
```text
users 1─1 user_profiles · users 1─1 user_settings · users 1─1 authentication_metadata
users 1─1 patients ─< appointments >─ providers >─ healthcare_organizations >─ vendors
                   ─< medications · ─< insurance_policies · ─< medical_documents >─ vendor_documents
users ─< purchases >─ merchants >─ vendors
         purchases ─< purchase_items ─1 warranties · purchases ─1 receipts · purchases ─< return_requests
users ─< financial_accounts >─ financial_institutions >─ vendors
         financial_accounts ─< financial_transactions ─< transaction_disputes · ─< bank_statements
vendors ─< vendor_services · ─< vendor_connections · ─< consent_authorizations >─ users
users ─< interaction_requests >─ vendors
         interaction_requests ─1 interaction_transactions · ─< interaction_responses · ─< interaction_audits
vendor_documents ─< document_fields
workflows · access_policies · audit_events · checkin_ai_summaries (legacy v1)
```

## Duplicate prevention
| Data | Unique key |
|---|---|
| Purchase | merchant + order_number |
| Bank transaction | account + fingerprint (bank id, else day + amount + normalized description) |
| Statement | account + period |
| Insurance | patient + payer + member_id |
| Appointment | patient + provider + start time |
| Imported document | SHA-256 content hash |
| Vendor request | idempotency key (user + vendor + purpose + record + payload) |
