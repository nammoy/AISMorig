package com.app.aisheildme.bytrix.data.repository

import com.app.aisheildme.bytrix.core.common.AppException
import com.app.aisheildme.bytrix.data.remote.api.MiscApi
import com.app.aisheildme.bytrix.data.remote.api.PagesApi
import com.app.aisheildme.bytrix.data.remote.dto.UnavailabilityRequest
import com.app.aisheildme.bytrix.data.remote.dto.toDomain
import com.app.aisheildme.bytrix.data.remote.dto.toRequest
import com.app.aisheildme.bytrix.domain.model.Invoice
import com.app.aisheildme.bytrix.domain.model.StaticPage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentRepository @Inject constructor(
    private val pagesApi: PagesApi,
    private val miscApi: MiscApi,
) {
    suspend fun getPages(): List<StaticPage> =
        pagesApi.getPages().data.orEmpty().map { it.toDomain() }

    suspend fun getPage(slug: String): StaticPage =
        pagesApi.getPageBySlug(slug).data?.toDomain()
            ?: throw AppException("Could not load this page.")

    suspend fun createInvoice(invoice: Invoice): String =
        miscApi.createInvoice(invoice.toRequest()).message

    suspend fun setUnavailability(startAt: String, endAt: String): String =
        miscApi.setUnavailability(UnavailabilityRequest(startAt, endAt)).message
}
