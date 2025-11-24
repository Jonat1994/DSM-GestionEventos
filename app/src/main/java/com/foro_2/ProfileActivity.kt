package com.foro_2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.foro_2.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

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
            Toast.makeText(this, "Edición de perfil próximamente", Toast.LENGTH_SHORT).show()
        }
        
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }
    
    private fun loadUserData() {
        val user = firebaseAuth.currentUser ?: return
        
        binding.textViewName.text = user.displayName ?: user.email?.split("@")?.get(0) ?: "Usuario"
        binding.textViewEmail.text = user.email ?: ""
        
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
        
        // Cargar foto si existe
        if (user.photoUrl != null) {
            // Usar Glide o similar para cargar imagen
            // Por ahora placeholder
        }
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

