package com.foro_2

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import android.widget.Toast

class SplashActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Inicializar Firebase primero para evitar Component activation exception
            try {
                FirebaseApp.initializeApp(this)
            } catch (e: Exception) {
                android.util.Log.w("SplashActivity", "Firebase ya estaba inicializado", e)
            }
            
            // Configurar pantalla completa usando APIs modernas
            setupFullScreen()
            
            setContentView(R.layout.activity_splash)
            
            // Verificar si hay credenciales guardadas y si el dispositivo soporta biométrica
            try {
                val secureStorage = SecureStorageHelper(this)
                // AppCompatActivity ya es un FragmentActivity, no necesita cast
                val biometricHelper = BiometricHelper(this)
                
                if (secureStorage.hasSavedCredentials() && biometricHelper.isBiometricAvailable()) {
                    // Mostrar prompt biométrico después de un breve delay
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (!isFinishing && !isDestroyed) {
                            try {
                                showBiometricAuthentication(secureStorage, biometricHelper)
                            } catch (e: Exception) {
                                android.util.Log.e("SplashActivity", "Error al mostrar autenticación biométrica", e)
                                navigateToNextScreen()
                            }
                        }
                    }, 2000)
                } else {
                    // Navegar normalmente después de mostrar el icono
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (!isFinishing && !isDestroyed) {
                            navigateToNextScreen()
                        }
                    }, 2000) // Mostrar el icono durante 2 segundos
                }
            } catch (e: Exception) {
                android.util.Log.e("SplashActivity", "Error al inicializar autenticación biométrica", e)
                e.printStackTrace()
                // Navegar normalmente si hay error
                Handler(Looper.getMainLooper()).postDelayed({
                    if (!isFinishing && !isDestroyed) {
                        navigateToNextScreen()
                    }
                }, 2000)
            }
        } catch (e: Exception) {
            android.util.Log.e("SplashActivity", "Error crítico en onCreate", e)
            e.printStackTrace()
            // Intentar navegar directamente si hay error
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } catch (e2: Exception) {
                    android.util.Log.e("SplashActivity", "Error crítico al navegar", e2)
                }
            }, 500)
        }
    }
    
    private fun showBiometricAuthentication(secureStorage: SecureStorageHelper, biometricHelper: BiometricHelper) {
        biometricHelper.showBiometricPrompt(
            title = "Autenticación biométrica",
            subtitle = "Usa tu huella dactilar o reconocimiento facial para iniciar sesión",
            negativeButtonText = "Usar contraseña",
            onSuccess = {
                // Autenticación biométrica exitosa, iniciar sesión con credenciales guardadas
                val credentials = secureStorage.getCredentials()
                val email = credentials.first
                val password = credentials.second
                if (email != null && password != null && email.isNotEmpty() && password.isNotEmpty()) {
                    android.util.Log.d("SplashActivity", "Autenticación biométrica exitosa, iniciando sesión...")
                    signInWithSavedCredentials(email, password)
                } else {
                    android.util.Log.e("SplashActivity", "No se encontraron credenciales guardadas")
                    navigateToNextScreen()
                }
            },
            onError = { error ->
                android.util.Log.e("SplashActivity", "Error en autenticación biométrica: $error")
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                navigateToNextScreen()
            },
            onCancel = {
                android.util.Log.d("SplashActivity", "Autenticación biométrica cancelada")
                navigateToNextScreen()
            }
        )
    }
    
    private fun signInWithSavedCredentials(email: String, password: String) {
        try {
            val firebaseAuth = FirebaseAuth.getInstance()
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        android.util.Log.d("SplashActivity", "Inicio de sesión exitoso con credenciales guardadas")
                        val intent = Intent(this, EventsListActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        android.util.Log.e("SplashActivity", "Error al iniciar sesión con credenciales guardadas", task.exception)
                        // Si las credenciales no funcionan, eliminarlas y navegar a login
                        val secureStorage = SecureStorageHelper(this)
                        secureStorage.clearCredentials()
                        navigateToNextScreen()
                    }
                }
        } catch (e: Exception) {
            android.util.Log.e("SplashActivity", "Error al iniciar sesión con credenciales guardadas", e)
            navigateToNextScreen()
        }
    }
    
    private fun navigateToNextScreen() {
        try {
            if (isFinishing || isDestroyed) {
                android.util.Log.w("SplashActivity", "Activity ya está finalizando o destruida, no se puede navegar")
                return
            }
            
            android.util.Log.d("SplashActivity", "Navegando a la siguiente pantalla...")
            
            // Obtener usuario de Firebase (ya inicializado en onCreate)
            val user = try {
                FirebaseAuth.getInstance().currentUser
            } catch (e: Exception) {
                android.util.Log.w("SplashActivity", "Error al obtener usuario de Firebase, navegando a login", e)
                null
            }
            
            android.util.Log.d("SplashActivity", "Usuario autenticado: ${user != null}")
            
            val intent = if (user != null) {
                android.util.Log.d("SplashActivity", "Navegando a EventsListActivity")
                Intent(this, EventsListActivity::class.java)
            } else {
                android.util.Log.d("SplashActivity", "Navegando a LoginActivity")
                Intent(this, LoginActivity::class.java)
            }
            
            startActivity(intent)
            finish()
            android.util.Log.d("SplashActivity", "Navegación completada exitosamente")
        } catch (e: Exception) {
            android.util.Log.e("SplashActivity", "Error al navegar a la siguiente pantalla", e)
            android.util.Log.e("SplashActivity", "Tipo de error: ${e.javaClass.simpleName}")
            android.util.Log.e("SplashActivity", "Mensaje: ${e.message}")
            android.util.Log.e("SplashActivity", "Causa: ${e.cause?.message}")
            e.printStackTrace()
            
            // Intentar navegar a LoginActivity como fallback
            try {
                android.util.Log.d("SplashActivity", "Intentando fallback a LoginActivity")
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            } catch (e2: Exception) {
                android.util.Log.e("SplashActivity", "Error crítico al navegar (fallback también falló)", e2)
                android.util.Log.e("SplashActivity", "Tipo de error: ${e2.javaClass.simpleName}")
                android.util.Log.e("SplashActivity", "Mensaje: ${e2.message}")
                e2.printStackTrace()
            }
        }
    }
    
    private fun setupFullScreen() {
        try {
            // Usar WindowCompat para compatibilidad
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ (API 30+)
                try {
                    window.insetsController?.let { controller ->
                        controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                        controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SplashActivity", "Error al configurar pantalla completa (Android 11+)", e)
                }
            } else {
                // Android 10 y anteriores - usar método antiguo con @Suppress
                try {
                    @Suppress("DEPRECATION")
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN
                    )
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
                } catch (e: Exception) {
                    android.util.Log.w("SplashActivity", "Error al configurar pantalla completa (Android 10-)", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("SplashActivity", "Error general al configurar pantalla completa", e)
            // Continuar sin pantalla completa si hay error
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
    }
}
