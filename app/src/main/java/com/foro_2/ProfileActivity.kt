package com.foro_2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.foro_2.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.bumptech.glide.Glide
import android.util.Log

class ProfileActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityProfileBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private var attendancesListener: ListenerRegistration? = null
    private var commentsListener: ListenerRegistration? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Configurar barras del sistema después de setContentView
        SystemUIHelper.setupSystemBars(this)
        
        firebaseAuth = FirebaseAuth.getInstance()
        
        setupUI()
        loadUserData()
        loadStatistics()
    }
    
    private fun setupUI() {
        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }
    
    private fun loadUserData() {
        val user = firebaseAuth.currentUser ?: return
        
        binding.textViewName.text = user.displayName ?: user.email?.split("@")?.get(0) ?: "Usuario"
        binding.textViewEmail.text = user.email ?: ""
        
        // Cargar información adicional desde Firestore
        FirestoreUtil.getUserProfile(user.uid,
            onSuccess = { profileData ->
                profileData?.let { data ->
                    // Cargar foto de perfil
                    val photoUrl = data["photoUrl"] as? String
                    if (!photoUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(photoUrl)
                            .circleCrop()
                            .placeholder(R.drawable.custom_splash_icon)
                            .error(R.drawable.custom_splash_icon)
                            .into(binding.imageViewProfile)
                    } else if (user.photoUrl != null) {
                        // Usar foto de Firebase Auth si no hay en Firestore
                        Glide.with(this)
                            .load(user.photoUrl)
                            .circleCrop()
                            .placeholder(R.drawable.custom_splash_icon)
                            .error(R.drawable.custom_splash_icon)
                            .into(binding.imageViewProfile)
                    }
                    
                    // Cargar información adicional
                    val displayName = data["displayName"] as? String
                    if (!displayName.isNullOrEmpty() && displayName != binding.textViewName.text) {
                        binding.textViewName.text = displayName
                    }
                    
                    val phone = data["phone"] as? String
                    if (!phone.isNullOrEmpty()) {
                        binding.textViewPhone.text = "📱 $phone"
                        binding.textViewPhone.visibility = android.view.View.VISIBLE
                    } else {
                        binding.textViewPhone.visibility = android.view.View.GONE
                    }
                    
                    val bio = data["bio"] as? String
                    if (!bio.isNullOrEmpty()) {
                        binding.textViewBio.text = bio
                        binding.textViewBio.visibility = android.view.View.VISIBLE
                    } else {
                        binding.textViewBio.visibility = android.view.View.GONE
                    }
                }
            },
            onFailure = { e ->
                Log.e("ProfileActivity", "Error al cargar perfil", e)
                // Intentar cargar foto de Firebase Auth
                if (user.photoUrl != null) {
                    Glide.with(this)
                        .load(user.photoUrl)
                        .circleCrop()
                        .placeholder(R.drawable.custom_splash_icon)
                        .error(R.drawable.custom_splash_icon)
                        .into(binding.imageViewProfile)
                }
            }
        )
        
        // Cargar rol
        FirestoreUtil.getUserRole(user.uid,
            onSuccess = { role ->
                binding.textViewAccountType.text = when (role) {
                    "organizador" -> "Organizador"
                    else -> "Usuario"
                }
            },
            onFailure = {
                binding.textViewAccountType.text = "Usuario"
            }
        )
    }
    
    private fun loadStatistics() {
        val user = firebaseAuth.currentUser ?: return
        
        // Contar eventos asistidos
        FirestoreUtil.getUserAttendedEvents(user.uid,
            onSuccess = { eventIds ->
                binding.textViewEventsAttended.text = "Eventos asistidos: ${eventIds.size}"
            },
            onFailure = {
                binding.textViewEventsAttended.text = "Eventos asistidos: 0"
            }
        )
        
        // Contar comentarios (simplificado - se puede mejorar)
        binding.textViewCommentsCount.text = "Comentarios realizados: 0"
    }
    
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro de que deseas cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                firebaseAuth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    override fun onPause() {
        super.onPause()
        attendancesListener?.remove()
        commentsListener?.remove()
    }
}

