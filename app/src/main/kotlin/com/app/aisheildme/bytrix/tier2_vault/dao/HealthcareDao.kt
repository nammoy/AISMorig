package com.app.aisheildme.bytrix.tier2_vault.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.app.aisheildme.bytrix.tier2_vault.entities.AppointmentEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.HealthcareOrganizationEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.InsurancePolicyEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.MedicalDocumentEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.MedicationEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.PatientEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.ProviderEntity
import kotlinx.coroutines.flow.Flow

/** Flattened appointment row for lists and agent planning. */
data class AppointmentDetail(
    val id: Long,
    val patientId: Long,
    val startsAt: Long,
    val reason: String?,
    val location: String?,
    val status: String,
    val checkInStatus: String,
    val providerName: String?,
    val specialty: String?,
    val organizationName: String?,
    val organizationVendorId: Long?,
)

@Dao
interface HealthcareDao {
    @Upsert
    suspend fun upsertPatient(patient: PatientEntity): Long

    @Query("SELECT * FROM patients WHERE user_id = :userId")
    suspend fun patientForUser(userId: Long): PatientEntity?

    @Query("SELECT * FROM patients WHERE user_id = :userId")
    fun observePatient(userId: Long): Flow<PatientEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrganization(org: HealthcareOrganizationEntity): Long

    @Query("SELECT * FROM healthcare_organizations WHERE name = :name AND address = :address")
    suspend fun organizationByNameAddress(name: String, address: String): HealthcareOrganizationEntity?

    @Query("SELECT * FROM healthcare_organizations WHERE id = :id")
    suspend fun organization(id: Long): HealthcareOrganizationEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProvider(provider: ProviderEntity): Long

    @Query("SELECT * FROM providers WHERE name = :name AND (organization_id = :organizationId OR (:organizationId IS NULL AND organization_id IS NULL)) LIMIT 1")
    suspend fun providerByName(name: String, organizationId: Long?): ProviderEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAppointment(appointment: AppointmentEntity): Long

    @Query(
        "SELECT a.id, a.patient_id AS patientId, a.starts_at AS startsAt, a.reason, a.location, a.status, " +
            "a.check_in_status AS checkInStatus, p.name AS providerName, p.specialty, " +
            "o.name AS organizationName, o.vendor_id AS organizationVendorId " +
            "FROM appointments a LEFT JOIN providers p ON p.id = a.provider_id " +
            "LEFT JOIN healthcare_organizations o ON o.id = a.organization_id " +
            "WHERE a.patient_id = :patientId ORDER BY a.starts_at",
    )
    fun observeAppointments(patientId: Long): Flow<List<AppointmentDetail>>

    @Query(
        "SELECT a.id, a.patient_id AS patientId, a.starts_at AS startsAt, a.reason, a.location, a.status, " +
            "a.check_in_status AS checkInStatus, p.name AS providerName, p.specialty, " +
            "o.name AS organizationName, o.vendor_id AS organizationVendorId " +
            "FROM appointments a LEFT JOIN providers p ON p.id = a.provider_id " +
            "LEFT JOIN healthcare_organizations o ON o.id = a.organization_id " +
            "WHERE a.patient_id = :patientId AND a.starts_at >= :from AND a.status = 'SCHEDULED' " +
            "ORDER BY a.starts_at LIMIT :limit",
    )
    suspend fun upcomingAppointments(patientId: Long, from: Long, limit: Int): List<AppointmentDetail>

    @Query("UPDATE appointments SET check_in_status = :checkInStatus WHERE id = :appointmentId")
    suspend fun setCheckInStatus(appointmentId: Long, checkInStatus: String)

    @Upsert
    suspend fun upsertMedication(medication: MedicationEntity): Long

    @Query("SELECT * FROM medications WHERE patient_id = :patientId AND active = 1 ORDER BY name")
    fun observeActiveMedications(patientId: Long): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE patient_id = :patientId AND active = 1 ORDER BY name")
    suspend fun activeMedications(patientId: Long): List<MedicationEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInsurance(policy: InsurancePolicyEntity): Long

    @Query("SELECT * FROM insurance_policies WHERE patient_id = :patientId ORDER BY id DESC")
    suspend fun insurance(patientId: Long): List<InsurancePolicyEntity>

    @Query("SELECT * FROM insurance_policies WHERE patient_id = :patientId ORDER BY id DESC")
    fun observeInsurance(patientId: Long): Flow<List<InsurancePolicyEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMedicalDocument(doc: MedicalDocumentEntity): Long

    @Query("SELECT COUNT(*) FROM appointments WHERE patient_id = :patientId")
    suspend fun appointmentCount(patientId: Long): Int
}
