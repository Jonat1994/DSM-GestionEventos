package com.foro_2

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureStorageHelper(private val context: Context) {
    
    private val keyAlias = "biometric_key"
    private val prefsName = "secure_prefs"
    
    private val masterKey: MasterKey by lazy {
        try {
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        } catch (e: Exception) {
            android.util.Log.e("SecureStorageHelper", "Error al crear MasterKey", e)
            throw e
        }
    }
    
    private val sharedPreferences: SharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                prefsName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            android.util.Log.e("SecureStorageHelper", "Error al crear EncryptedSharedPreferences", e)
            // Fallback a SharedPreferences normal si falla (menos seguro pero evita crash)
            android.util.Log.w("SecureStorageHelper", "Usando SharedPreferences normal como fallback")
            context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        }
    }
    
    /**
     * Guarda las credenciales de forma segura
     */
    fun saveCredentials(email: String, password: String) {
        try {
            sharedPreferences.edit()
                .putString("saved_email", email)
                .putString("saved_password", password)
                .putBoolean("biometric_enabled", true)
                .apply()
        } catch (e: Exception) {
            android.util.Log.e("SecureStorageHelper", "Error al guardar credenciales", e)
        }
    }
    
    /**
     * Obtiene las credenciales guardadas
     */
    fun getCredentials(): Pair<String?, String?> {
        return try {
            val email = sharedPreferences.getString("saved_email", null)
            val password = sharedPreferences.getString("saved_password", null)
            Pair(email, password)
        } catch (e: Exception) {
            android.util.Log.e("SecureStorageHelper", "Error al obtener credenciales", e)
            Pair(null, null)
        }
    }
    
    /**
     * Verifica si hay credenciales guardadas
     */
    fun hasSavedCredentials(): Boolean {
        return try {
            val email = sharedPreferences.getString("saved_email", null)
            val password = sharedPreferences.getString("saved_password", null)
            val biometricEnabled = sharedPreferences.getBoolean("biometric_enabled", false)
            !email.isNullOrEmpty() && !password.isNullOrEmpty() && biometricEnabled
        } catch (e: Exception) {
            android.util.Log.e("SecureStorageHelper", "Error al verificar credenciales", e)
            false
        }
    }
    
    /**
     * Elimina las credenciales guardadas
     */
    fun clearCredentials() {
        try {
            sharedPreferences.edit()
                .remove("saved_email")
                .remove("saved_password")
                .putBoolean("biometric_enabled", false)
                .apply()
        } catch (e: Exception) {
            android.util.Log.e("SecureStorageHelper", "Error al eliminar credenciales", e)
        }
    }
    
    /**
     * Verifica si la autenticación biométrica está habilitada
     */
    fun isBiometricEnabled(): Boolean {
        return try {
            sharedPreferences.getBoolean("biometric_enabled", false)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Habilita o deshabilita la autenticación biométrica
     */
    fun setBiometricEnabled(enabled: Boolean) {
        try {
            sharedPreferences.edit()
                .putBoolean("biometric_enabled", enabled)
                .apply()
        } catch (e: Exception) {
            android.util.Log.e("SecureStorageHelper", "Error al cambiar estado biométrico", e)
        }
    }
}

