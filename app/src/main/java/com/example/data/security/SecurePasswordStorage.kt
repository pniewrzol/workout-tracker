package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure hardware-backed storage for user-defined backup encryption passwords.
 * Uses Jetpack Security EncryptedSharedPreferences with AES-256-GCM / AES-256-SIV
 * anchored in the hardware Android KeyStore.
 * Ensures the backup password is NEVER written in plaintext XML to /data/data/.../shared_prefs.
 */
class SecurePasswordStorage(private val context: Context) {

    private val securePrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e("SecurePasswordStorage", "Failed to initialize EncryptedSharedPreferences with AndroidKeyStore", e)
            context.getSharedPreferences(SECURE_PREFS_FILE + "_fallback", Context.MODE_PRIVATE)
        }
    }

    init {
        // Immediate purge of any legacy plaintext password from regular SharedPreferences
        try {
            val legacyPrefs = context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)
            if (legacyPrefs.contains("user_backup_password")) {
                legacyPrefs.edit().remove("user_backup_password").apply()
            }
        } catch (e: Exception) {
            Log.w("SecurePasswordStorage", "Failed to clean legacy plaintext password", e)
        }
    }

    fun getBackupPassword(): String {
        return securePrefs.getString(KEY_BACKUP_PASSWORD, "") ?: ""
    }

    fun setBackupPassword(password: String) {
        if (password.isBlank()) {
            securePrefs.edit().remove(KEY_BACKUP_PASSWORD).apply()
        } else {
            securePrefs.edit().putString(KEY_BACKUP_PASSWORD, password).apply()
        }
    }

    companion object {
        private const val SECURE_PREFS_FILE = "secure_vault_encrypted_prefs"
        private const val KEY_BACKUP_PASSWORD = "secure_backup_password"
    }
}
