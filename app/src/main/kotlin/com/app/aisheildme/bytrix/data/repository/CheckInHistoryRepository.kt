package com.app.aisheildme.bytrix.data.repository

import com.app.aisheildme.bytrix.data.local.db.CheckInHistoryDao
import com.app.aisheildme.bytrix.data.local.db.toDomain
import com.app.aisheildme.bytrix.data.local.db.toEntity
import com.app.aisheildme.bytrix.domain.model.SavedCheckInSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckInHistoryRepository @Inject constructor(
    private val dao: CheckInHistoryDao,
) {
    val summaries: Flow<List<SavedCheckInSummary>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun all(): List<SavedCheckInSummary> = dao.all().map { it.toDomain() }

    suspend fun count(): Int = dao.count()

    suspend fun save(summary: SavedCheckInSummary) = dao.save(summary.toEntity())

    suspend fun delete(id: String) = dao.delete(id)

    suspend fun clear() = dao.clear()
}
