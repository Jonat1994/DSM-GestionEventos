package com.foro_2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.foro_2.databinding.ActivityEventDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class EventDetailsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityEventDetailsBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private var eventId: String? = null
    private var currentEvent: Event? = null
    private var attendanceStatus: String? = null
    private var attendanceListener: ListenerRegistration? = null
    private var commentsListener: ListenerRegistration? = null
    private var currentRole: String = "usuario"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityEventDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Configurar barras del sistema después de setContentView
        SystemUIHelper.setupSystemBars(this)
        
        firebaseAuth = FirebaseAuth.getInstance()
        eventId = intent.getStringExtra("eventId")
        
        if (eventId == null) {
            Toast.makeText(this, "Error: Evento no encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        setupUI()
        loadUserRole()
        loadEvent()
        checkAttendance()
        loadComments()
        loadAttendeesCount()
    }
    
    override fun onResume() {
        super.onResume()
        // Recrear el listener cuando la actividad se reanuda
        setupAttendanceListener()
    }
    
    private fun setupAttendanceListener() {
        // Remover listener anterior si existe
        attendanceListener?.remove()
        
        if (eventId == null) {
            android.util.Log.w("EventDetailsActivity", "No se puede configurar listener: eventId es null")
            return
        }
        
        android.util.Log.d("EventDetailsActivity", "Configurando listener de asistencias para evento: $eventId")
        
        // Escuchar cambios en tiempo real en las asistencias
        attendanceListener = FirestoreUtil.listenToEventAttendances(eventId!!) { attendances ->
            runOnUiThread {
                val count = attendances.size
                binding.textViewAttendeesCount.text = getString(R.string.attendees_count, count)
                android.util.Log.d("EventDetailsActivity", "✓ Conteo de asistencia actualizado en tiempo real: $count")
            }
        }
    }
    
    private fun setupUI() {
        binding.btnConfirmAttendance.setOnClickListener {
            confirmAttendance()
        }
        
        binding.btnCancelAttendance.setOnClickListener {
            cancelAttendance()
        }
        
        binding.btnComments.setOnClickListener {
            val intent = Intent(this, CommentsActivity::class.java)
            intent.putExtra("eventId", eventId)
            startActivity(intent)
        }
        
        binding.btnShare.setOnClickListener {
            shareEvent()
        }
    }
    
    private fun loadUserRole() {
        val user = firebaseAuth.currentUser ?: return
        
        FirestoreUtil.getUserRole(user.uid,
            onSuccess = { role ->
                currentRole = role ?: "usuario"
                updateUIForRole()
            },
            onFailure = {
                currentRole = "usuario"
                updateUIForRole()
            }
        )
    }
    
    private fun updateUIForRole() {
        if (currentRole == "organizador" && currentEvent?.organizerId == firebaseAuth.currentUser?.uid) {
            binding.btnEditEvent.visibility = android.view.View.VISIBLE
            binding.btnEditEvent.setOnClickListener {
                val intent = Intent(this, CreateEventActivity::class.java)
                intent.putExtra("eventId", eventId)
                startActivity(intent)
            }
        } else {
            binding.btnEditEvent.visibility = android.view.View.GONE
        }
    }
    
    private fun loadEvent() {
        FirestoreUtil.getEvent(eventId!!,
            onSuccess = { event ->
                if (event != null) {
                    currentEvent = event
                    binding.textViewTitle.text = event.title
                    binding.textViewDate.text = getString(R.string.date_time_format, event.date, event.time)
                    binding.textViewLocation.text = event.location
                    binding.textViewDescription.text = event.description
                    updateUIForRole()
                } else {
                    Toast.makeText(this, getString(R.string.event_not_found), Toast.LENGTH_SHORT).show()
                    finish()
                }
            },
            onFailure = {
                Toast.makeText(this, getString(R.string.error_loading_event, it.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    private fun checkAttendance() {
        val user = firebaseAuth.currentUser ?: return
        
        FirestoreUtil.checkUserAttendance(user.uid, eventId!!,
            onSuccess = { status ->
                attendanceStatus = status
                updateAttendanceButtons()
            },
            onFailure = {
                attendanceStatus = null
                updateAttendanceButtons()
            }
        )
    }
    
    private fun updateAttendanceButtons() {
        if (attendanceStatus == "CONFIRMED") {
            binding.btnConfirmAttendance.visibility = android.view.View.GONE
            binding.btnCancelAttendance.visibility = android.view.View.VISIBLE
        } else {
            binding.btnConfirmAttendance.visibility = android.view.View.VISIBLE
            binding.btnCancelAttendance.visibility = android.view.View.GONE
        }
    }
    
    private fun confirmAttendance() {
        val user = firebaseAuth.currentUser ?: return
        
        val attendance = Attendance(
            userId = user.uid,
            eventId = eventId!!,
            status = "CONFIRMED"
        )
        
        android.util.Log.d("EventDetailsActivity", "Confirmando asistencia para evento: $eventId")
        
        FirestoreUtil.confirmAttendance(attendance,
            onSuccess = {
                android.util.Log.d("EventDetailsActivity", "Asistencia confirmada exitosamente")
                Toast.makeText(this, getString(R.string.attendance_confirmed), Toast.LENGTH_SHORT).show()
                checkAttendance()
                // El listener en tiempo real actualizará el conteo automáticamente
                // Pero también cargamos manualmente por si acaso
                loadAttendeesCount()
            },
            onFailure = { error ->
                android.util.Log.e("EventDetailsActivity", "Error al confirmar asistencia", error)
                Toast.makeText(this, getString(R.string.error_attendance, error.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    private fun cancelAttendance() {
        val user = firebaseAuth.currentUser ?: return
        
        FirestoreUtil.cancelAttendance(user.uid, eventId!!,
            onSuccess = {
                Toast.makeText(this, getString(R.string.attendance_cancelled), Toast.LENGTH_SHORT).show()
                checkAttendance()
                loadAttendeesCount()
            },
            onFailure = {
                Toast.makeText(this, getString(R.string.error_attendance, it.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    private fun loadAttendeesCount() {
        FirestoreUtil.getConfirmedAttendeesCount(eventId!!,
            onSuccess = { count ->
                binding.textViewAttendeesCount.text = getString(R.string.attendees_count, count)
                android.util.Log.d("EventDetailsActivity", "Conteo inicial de asistencia: $count")
            },
            onFailure = { error ->
                android.util.Log.e("EventDetailsActivity", "Error al cargar conteo de asistencia", error)
                binding.textViewAttendeesCount.text = getString(R.string.attendees_count_zero)
            }
        )
    }
    
    private fun loadComments() {
        commentsListener = FirestoreUtil.listenToEventComments(eventId!!) { comments ->
            if (comments.isNotEmpty()) {
                val previewComments = comments.take(2)
                binding.textViewCommentsPreview.text = previewComments.joinToString("\n") { 
                    getString(R.string.comment_format, it.userName, it.text)
                }
            } else {
                binding.textViewCommentsPreview.text = getString(R.string.no_comments)
            }
        }
    }
    
    private fun shareEvent() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        val eventTitle = currentEvent?.title ?: "Evento"
        val eventDate = currentEvent?.date ?: ""
        val eventLocation = currentEvent?.location ?: ""
        shareIntent.putExtra(Intent.EXTRA_TEXT, "$eventTitle\nFecha: $eventDate\nUbicación: $eventLocation")
        startActivity(Intent.createChooser(shareIntent, "Compartir evento"))
    }
    
    override fun onPause() {
        super.onPause()
        // No remover el listener aquí, solo en onDestroy
        // Esto permite que se actualice cuando la actividad está en segundo plano
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Remover listeners solo cuando la actividad se destruye completamente
        attendanceListener?.remove()
        commentsListener?.remove()
        android.util.Log.d("EventDetailsActivity", "Listeners removidos en onDestroy")
    }
}

