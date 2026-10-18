package com.app.aisheildme.bytrix.data.repository

import com.app.aisheildme.bytrix.data.remote.api.RecordApi
import com.app.aisheildme.bytrix.data.remote.dto.toDomain
import com.app.aisheildme.bytrix.domain.model.BankStatement
import com.app.aisheildme.bytrix.domain.model.HealthAppointment
import com.app.aisheildme.bytrix.domain.model.RetailOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordRepository @Inject constructor(
    private val api: RecordApi,
) {
    suspend fun getHealth(page: Int = 1, limit: Int = 20): List<HealthAppointment> =
        api.getHealth(page, limit).data.orEmpty().map { it.toDomain() }

    suspend fun getRetail(page: Int = 1, limit: Int = 20): List<RetailOrder> =
        api.getRetail(page, limit).data.orEmpty().map { it.toDomain() }

    suspend fun getBanking(page: Int = 1, limit: Int = 20): List<BankStatement> =
        api.getBanking(page, limit).data.orEmpty().map { it.toDomain() }
}
