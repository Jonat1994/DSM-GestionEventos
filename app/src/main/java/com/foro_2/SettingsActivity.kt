package com.foro_2

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.foro_2.databinding.ActivitySettingsBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import android.content.ContentResolver

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var storage: FirebaseStorage
    private var selectedImageUri: Uri? = null
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showImageSourceDialog()
        } else {
            Toast.makeText(this, "Permiso denegado. No se puede acceder a la galería.", Toast.LENGTH_SHORT).show()
        }
    }
    
    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            showImageSourceDialog()
        } else {
            Toast.makeText(this, "Permiso denegado. No se puede acceder a la galería.", Toast.LENGTH_SHORT).show()
        }
    }
    
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                uploadProfilePhoto(uri)
            }
        }
    }
    
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && selectedImageUri != null) {
            uploadProfilePhoto(selectedImageUri!!)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        SystemUIHelper.setupSystemBars(this)
        
        firebaseAuth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
        sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE)
        
        setupToolbar()
        setupGoogleSignInClient()
        setupClickListeners()
        loadSettings()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(android.R.drawable.ic_menu_revert)
        supportActionBar?.title = "Ajustes"
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    private fun setupGoogleSignInClient() {
        try {
            val webClientId = getString(R.string.default_web_client_id).trim()
            if (webClientId.isNotEmpty() && webClientId.contains(".apps.googleusercontent.com")) {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build()
                googleSignInClient = GoogleSignIn.getClient(this, gso)
            }
        } catch (e: Exception) {
            Log.e("SettingsActivity", "Error al configurar Google Sign-In", e)
        }
    }
    
    private fun setupClickListeners() {
        // Cuenta y Perfil
        binding.cardUploadPhoto.setOnClickListener {
            checkPermissionAndSelectImage()
        }
        
        binding.cardEditProfile.setOnClickListener {
            showEditProfileDialog()
        }
        
        binding.cardChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }
        
        binding.cardLinkedAccounts.setOnClickListener {
            showLinkedAccountsDialog()
        }
        
        binding.cardDeleteAccount.setOnClickListener {
            showDeleteAccountConfirmation()
        }
        
        // Notificaciones
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            saveNotificationPreference(isChecked)
        }
        
        binding.switchEventReminders.setOnCheckedChangeListener { _, isChecked ->
            saveEventReminderPreference(isChecked)
        }
        
        binding.switchCommentNotifications.setOnCheckedChangeListener { _, isChecked ->
            saveCommentNotificationPreference(isChecked)
        }
        
        // Privacidad - Autenticación biométrica
        binding.switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            handleBiometricToggle(isChecked)
        }
        
        binding.cardPrivacyPolicy.setOnClickListener {
            openPrivacyPolicy()
        }
        
        binding.cardTermsOfService.setOnClickListener {
            openTermsOfService()
        }
        
        // Información
        binding.cardAbout.setOnClickListener {
            showAboutDialog()
        }
    }
    
    private fun loadSettings() {
        val user = firebaseAuth.currentUser
        if (user != null) {
            binding.textViewUserEmail.text = user.email ?: "No disponible"
            binding.textViewUserName.text = user.displayName ?: user.email?.split("@")?.get(0) ?: "Usuario"
        }
        
        // Cargar preferencias de notificaciones
        binding.switchNotifications.isChecked = sharedPreferences.getBoolean("notifications_enabled", true)
        binding.switchEventReminders.isChecked = sharedPreferences.getBoolean("event_reminders_enabled", true)
        binding.switchCommentNotifications.isChecked = sharedPreferences.getBoolean("comment_notifications_enabled", true)
        
        // Cargar preferencia de autenticación biométrica
        try {
            val secureStorage = SecureStorageHelper(this)
            val biometricHelper = BiometricHelper(this)
            val isBiometricAvailable = biometricHelper.isBiometricAvailable()
            val isBiometricEnabled = secureStorage.isBiometricEnabled()
            
            binding.switchBiometric.isChecked = isBiometricEnabled
            binding.switchBiometric.isEnabled = isBiometricAvailable
            
            if (!isBiometricAvailable) {
                binding.switchBiometric.alpha = 0.5f
            }
        } catch (e: Exception) {
            Log.e("SettingsActivity", "Error al cargar configuración biométrica", e)
            binding.switchBiometric.isEnabled = false
            binding.switchBiometric.alpha = 0.5f
        }
    }
    
    // ========== EDITAR PERFIL ==========
    private fun showEditProfileDialog() {
        val user = firebaseAuth.currentUser ?: return
        
        // Cargar datos actuales desde Firestore
        FirestoreUtil.getUserProfile(user.uid,
            onSuccess = { profileData ->
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Editar Perfil")
                
                val view = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
                val nameInput = view.findViewById<android.widget.EditText>(R.id.editTextName)
                val emailInput = view.findViewById<android.widget.EditText>(R.id.editTextEmail)
                val phoneInput = view.findViewById<android.widget.EditText>(R.id.editTextPhone)
                val bioInput = view.findViewById<android.widget.EditText>(R.id.editTextBio)
                
                nameInput.setText(profileData?.get("displayName") as? String ?: user.displayName ?: "")
                emailInput.setText(user.email ?: "")
                phoneInput.setText(profileData?.get("phone") as? String ?: "")
                bioInput.setText(profileData?.get("bio") as? String ?: "")
                
                builder.setView(view)
                builder.setPositiveButton("Guardar") { _, _ ->
                    val newName = nameInput.text.toString().trim()
                    val newEmail = emailInput.text.toString().trim()
                    val newPhone = phoneInput.text.toString().trim()
                    val newBio = bioInput.text.toString().trim()
                    
                    if (newName.isEmpty()) {
                        Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    
                    if (newEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                        Toast.makeText(this, "Email inválido", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    
                    updateProfile(newName, newEmail, newPhone, newBio)
                }
                builder.setNegativeButton("Cancelar", null)
                builder.show()
            },
            onFailure = { e ->
                Log.e("SettingsActivity", "Error al cargar perfil", e)
                Toast.makeText(this, "Error al cargar datos del perfil", Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    private fun updateProfile(name: String, email: String, phone: String, bio: String) {
        val user = firebaseAuth.currentUser ?: return
        
        // Actualizar display name en Firebase Auth
        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()
        
        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("SettingsActivity", "Nombre actualizado")
                } else {
                    Log.e("SettingsActivity", "Error al actualizar nombre", task.exception)
                }
            }
        
        // Actualizar información adicional en Firestore
        FirestoreUtil.updateUserInfo(
            userId = user.uid,
            displayName = name,
            phone = if (phone.isNotEmpty()) phone else null,
            bio = if (bio.isNotEmpty()) bio else null,
            onSuccess = {
                Log.d("SettingsActivity", "Información adicional actualizada")
            },
            onFailure = { e ->
                Log.e("SettingsActivity", "Error al actualizar información adicional", e)
            }
        )
        
        // Actualizar email si cambió
        if (email != user.email) {
            user.updateEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show()
                        loadSettings()
                    } else {
                        Toast.makeText(this, "Error al actualizar email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        Log.e("SettingsActivity", "Error al actualizar email", task.exception)
                    }
                }
        } else {
            Toast.makeText(this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show()
            loadSettings()
        }
    }
    
    // ========== CAMBIAR CONTRASEÑA ==========
    private fun showChangePasswordDialog() {
        val user = firebaseAuth.currentUser
        if (user == null || user.providerData.isEmpty() || 
            !user.providerData.any { it.providerId == "password" }) {
            Toast.makeText(this, "Esta opción solo está disponible para cuentas con email/contraseña", Toast.LENGTH_LONG).show()
            return
        }
        
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Cambiar Contraseña")
        
        val view = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val currentPasswordInput = view.findViewById<android.widget.EditText>(R.id.editTextCurrentPassword)
        val newPasswordInput = view.findViewById<android.widget.EditText>(R.id.editTextNewPassword)
        val confirmPasswordInput = view.findViewById<android.widget.EditText>(R.id.editTextConfirmPassword)
        
        builder.setView(view)
        builder.setPositiveButton("Cambiar") { _, _ ->
            val currentPassword = currentPasswordInput.text.toString()
            val newPassword = newPasswordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()
            
            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Todos los campos son requeridos", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            
            if (newPassword.length < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            
            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            
            changePassword(currentPassword, newPassword)
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }
    
    private fun changePassword(currentPassword: String, newPassword: String) {
        val user = firebaseAuth.currentUser ?: return
        val email = user.email ?: return
        
        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        
        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    user.updatePassword(newPassword)
                        .addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                Toast.makeText(this, "Contraseña actualizada correctamente", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Error al actualizar contraseña: ${updateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                Log.e("SettingsActivity", "Error al actualizar contraseña", updateTask.exception)
                            }
                        }
                } else {
                    Toast.makeText(this, "Contraseña actual incorrecta", Toast.LENGTH_SHORT).show()
                    Log.e("SettingsActivity", "Error en reautenticación", reauthTask.exception)
                }
            }
    }
    
    // ========== CUENTAS VINCULADAS ==========
    private fun showLinkedAccountsDialog() {
        val user = firebaseAuth.currentUser ?: return
        
        val providers = user.providerData.map { it.providerId }
        val providerNames = providers.map { providerId ->
            when (providerId) {
                "google.com" -> "Google"
                "facebook.com" -> "Facebook"
                "password" -> "Email/Contraseña"
                else -> providerId
            }
        }
        
        val message = if (providerNames.isNotEmpty()) {
            "Cuentas vinculadas:\n\n${providerNames.joinToString("\n") { "• $it" }}"
        } else {
            "No hay cuentas vinculadas"
        }
        
        AlertDialog.Builder(this)
            .setTitle("Cuentas Vinculadas")
            .setMessage(message)
            .setPositiveButton("Cerrar", null)
            .show()
    }
    
    // ========== ELIMINAR CUENTA ==========
    private fun showDeleteAccountConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Cuenta")
            .setMessage("¿Estás seguro de que deseas eliminar tu cuenta? Esta acción no se puede deshacer y se eliminarán todos tus datos.")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun deleteAccount() {
        val user = firebaseAuth.currentUser ?: return
        
        // Eliminar datos de Firestore
        FirebaseFirestore.getInstance().collection("users").document(user.uid)
            .delete()
            .addOnSuccessListener {
                Log.d("SettingsActivity", "Documento de usuario eliminado de Firestore")
            }
            .addOnFailureListener { e ->
                Log.e("SettingsActivity", "Error al eliminar documento de usuario", e)
            }
        
        // Eliminar cuenta de Firebase Auth
        user.delete()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Cuenta eliminada correctamente", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Error al eliminar cuenta: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    Log.e("SettingsActivity", "Error al eliminar cuenta", task.exception)
                }
            }
    }
    
    // ========== NOTIFICACIONES ==========
    private fun saveNotificationPreference(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("notifications_enabled", enabled).apply()
        Toast.makeText(this, if (enabled) "Notificaciones activadas" else "Notificaciones desactivadas", Toast.LENGTH_SHORT).show()
    }
    
    private fun saveEventReminderPreference(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("event_reminders_enabled", enabled).apply()
    }
    
    private fun saveCommentNotificationPreference(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("comment_notifications_enabled", enabled).apply()
    }
    
    // ========== AUTENTICACIÓN BIOMÉTRICA ==========
    private fun handleBiometricToggle(isEnabled: Boolean) {
        val secureStorage = SecureStorageHelper(this)
        val user = firebaseAuth.currentUser
        
        if (isEnabled) {
            // Habilitar autenticación biométrica
            if (user != null && user.email != null) {
                // Necesitamos la contraseña para guardarla. Mostrar diálogo para confirmar contraseña
                showPasswordConfirmationDialog { password ->
                    if (password.isNotEmpty()) {
                        // Verificar que la contraseña es correcta antes de guardar
                        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(user.email!!, password)
                        user.reauthenticate(credential)
                            .addOnSuccessListener {
                                // Contraseña correcta, guardar credenciales
                                secureStorage.saveCredentials(user.email!!, password)
                                secureStorage.setBiometricEnabled(true)
                                Toast.makeText(this, "Autenticación biométrica habilitada", Toast.LENGTH_SHORT).show()
                                Log.d("SettingsActivity", "Autenticación biométrica habilitada")
                            }
                            .addOnFailureListener { e ->
                                Log.e("SettingsActivity", "Error al verificar contraseña", e)
                                Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                                binding.switchBiometric.isChecked = false
                            }
                    } else {
                        binding.switchBiometric.isChecked = false
                    }
                }
            } else {
                Toast.makeText(this, "No se puede habilitar. Inicia sesión con email y contraseña primero.", Toast.LENGTH_LONG).show()
                binding.switchBiometric.isChecked = false
            }
        } else {
            // Deshabilitar autenticación biométrica
            secureStorage.setBiometricEnabled(false)
            secureStorage.clearCredentials()
            Toast.makeText(this, "Autenticación biométrica deshabilitada", Toast.LENGTH_SHORT).show()
            Log.d("SettingsActivity", "Autenticación biométrica deshabilitada")
        }
    }
    
    private fun showPasswordConfirmationDialog(onConfirm: (String) -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirmar contraseña")
        builder.setMessage("Ingresa tu contraseña para habilitar la autenticación biométrica")
        
        val input = android.widget.EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.hint = "Contraseña"
        builder.setView(input)
        
        builder.setPositiveButton("Confirmar") { _, _ ->
            val password = input.text.toString()
            onConfirm(password)
        }
        
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }
        
        builder.show()
    }
    
    // ========== PRIVACIDAD ==========
    private fun openPrivacyPolicy() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com/privacy-policy"))
        startActivity(intent)
    }
    
    private fun openTermsOfService() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com/terms-of-service"))
        startActivity(intent)
    }
    
    // ========== INFORMACIÓN ==========
    private fun showAboutDialog() {
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        val versionCode = packageManager.getPackageInfo(packageName, 0).versionCode
        
        AlertDialog.Builder(this)
            .setTitle("Acerca de")
            .setMessage("Event.ly\n\nVersión: $versionName ($versionCode)\n\nAplicación de gestión de eventos")
            .setPositiveButton("Cerrar", null)
            .show()
    }
    
    // ========== SUBIR FOTO DE PERFIL ==========
    private fun checkPermissionAndSelectImage() {
        // En Android 13+ (API 33+), usar READ_MEDIA_IMAGES
        // En versiones anteriores, usar READ_EXTERNAL_STORAGE
        // Para ACTION_GET_CONTENT no se necesita permiso en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: usar READ_MEDIA_IMAGES solo para ACTION_PICK
            // Pero mejor usar ACTION_GET_CONTENT que no requiere permisos
            showImageSourceDialog()
        } else {
            // Android 12 y anteriores: verificar READ_EXTERNAL_STORAGE
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    showImageSourceDialog()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }
    
    private fun showImageSourceDialog() {
        val options = arrayOf("Galería", "Cámara")
        AlertDialog.Builder(this)
            .setTitle("Seleccionar foto de perfil")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickImageFromGallery()
                    1 -> takePictureWithCamera()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun pickImageFromGallery() {
        // Usar ACTION_GET_CONTENT que funciona mejor en Android 13+ sin permisos
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        // Intent alternativo si ACTION_GET_CONTENT no está disponible
        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val chooserIntent = Intent.createChooser(intent, "Seleccionar imagen")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(pickIntent))
        pickImageLauncher.launch(chooserIntent)
    }
    
    private fun takePictureWithCamera() {
        try {
            val photoFile = File(getExternalFilesDir(null), "profile_photo_${System.currentTimeMillis()}.jpg")
            selectedImageUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )
            selectedImageUri?.let { uri ->
                takePictureLauncher.launch(uri)
            } ?: run {
                Toast.makeText(this, "Error al crear archivo para la foto", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("SettingsActivity", "Error al tomar foto", e)
            Toast.makeText(this, "Error al acceder a la cámara: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun uploadProfilePhoto(imageUri: Uri) {
        val user = firebaseAuth.currentUser ?: return
        
        Toast.makeText(this, "Subiendo foto...", Toast.LENGTH_SHORT).show()
        
        try {
            // Copiar la imagen a un archivo temporal para evitar problemas con URIs de contenido
            val tempDir = getExternalFilesDir(null) ?: cacheDir
            val tempFile = File(tempDir, "temp_profile_${System.currentTimeMillis()}.jpg")
            
            contentResolver.openInputStream(imageUri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: run {
                Toast.makeText(this, "Error al leer la imagen seleccionada", Toast.LENGTH_SHORT).show()
                Log.e("SettingsActivity", "No se pudo abrir el InputStream de la URI")
                return
            }
            
            val tempUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                tempFile
            )
            val storageRef = storage.reference
            val photoRef = storageRef.child("profile_photos/${user.uid}.jpg")
            
            photoRef.putFile(tempUri)
                .addOnSuccessListener { taskSnapshot ->
                    photoRef.downloadUrl.addOnSuccessListener { uri ->
                        val photoUrl = uri.toString()
                        
                        // Actualizar en Firestore
                        FirestoreUtil.updateUserPhotoUrl(
                            userId = user.uid,
                            photoUrl = photoUrl,
                            onSuccess = {
                                // Actualizar también en Firebase Auth
                                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                    .setPhotoUri(uri)
                                    .build()
                                
                                user.updateProfile(profileUpdates)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Toast.makeText(this, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show()
                                            loadSettings()
                                        } else {
                                            Toast.makeText(this, "Foto guardada, pero error al actualizar perfil", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            },
                            onFailure = { e ->
                                Toast.makeText(this, "Error al guardar URL: ${e.message}", Toast.LENGTH_SHORT).show()
                                Log.e("SettingsActivity", "Error al guardar URL de foto", e)
                            }
                        )
                        
                        // Limpiar archivo temporal
                        try {
                            if (tempFile.exists()) {
                                tempFile.delete()
                            }
                        } catch (e: Exception) {
                            Log.e("SettingsActivity", "Error al eliminar archivo temporal", e)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al subir foto: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("SettingsActivity", "Error al subir foto", e)
                    
                    // Limpiar archivo temporal en caso de error
                    try {
                        if (tempFile.exists()) {
                            tempFile.delete()
                        }
                    } catch (ex: Exception) {
                        Log.e("SettingsActivity", "Error al eliminar archivo temporal", ex)
                    }
                }
                .addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                    Log.d("SettingsActivity", "Progreso de subida: $progress%")
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al procesar imagen: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("SettingsActivity", "Error al procesar imagen", e)
        }
    }
}

