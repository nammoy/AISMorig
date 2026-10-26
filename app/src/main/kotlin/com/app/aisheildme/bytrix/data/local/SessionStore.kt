package com.app.aisheildme.bytrix.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.app.aisheildme.bytrix.domain.model.User
import com.app.aisheildme.bytrix.tier2_vault.security.KeystoreCipher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "twentyclaude_session")

@Singleton
class SessionStore @Inject constructor(
    private val context: Context,
    private val json: Json,
) {
    private object Keys {
        val USER = stringPreferencesKey("user")
        val API_TOKEN = stringPreferencesKey("api_token")
        val REMEMBERED_EMAIL = stringPreferencesKey("remembered_email")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    }

    private val preferences: Flow<Preferences> = context.dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }

    /**
     * The cached profile is stored without its token; the token is re-attached from the
     * Keystore-sealed entry so callers see the same [User] shape as before.
     */
    val user: Flow<User?> = preferences.map { prefs ->
        prefs[Keys.USER]?.let { raw ->
            runCatching { json.decodeFromString<User>(raw) }.getOrNull()
        }?.let { cached ->
            val token = unseal(prefs[Keys.API_TOKEN]) ?: cached.apiToken
            cached.copy(apiToken = token)
        }
    }

    suspend fun currentUser(): User? = user.first()

    /** Keystore-sealed at rest. A legacy plaintext token is returned once and re-sealed. */
    private val tokenCipher by lazy { KeystoreCipher(TOKEN_KEY_ALIAS) }

    suspend fun apiToken(): String? {
        // One-time upgrade: older builds kept the token inside the cached profile JSON.
        preferences.first()[Keys.USER]
            ?.let { raw -> runCatching { json.decodeFromString<User>(raw) }.getOrNull() }
            ?.takeIf { it.apiToken.isNotEmpty() }
            ?.let { setUser(it) }
        val stored = preferences.first()[Keys.API_TOKEN]?.takeIf { it.isNotEmpty() } ?: return null
        if (!KeystoreCipher.isSealed(stored)) {
            setApiToken(stored)
            return stored
        }
        return unseal(stored)
    }

    private fun unseal(stored: String?): String? {
        if (stored.isNullOrEmpty()) return null
        if (!KeystoreCipher.isSealed(stored)) return stored
        return runCatching { String(tokenCipher.decrypt(stored), Charsets.UTF_8) }.getOrNull()
    }

    suspend fun setUser(user: User) {
        if (user.apiToken.isNotEmpty()) setApiToken(user.apiToken)
        context.dataStore.edit { it[Keys.USER] = json.encodeToString(user.copy(apiToken = "")) }
    }

    suspend fun setApiToken(token: String) {
        val sealed = tokenCipher.encrypt(token.toByteArray(Charsets.UTF_8))
        context.dataStore.edit { it[Keys.API_TOKEN] = sealed }
    }

    suspend fun rememberedEmail(): String? =
        preferences.first()[Keys.REMEMBERED_EMAIL]?.takeIf { it.isNotEmpty() }

    suspend fun setRememberedEmail(email: String?) {
        context.dataStore.edit { prefs ->
            if (email.isNullOrEmpty()) prefs.remove(Keys.REMEMBERED_EMAIL)
            else prefs[Keys.REMEMBERED_EMAIL] = email
        }
    }

    suspend fun isBiometricEnabled(): Boolean =
        preferences.first()[Keys.BIOMETRIC_ENABLED] ?: false

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.USER)
            prefs.remove(Keys.API_TOKEN)
        }
    }

    private companion object {
        const val TOKEN_KEY_ALIAS = "twentyclaude.session.token"
    }
}
