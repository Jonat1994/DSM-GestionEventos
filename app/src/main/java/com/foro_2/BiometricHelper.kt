package com.foro_2

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricHelper(private val activity: FragmentActivity) {
    
    private val executor = ContextCompat.getMainExecutor(activity)
    
    /**
     * Verifica si el dispositivo soporta autenticación biométrica
     */
    fun isBiometricAvailable(): Boolean {
        return try {
            val biometricManager = BiometricManager.from(activity)
            when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
                BiometricManager.BIOMETRIC_SUCCESS -> true
                else -> false
            }
        } catch (e: Exception) {
            android.util.Log.e("BiometricHelper", "Error al verificar disponibilidad biométrica", e)
            false
        }
    }
    
    /**
     * Muestra el prompt de autenticación biométrica
     */
    fun showBiometricPrompt(
        title: String = "Autenticación biométrica",
        subtitle: String = "Usa tu huella dactilar o reconocimiento facial para continuar",
        negativeButtonText: String = "Cancelar",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        try {
            if (!isBiometricAvailable()) {
                onError("La autenticación biométrica no está disponible en este dispositivo")
                return
            }
            
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(negativeButtonText)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()
            
            val biometricPrompt = BiometricPrompt(activity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }
                    
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        when (errorCode) {
                            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                            BiometricPrompt.ERROR_USER_CANCELED -> onCancel()
                            else -> onError(errString.toString())
                        }
                    }
                    
                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onError("Autenticación fallida. Intenta de nuevo.")
                    }
                }
            )
            
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            android.util.Log.e("BiometricHelper", "Error al mostrar prompt biométrico", e)
            onError("Error al mostrar autenticación biométrica: ${e.message}")
        }
    }
}

