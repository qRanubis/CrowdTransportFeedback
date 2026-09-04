package com.example.crowdtransportfeedback.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

interface TokenStore { fun read(): StoredSession?; fun save(session: StoredSession); fun clear() }

class SecureTokenStore(context: Context) : TokenStore {
    private val preferences = EncryptedSharedPreferences.create(
        context.applicationContext,
        "secure_auth_session",
        MasterKey.Builder(context.applicationContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    override fun read(): StoredSession? {
        val access = preferences.getString("access", null) ?: return null
        val refresh = preferences.getString("refresh", null) ?: return null
        val id = preferences.getString("user_id", null) ?: return null
        val email = preferences.getString("email", null) ?: return null
        val role = preferences.getString("role", null)?.let { runCatching { UserRole.valueOf(it) }.getOrNull() } ?: return null
        return StoredSession(access, refresh, AuthUser(id, email, role))
    }
    override fun save(session: StoredSession) { preferences.edit().putString("access", session.accessToken).putString("refresh", session.refreshToken).putString("user_id", session.user.id).putString("email", session.user.email).putString("role", session.user.role.name).apply() }
    override fun clear() { preferences.edit().clear().apply() }
}
