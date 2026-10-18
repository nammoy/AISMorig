package com.app.aisheildme.bytrix.vault

import android.app.Application
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.app.aisheildme.bytrix.tier2_vault.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Upgrading a v1 (check-in history only) database must keep every existing row. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class VaultMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun `migrate 1 to 2 keeps check-in history and adds the vault tables`() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO checkin_ai_summaries (id, created_at, summary_text, patient_name) " +
                    "VALUES ('s1', 1700000000000, 'Synthetic summary', 'Test Patient')",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 2, true)

        migrated.query("SELECT summary_text FROM checkin_ai_summaries WHERE id = 's1'").use {
            it.moveToFirst()
            assertEquals("Synthetic summary", it.getString(0))
        }
        migrated.query("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name IN ('users','appointments','purchases','financial_transactions','interaction_requests','consent_authorizations')").use {
            it.moveToFirst()
            assertEquals(6, it.getInt(0))
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
