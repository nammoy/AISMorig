package com.app.aisheildme.bytrix.tier2_vault.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "patients",
    foreignKeys = [ForeignKey(UserEntity::class, ["id"], ["user_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["user_id"], unique = true)],
)
data class PatientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "first_name") val firstName: String,
    @ColumnInfo(name = "last_name") val lastName: String,
    @ColumnInfo(name = "date_of_birth") val dateOfBirth: String? = null,
    val sex: String? = null,
    @ColumnInfo(name = "blood_type") val bloodType: String? = null,
    val allergies: String? = null,
    val conditions: String? = null,
    /** FHIR Patient.id at the user's provider, when linked. */
    @ColumnInfo(name = "fhir_id") val fhirId: String? = null,
)

@Entity(
    tableName = "healthcare_organizations",
    foreignKeys = [ForeignKey(VendorEntity::class, ["id"], ["vendor_id"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("vendor_id"), Index(value = ["name", "address"], unique = true)],
)
data class HealthcareOrganizationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "vendor_id") val vendorId: Long? = null,
    val name: String,
    val type: String = "CLINIC",
    val phone: String? = null,
    val address: String = "",
    @ColumnInfo(name = "fhir_id") val fhirId: String? = null,
)

@Entity(
    tableName = "providers",
    foreignKeys = [
        ForeignKey(HealthcareOrganizationEntity::class, ["id"], ["organization_id"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("organization_id"), Index(value = ["npi"], unique = true)],
)
data class ProviderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "organization_id") val organizationId: Long? = null,
    val name: String,
    val specialty: String? = null,
    /** US National Provider Identifier; unique when present. */
    val npi: String? = null,
    val phone: String? = null,
    @ColumnInfo(name = "fhir_id") val fhirId: String? = null,
)

@Entity(
    tableName = "appointments",
    foreignKeys = [
        ForeignKey(PatientEntity::class, ["id"], ["patient_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(ProviderEntity::class, ["id"], ["provider_id"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(HealthcareOrganizationEntity::class, ["id"], ["organization_id"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [
        Index("patient_id"), Index("provider_id"), Index("organization_id"), Index("starts_at"),
        Index(value = ["patient_id", "provider_id", "starts_at"], unique = true),
    ],
)
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "patient_id") val patientId: Long,
    @ColumnInfo(name = "provider_id") val providerId: Long? = null,
    @ColumnInfo(name = "organization_id") val organizationId: Long? = null,
    @ColumnInfo(name = "starts_at") val startsAt: Long,
    val reason: String? = null,
    val location: String? = null,
    val status: String = "SCHEDULED",
    @ColumnInfo(name = "check_in_status") val checkInStatus: String = "NOT_STARTED",
    @ColumnInfo(name = "external_ref") val externalRef: String? = null,
)

@Entity(
    tableName = "medications",
    foreignKeys = [
        ForeignKey(PatientEntity::class, ["id"], ["patient_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(ProviderEntity::class, ["id"], ["prescriber_id"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("patient_id"), Index("prescriber_id"), Index(value = ["patient_id", "name", "dosage"], unique = true)],
)
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "patient_id") val patientId: Long,
    @ColumnInfo(name = "prescriber_id") val prescriberId: Long? = null,
    val name: String,
    val dosage: String = "",
    val frequency: String? = null,
    @ColumnInfo(name = "start_date") val startDate: String? = null,
    @ColumnInfo(name = "end_date") val endDate: String? = null,
    val active: Boolean = true,
)

@Entity(
    tableName = "medical_documents",
    foreignKeys = [
        ForeignKey(PatientEntity::class, ["id"], ["patient_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(VendorDocumentEntity::class, ["id"], ["document_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("patient_id"), Index(value = ["document_id"], unique = true)],
)
data class MedicalDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "patient_id") val patientId: Long,
    @ColumnInfo(name = "document_id") val documentId: Long,
    val category: String,
    @ColumnInfo(name = "document_date") val documentDate: String? = null,
)

@Entity(
    tableName = "insurance_policies",
    foreignKeys = [ForeignKey(PatientEntity::class, ["id"], ["patient_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("patient_id"), Index(value = ["patient_id", "payer_name", "member_id"], unique = true)],
)
data class InsurancePolicyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "patient_id") val patientId: Long,
    @ColumnInfo(name = "payer_name") val payerName: String,
    @ColumnInfo(name = "member_id") val memberId: String,
    @ColumnInfo(name = "group_number") val groupNumber: String? = null,
    @ColumnInfo(name = "plan_name") val planName: String? = null,
    @ColumnInfo(name = "effective_date") val effectiveDate: String? = null,
    @ColumnInfo(name = "expiration_date") val expirationDate: String? = null,
)
