package com.app.aisheildme.bytrix.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.app.aisheildme.bytrix.data.local.db.CheckInHistoryDao
import com.app.aisheildme.bytrix.tier2_vault.AppDatabase
import com.app.aisheildme.bytrix.tier2_vault.dao.AuditDao
import com.app.aisheildme.bytrix.tier2_vault.security.DatabaseEncryption
import com.app.aisheildme.bytrix.tier2_vault.security.DatabaseKeyManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

/** Tier 2 wiring: one encrypted Room database, opened with a Keystore-protected passphrase. */
@Module
@InstallIn(SingletonComponent::class)
object VaultModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keys: DatabaseKeyManager,
    ): AppDatabase {
        DatabaseEncryption.loadNativeLibrary()
        val passphrase = keys.passphrase()
        DatabaseEncryption.encryptInPlaceIfNeeded(context, AppDatabase.NAME, passphrase)
        return Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .openHelperFactory(SupportOpenHelperFactory(passphrase.toByteArray(Charsets.US_ASCII)))
            .addCallback(ForeignKeysOn)
            .build()
    }

    @Provides
    @Singleton
    fun provideCheckInHistoryDao(db: AppDatabase): CheckInHistoryDao = db.checkInHistoryDao()

    @Provides
    fun provideAuditDao(db: AppDatabase): AuditDao = db.auditDao()

    /** Enforce referential integrity on every connection. */
    object ForeignKeysOn : RoomDatabase.Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys = ON")
        }
    }
}
