package com.app.aisheildme.bytrix.di

import com.app.aisheildme.bytrix.tier4_vendor.VendorInteractionManager
import com.app.aisheildme.bytrix.tier4_vendor.transactions.RequestExecutor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class NegotiatorModule {
    @Binds
    abstract fun bindRequestExecutor(impl: VendorInteractionManager): RequestExecutor
}
