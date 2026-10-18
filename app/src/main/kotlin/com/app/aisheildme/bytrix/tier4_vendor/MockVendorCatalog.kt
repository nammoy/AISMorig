package com.app.aisheildme.bytrix.tier4_vendor

import com.app.aisheildme.bytrix.tier2_vault.entities.VendorConnectionEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.VendorServiceEntity
import com.app.aisheildme.bytrix.tier2_vault.model.Channel
import com.app.aisheildme.bytrix.tier2_vault.repository.VendorRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Installs the sample vendors (idempotent). Their endpoints use the in-app `mock://` connector. */
@Singleton
class MockVendorCatalog @Inject constructor(private val vendors: VendorRepository) {

    data class Installed(val clinicId: Long, val martId: Long, val bankId: Long)

    suspend fun install(): Installed {
        val ids = TrustedVendorRegistry.entries.associate { trusted ->
            val id = vendors.ensureVendor(trusted.legalName, trusted.domain)
            trusted.services.forEach { vendors.addService(VendorServiceEntity(vendorId = id, serviceType = it.name, channel = Channel.REST_API.name)) }
            vendors.addConnection(
                VendorConnectionEntity(
                    vendorId = id,
                    endpointUrl = "mock://${trusted.hosts.first()}/api",
                    authType = "NONE",
                ),
            )
            trusted.registryId to id
        }
        return Installed(ids.getValue("mock-clinic"), ids.getValue("mock-mart"), ids.getValue("mock-bank"))
    }
}
