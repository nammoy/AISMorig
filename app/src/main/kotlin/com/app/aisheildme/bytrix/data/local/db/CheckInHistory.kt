package com.app.aisheildme.bytrix.data.local.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.app.aisheildme.bytrix.domain.model.SavedCheckInSummary
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "checkin_ai_summaries")
data class CheckInSummaryEntity(
    @PrimaryKey val id: String,
    val created_at: Long,
    val summary_text: String,
    val patient_name: String?,
    val clinic_name: String?,
    val appointment_type: String?,
    val reason_for_visit: String?,
    val device_label: String?,
)

fun CheckInSummaryEntity.toDomain() = SavedCheckInSummary(
    id = id,
    createdAtEpochMs = created_at,
    summaryText = summary_text,
    patientName = patient_name.orEmpty(),
    clinicName = clinic_name.orEmpty(),
    appointmentType = appointment_type.orEmpty(),
    reasonForVisit = reason_for_visit.orEmpty(),
    deviceLabel = device_label,
)

fun SavedCheckInSummary.toEntity() = CheckInSummaryEntity(
    id = id,
    created_at = createdAtEpochMs,
    summary_text = summaryText,
    patient_name = patientName,
    clinic_name = clinicName,
    appointment_type = appointmentType,
    reason_for_visit = reasonForVisit,
    device_label = deviceLabel,
)

@Dao
interface CheckInHistoryDao {

    @Query("SELECT * FROM checkin_ai_summaries ORDER BY created_at DESC")
    fun observeAll(): Flow<List<CheckInSummaryEntity>>

    @Query("SELECT * FROM checkin_ai_summaries ORDER BY created_at DESC")
    suspend fun all(): List<CheckInSummaryEntity>

    @Query("SELECT COUNT(*) FROM checkin_ai_summaries")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: CheckInSummaryEntity)

    @Query("DELETE FROM checkin_ai_summaries WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM checkin_ai_summaries")
    suspend fun clear()
}
