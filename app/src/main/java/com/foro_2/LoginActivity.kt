// kotlin
package com.foro_2

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.foro_2.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import android.util.Log
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var callbackManager: CallbackManager
    
    // Activity Result Launcher para Google Sign-In
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("LoginActivity", "Result code: ${result.resultCode}")
        Log.d("LoginActivity", "Result data: ${result.data != null}")
        
        // Siempre intentar procesar el intent, incluso si el resultado es CANCELED
        // porque los errores de configuración pueden aparecer como CANCELED
        if (result.data == null) {
            // Solo mostrar cancelado si realmente no hay datos Y el código es CANCELED
            if (result.resultCode == android.app.Activity.RESULT_CANCELED) {
                Toast.makeText(this, "Inicio de sesión cancelado", Toast.LENGTH_SHORT).show()
                Log.d("LoginActivity", "Usuario canceló el inicio de sesión (sin datos)")
            } else {
                Toast.makeText(this, "Error: No se recibieron datos de Google", Toast.LENGTH_SHORT).show()
                Log.e("LoginActivity", "result.data es null pero result code es ${result.resultCode}")
            }
            return@registerForActivityResult
        }
        
        // Intentar procesar el resultado incluso si el código es CANCELED
        // porque puede contener información de error útil
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        
        // Verificar si la tarea fue exitosa antes de intentar obtener el resultado
        if (task.isSuccessful) {
            try {
                val account = task.result
                val idToken = account?.idToken

                if (idToken != null) {
                    Log.d("LoginActivity", "ID Token recibido correctamente")
                    firebaseAuthWithGoogle(idToken)
                } else {
                    Toast.makeText(this, "Error: token de Google es nulo", Toast.LENGTH_SHORT).show()
                    Log.e("LoginActivity", "idToken nulo en Google Sign-In. Account: ${account?.email}")
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error al procesar cuenta de Google: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("LoginActivity", "Error al procesar cuenta", e)
            }
        } else {
            // La tarea falló, obtener el error específico
            try {
                val exception = task.exception as? ApiException
                if (exception != null) {
                    val errorMessage = when (exception.statusCode) {
                        12501 -> "Inicio de sesión cancelado por el usuario"
                        10 -> {
                            val detailedMsg = "Error de configuración (10). Verifica:\n1. SHA-1 fingerprint agregado en Firebase\n2. Web Client ID correcto en strings.xml\n3. Google Sign-In habilitado en Firebase"
                            Log.e("LoginActivity", detailedMsg, exception)
                            detailedMsg
                        }
                        7 -> "Error de conexión. Verifica tu conexión a internet"
                        8 -> "Error interno de Google. Intenta de nuevo"
                        12500 -> "Error al iniciar sesión. Intenta de nuevo"
                        else -> {
                            val msg = "Google Sign-In falló (código ${exception.statusCode}): ${exception.message}"
                            Log.e("LoginActivity", msg, exception)
                            msg
                        }
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    Log.e("LoginActivity", "Google Sign-In error completo", exception)
                    Log.e("LoginActivity", "Status code: ${exception.statusCode}, Message: ${exception.message}")
                } else {
                    val errorMsg = "Error desconocido: ${task.exception?.message ?: "Sin detalles"}"
                    Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
                    Log.e("LoginActivity", "Error en Google Sign-In (no ApiException)", task.exception)
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error inesperado: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("LoginActivity", "Error inesperado al procesar error de Google Sign-In", e)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Configurar barras del sistema después de setContentView
        SystemUIHelper.setupSystemBars(this)

        // Inicializa Firebase explícitamente
        FirebaseApp.initializeApp(this)
        firebaseAuth = FirebaseAuth.getInstance()
        
        // Inicializar Facebook CallbackManager
        callbackManager = CallbackManager.Factory.create()

        binding.notUserYet.setOnClickListener{
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Configuración para el inicio de sesión con Google
        try {
            val webClientId = getString(R.string.default_web_client_id).trim()
            Log.d("LoginActivity", "Web Client ID configurado: ${webClientId.take(20)}...")
            
            if (webClientId.isNotEmpty() && webClientId.contains(".apps.googleusercontent.com")) {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build()
                googleSignInClient = GoogleSignIn.getClient(this, gso)
                Log.d("LoginActivity", "Google Sign-In configurado correctamente")
            } else {
                Log.e("LoginActivity", "default_web_client_id inválido o vacío en strings.xml")
                Log.e("LoginActivity", "Asegúrate de usar el Web Client ID de Firebase Console")
                // No mostrar toast aquí para no molestar al usuario si no usa Google Sign-In
            }
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error al configurar Google Sign-In", e)
            // No mostrar toast aquí para no molestar al usuario si no usa Google Sign-In
        }

        // Referencias a los elementos del layout usando binding
        val emailEditText = binding.emailEditText
        val passwordEditText = binding.passwordEditText

        // Configurar switch de autenticación biométrica
        setupBiometricSwitch()

        // Acción del botón de login tradicional
        binding.loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (!isValidEmail(email)) {
                Toast.makeText(this, "Correo inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                Toast.makeText(this, "La contraseña no puede estar vacía", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validación aprobada, acceso permitido
            firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("LoginActivity", "Inicio de sesión exitoso")
                    
                    // Guardar credenciales de forma segura para autenticación biométrica
                    val secureStorage = SecureStorageHelper(this)
                    secureStorage.saveCredentials(email, password)
                    Log.d("LoginActivity", "Credenciales guardadas para autenticación biométrica")
                    
                    // Obtener y guardar token FCM
                    initializeFCMToken()
                    
                    val intent = Intent(this, EventsListActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Log.e("LoginActivity", "Error en login", task.exception)
                    Toast.makeText(this, task.exception?.localizedMessage ?: "Error desconocido", Toast.LENGTH_SHORT).show()
                }
            }

        }

        // Acción del botón de Google Sign-In (ImageButton)
        binding.googleSignInButton.setOnClickListener {
            if (::googleSignInClient.isInitialized) {
                Log.d("LoginActivity", "Iniciando Google Sign-In...")
                try {
                    // Intentar obtener el intent directamente sin hacer signOut primero
                    // Esto evita problemas si signOut falla
                    val signInIntent = googleSignInClient.signInIntent
                    if (signInIntent != null) {
                        Log.d("LoginActivity", "Lanzando intent de Google Sign-In")
                        googleSignInLauncher.launch(signInIntent)
                    } else {
                        Toast.makeText(this, "Error: No se pudo crear el intent de Google Sign-In", Toast.LENGTH_SHORT).show()
                        Log.e("LoginActivity", "signInIntent es null")
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error al iniciar Google Sign-In: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("LoginActivity", "Error al lanzar Google Sign-In", e)
                }
            } else {
                val errorMsg = "Google Sign-In no está configurado. Verifica:\n1. default_web_client_id en strings.xml\n2. Web Client ID de Firebase Console"
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                Log.e("LoginActivity", errorMsg)
            }
        }

        // Acción del botón de Facebook Sign-In
        binding.facebookSignInButton.setOnClickListener {
            Log.d("LoginActivity", "Iniciando Facebook Sign-In...")
            try {
                val facebookAppId = getString(R.string.facebook_app_id)
                if (facebookAppId == "TU_FACEBOOK_APP_ID_AQUI" || facebookAppId.isEmpty()) {
                    Toast.makeText(
                        this,
                        "Facebook App ID no configurado. Configura tu App ID en strings.xml",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("LoginActivity", "Facebook App ID no configurado en strings.xml")
                    return@setOnClickListener
                }
                
                // Configurar permisos de Facebook Login
                LoginManager.getInstance().logInWithReadPermissions(
                    this,
                    listOf("email", "public_profile")
                )
                
                // Configurar callback de Facebook
                LoginManager.getInstance().registerCallback(
                    callbackManager,
                    object : FacebookCallback<LoginResult> {
                        override fun onSuccess(result: LoginResult) {
                            Log.d("LoginActivity", "Facebook login exitoso")
                            val token = result.accessToken
                            if (token != null) {
                                firebaseAuthWithFacebook(token.token)
                            } else {
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Error: Token de Facebook es nulo",
                                    Toast.LENGTH_SHORT
                                ).show()
                                Log.e("LoginActivity", "Facebook token es nulo")
                            }
                        }
                        
                        override fun onCancel() {
                            Log.d("LoginActivity", "Facebook login cancelado por el usuario")
                            Toast.makeText(
                                this@LoginActivity,
                                "Inicio de sesión con Facebook cancelado",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        
                        override fun onError(error: FacebookException) {
                            Log.e("LoginActivity", "Error en Facebook login", error)
                            Toast.makeText(
                                this@LoginActivity,
                                "Error en Facebook login: ${error.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "Error al iniciar Facebook Sign-In: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("LoginActivity", "Error al lanzar Facebook Sign-In", e)
            }
        }

        // Toggle para mostrar/ocultar contraseña
        var isPasswordVisible = false
        binding.btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            
            if (isPasswordVisible) {
                // Mostrar contraseña
                binding.passwordEditText.inputType = android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.btnTogglePassword.setImageResource(R.drawable.ic_visibility)
            } else {
                // Ocultar contraseña
                binding.passwordEditText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.btnTogglePassword.setImageResource(R.drawable.ic_visibility_off)
            }
            
            // Mover el cursor al final del texto
            binding.passwordEditText.setSelection(binding.passwordEditText.text.length)
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun setupBiometricSwitch() {
        val secureStorage = SecureStorageHelper(this)
        val biometricHelper = BiometricHelper(this)
        
        // Verificar si hay credenciales guardadas y si el dispositivo soporta biométrica
        val hasCredentials = secureStorage.hasSavedCredentials()
        val isBiometricAvailable = try {
            biometricHelper.isBiometricAvailable()
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error al verificar disponibilidad biométrica", e)
            false
        }
        
        // Habilitar switch solo si hay credenciales y biométrica disponible
        binding.switchBiometricLogin.isEnabled = hasCredentials && isBiometricAvailable
        
        if (!hasCredentials || !isBiometricAvailable) {
            binding.switchBiometricLogin.alpha = 0.5f
        }
        
        // Listener del switch
        binding.switchBiometricLogin.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Activar modo huella: ocultar contraseña y mostrar prompt biométrico
                binding.passwordContainer.visibility = android.view.View.GONE
                binding.forgotPassword.visibility = android.view.View.GONE
                
                // Verificar que hay credenciales guardadas (verificar de nuevo por si acaban de guardarse)
                val currentHasCredentials = secureStorage.hasSavedCredentials()
                if (currentHasCredentials) {
                    // Mostrar prompt biométrico
                    showBiometricPromptForLogin(secureStorage, biometricHelper)
                } else {
                    Toast.makeText(this, "No hay credenciales guardadas. Inicia sesión con contraseña primero.", Toast.LENGTH_LONG).show()
                    binding.switchBiometricLogin.isChecked = false
                    binding.passwordContainer.visibility = android.view.View.VISIBLE
                    binding.forgotPassword.visibility = android.view.View.VISIBLE
                }
            } else {
                // Desactivar modo huella: mostrar contraseña
                binding.passwordContainer.visibility = android.view.View.VISIBLE
                binding.forgotPassword.visibility = android.view.View.VISIBLE
            }
        }
    }
    
    private fun showBiometricPromptForLogin(secureStorage: SecureStorageHelper, biometricHelper: BiometricHelper) {
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
                    Log.d("LoginActivity", "Autenticación biométrica exitosa, iniciando sesión...")
                    signInWithSavedCredentials(email, password)
                } else {
                    Log.e("LoginActivity", "No se encontraron credenciales guardadas")
                    Toast.makeText(this, "Error: No se encontraron credenciales guardadas", Toast.LENGTH_SHORT).show()
                    binding.switchBiometricLogin.isChecked = false
                    binding.passwordContainer.visibility = android.view.View.VISIBLE
                    binding.forgotPassword.visibility = android.view.View.VISIBLE
                }
            },
            onError = { error ->
                Log.e("LoginActivity", "Error en autenticación biométrica: $error")
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                binding.switchBiometricLogin.isChecked = false
                binding.passwordContainer.visibility = android.view.View.VISIBLE
                binding.forgotPassword.visibility = android.view.View.VISIBLE
            },
            onCancel = {
                Log.d("LoginActivity", "Autenticación biométrica cancelada")
                binding.switchBiometricLogin.isChecked = false
                binding.passwordContainer.visibility = android.view.View.VISIBLE
                binding.forgotPassword.visibility = android.view.View.VISIBLE
            }
        )
    }
    
    private fun signInWithSavedCredentials(email: String, password: String) {
        try {
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("LoginActivity", "Inicio de sesión exitoso con credenciales guardadas")
                        val intent = Intent(this, EventsListActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Log.e("LoginActivity", "Error al iniciar sesión con credenciales guardadas", task.exception)
                        // Si las credenciales no funcionan, eliminarlas y mostrar contraseña
                        val secureStorage = SecureStorageHelper(this)
                        secureStorage.clearCredentials()
                        Toast.makeText(this, "Las credenciales guardadas ya no son válidas. Usa tu contraseña.", Toast.LENGTH_LONG).show()
                        binding.switchBiometricLogin.isChecked = false
                        binding.passwordContainer.visibility = android.view.View.VISIBLE
                        binding.forgotPassword.visibility = android.view.View.VISIBLE
                    }
                }
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error al iniciar sesión con credenciales guardadas", e)
            Toast.makeText(this, "Error al iniciar sesión: ${e.message}", Toast.LENGTH_SHORT).show()
            binding.switchBiometricLogin.isChecked = false
            binding.passwordContainer.visibility = android.view.View.VISIBLE
            binding.forgotPassword.visibility = android.view.View.VISIBLE
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        // Crear o actualizar documento de usuario en Firestore
                        FirestoreUtil.createUserDocument(
                            userId = user.uid,
                            email = user.email ?: "",
                            role = "usuario", // Por defecto es usuario
                            onSuccess = {
                                Log.d("LoginActivity", "Usuario creado/actualizado en Firestore")
                                goToHome()
                            },
                            onFailure = { e ->
                                Log.e("LoginActivity", "Error al crear documento de usuario", e)
                                // Continuar de todas formas, el usuario ya está autenticado
                                goToHome()
                            }
                        )
                    } else {
                        goToHome()
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Error desconocido"
                    Toast.makeText(this, "Error con Firebase: $errorMessage", Toast.LENGTH_SHORT).show()
                    Log.e("LoginActivity", "Firebase signInWithCredential failed", task.exception)
                }
            }
    }

    private fun firebaseAuthWithFacebook(token: String) {
        val credential = FacebookAuthProvider.getCredential(token)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        // Crear o actualizar documento de usuario en Firestore
                        FirestoreUtil.createUserDocument(
                            userId = user.uid,
                            email = user.email ?: "",
                            role = "usuario", // Por defecto es usuario
                            onSuccess = {
                                Log.d("LoginActivity", "Usuario creado/actualizado en Firestore")
                                goToHome()
                            },
                            onFailure = { e ->
                                Log.e("LoginActivity", "Error al crear documento de usuario", e)
                                // Continuar de todas formas, el usuario ya está autenticado
                                goToHome()
                            }
                        )
                    } else {
                        goToHome()
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Error desconocido"
                    Toast.makeText(this, "Error con Firebase: $errorMessage", Toast.LENGTH_SHORT).show()
                    Log.e("LoginActivity", "Firebase signInWithCredential failed", task.exception)
                }
            }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Pasar el resultado al CallbackManager de Facebook
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun goToHome() {
        // Obtener y guardar token FCM
        initializeFCMToken()
        
        val intent = Intent(this, EventsListActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    private fun initializeFCMToken() {
        val user = firebaseAuth.currentUser
        if (user != null) {
            Log.d("LoginActivity", "Inicializando token FCM para usuario: ${user.uid}")
            
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e("LoginActivity", "Error al obtener token FCM", task.exception)
                    task.exception?.printStackTrace()
                    return@addOnCompleteListener
                }
                
                val token = task.result
                Log.d("LoginActivity", "Token FCM obtenido exitosamente")
                Log.d("LoginActivity", "Token: ${token.take(50)}...")
                
                // Guardar token en Firestore usando merge para no sobrescribir otros campos
                val userRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.uid)
                
                userRef.get().addOnSuccessListener { document ->
                    val currentToken = document.getString("fcmToken")
                    if (currentToken != token) {
                        // Solo actualizar si el token cambió
                        userRef.update("fcmToken", token)
                            .addOnSuccessListener {
                                Log.d("LoginActivity", "Token FCM guardado/actualizado en Firestore")
                            }
                            .addOnFailureListener { e ->
                                Log.e("LoginActivity", "Error al guardar token FCM en Firestore", e)
                                e.printStackTrace()
                                // Intentar con merge como fallback
                                userRef.set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
                                    .addOnSuccessListener {
                                        Log.d("LoginActivity", "Token FCM guardado usando merge (fallback)")
                                    }
                            }
                    } else {
                        Log.d("LoginActivity", "Token FCM ya está actualizado")
                    }
                }.addOnFailureListener { e ->
                    Log.e("LoginActivity", "Error al leer documento de usuario, usando merge", e)
                    // Si falla la lectura, usar merge directamente
                    userRef.set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
                        .addOnSuccessListener {
                            Log.d("LoginActivity", "Token FCM guardado usando merge")
                        }
                        .addOnFailureListener { e2 ->
                            Log.e("LoginActivity", "Error al guardar token FCM con merge", e2)
                            e2.printStackTrace()
                        }
                }
            }
        } else {
            Log.w("LoginActivity", "Usuario no autenticado, no se puede obtener token FCM")
        }
    }
}
