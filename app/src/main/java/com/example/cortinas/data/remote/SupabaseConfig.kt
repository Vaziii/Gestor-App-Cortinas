package com.example.cortinas.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SupabaseConfig(context: Context) {
    companion object {
        // La URL y la clave anon son públicas; nunca incluir SUPABASE_SECRET_KEY
        // en una app Android distribuible.
        private const val DEFAULT_SUPABASE_URL = "https://rysctwbypoqhfkygpgsn.supabase.co"
        private const val DEFAULT_SUPABASE_ANON_KEY = "sb_publishable_31-n6rheo3lKYdQ_FKvxQw_WVyhKhnb"
        private const val TOKEN_KEY_ALIAS = "cortinas_supabase_tokens_v1"
        private const val ENCRYPTED_PREFIX = "enc:v1:"
        const val SESSION_VALIDITY_MS = 3L * 24 * 60 * 60 * 1000
    }

    private val prefs: SharedPreferences = context.getSharedPreferences("supabase_config", Context.MODE_PRIVATE)
    private val _sessionInvalidated = MutableStateFlow(false)
    val sessionInvalidated = _sessionInvalidated.asStateFlow()
    private val _authorizationDenied = MutableStateFlow(false)
    val authorizationDenied = _authorizationDenied.asStateFlow()

    fun markAuthorizationDenied() {
        _authorizationDenied.value = true
    }

    fun clearAuthorizationDenied() {
        _authorizationDenied.value = false
    }

    private fun tokenKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(TOKEN_KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    TOKEN_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            generateKey()
        }
    }

    private fun encryptToken(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, tokenKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return ENCRYPTED_PREFIX + Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun readToken(key: String): String {
        val stored = prefs.getString(key, "") ?: return ""
        if (stored.isBlank()) return ""
        return try {
            if (!stored.startsWith(ENCRYPTED_PREFIX)) {
                val migrated = encryptToken(stored)
                prefs.edit().putString(key, migrated).commit()
                stored
            } else {
                val payload = Base64.decode(stored.removePrefix(ENCRYPTED_PREFIX), Base64.NO_WRAP)
                val ivSize = 12
                require(payload.size > ivSize)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, tokenKey(), GCMParameterSpec(128, payload.copyOfRange(0, ivSize)))
                String(cipher.doFinal(payload.copyOfRange(ivSize, payload.size)), Charsets.UTF_8)
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun writeToken(key: String, value: String) {
        if (value.isBlank()) {
            prefs.edit().remove(key).apply()
            return
        }
        runCatching { encryptToken(value) }
            .onSuccess { prefs.edit().putString(key, it).apply() }
            .onFailure { prefs.edit().remove(key).apply() }
    }

    val url: String = DEFAULT_SUPABASE_URL

    val anonKey: String = DEFAULT_SUPABASE_ANON_KEY

    var accessToken: String
        get() = readToken("supabase_access_token")
        set(value) = writeToken("supabase_access_token", value)

    var refreshToken: String
        get() = readToken("supabase_refresh_token")
        set(value) = writeToken("supabase_refresh_token", value)

    var tokenExpiresAt: Long
        get() = prefs.getLong("supabase_token_expires_at", 0L)
        set(value) = prefs.edit().putLong("supabase_token_expires_at", value).apply()

    var keepSession: Boolean
        get() = prefs.getBoolean("keep_session", true)
        set(value) = prefs.edit().putBoolean("keep_session", value).apply()

    var loginExpiresAt: Long
        get() = prefs.getLong("login_expires_at", 0L)
        private set(value) = prefs.edit().putLong("login_expires_at", value).apply()

    val isAuthenticated: Boolean
        get() = accessToken.isNotBlank() && refreshToken.isNotBlank()

    fun saveSession(access: String, refresh: String, expiresInSeconds: Long) {
        accessToken = access
        refreshToken = refresh
        tokenExpiresAt = System.currentTimeMillis() + expiresInSeconds.coerceAtLeast(60) * 1000L
        if (isAuthenticated) {
            _sessionInvalidated.value = false
            _authorizationDenied.value = false
        }
    }

    fun recordLogin(rememberSession: Boolean) {
        keepSession = rememberSession
        loginExpiresAt = System.currentTimeMillis() + SESSION_VALIDITY_MS
    }

    val requiresFreshLogin: Boolean
        get() = !keepSession || loginExpiresAt <= System.currentTimeMillis()

    val isLoginExpired: Boolean
        get() = loginExpiresAt <= System.currentTimeMillis()

    fun clearSession() {
        prefs.edit()
            .remove("supabase_access_token")
            .remove("supabase_refresh_token")
            .remove("supabase_token_expires_at")
            .remove("login_expires_at")
            .apply()
        _authorizationDenied.value = false
        _sessionInvalidated.value = true
    }

    var nombreNegocio: String
        get() = prefs.getString("nombre_negocio", "Mi Taller de Cortinas") ?: "Mi Taller de Cortinas"
        set(value) = prefs.edit().putString("nombre_negocio", value.trim()).apply()

    var telefonoNegocio: String
        get() = prefs.getString("telefono_negocio", "") ?: ""
        set(value) = prefs.edit().putString("telefono_negocio", value.trim()).apply()

}
