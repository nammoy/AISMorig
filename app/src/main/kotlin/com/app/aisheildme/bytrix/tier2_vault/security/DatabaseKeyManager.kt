package com.app.aisheildme.bytrix.tier2_vault.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the SQLCipher passphrase: 32 random bytes, hex-encoded, sealed with a Keystore key and
 * kept in app-private preferences (backups are disabled in the manifest). The passphrase never
 * appears in the database, logs, or source.
 */
@Singleton
class DatabaseKeyManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val cipher = KeystoreCipher(KEY_ALIAS)
    private val prefs get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun passphrase(): String {
        prefs.getString(PREF_SEALED, null)?.let { sealed ->
            return String(cipher.decrypt(sealed), Charsets.US_ASCII)
        }
        val raw = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val hex = raw.joinToString("") { "%02x".format(it) }
        raw.fill(0)
        check(prefs.edit().putString(PREF_SEALED, cipher.encrypt(hex.toByteArray(Charsets.US_ASCII))).commit()) {
            "Could not persist the vault key"
        }
        return hex
    }

    private companion object {
        const val PREFS = "twentyclaude_vault_keys"
        const val PREF_SEALED = "db_passphrase_sealed"
        const val KEY_ALIAS = "twentyclaude.vault.db"
    }
}
