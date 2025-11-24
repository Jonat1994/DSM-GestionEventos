package com.foro_2

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout
    private lateinit var auth: FirebaseAuth
    private var eventsListener: ListenerRegistration? = null
    private var attendancesListener: ListenerRegistration? = null
    private var attendedEventIds: List<String> = emptyList()
    private var allEvents: List<Event> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContentView(R.layout.activity_history)
        
        // Configurar barras del sistema después de setContentView
        SystemUIHelper.setupSystemBars(this)

        container = findViewById(R.id.historyListContainer)
        auth = FirebaseAuth.getInstance()

        // Botón para volver al Home
        findViewById<Button>(R.id.btnBackToHome)?.setOnClickListener {
            startActivity(Intent(this, EventsListActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadAttendedEvents()
    }

    override fun onPause() {
        super.onPause()
        eventsListener?.remove()
        attendancesListener?.remove()
    }

    private fun loadAttendedEvents() {
        val user = auth.currentUser ?: run {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Cargar eventos asistidos del usuario
        FirestoreUtil.getUserAttendedEvents(user.uid,
            onSuccess = { eventIds ->
                attendedEventIds = eventIds
                loadAllEvents()
            },
            onFailure = {
                Toast.makeText(this, "Error al cargar historial", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun loadAllEvents() {
        eventsListener = FirestoreUtil.listenToAllEvents { events ->
            allEvents = events
            displayHistory()
        }
    }

    private fun displayHistory() {
        container.removeAllViews()

        // Filtrar solo eventos pasados que el usuario asistió
        val now = System.currentTimeMillis()
        val pastAttendedEvents = allEvents.filter { event ->
            attendedEventIds.contains(event.id) && event.timestamp < now
        }.sortedByDescending { it.timestamp }

        if (pastAttendedEvents.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = "📭\n\nNo hay eventos asistidos en tu historial\n\nConfirma tu asistencia a eventos para verlos aquí"
                textSize = 18f
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                setPadding(32, 100, 32, 32)
                setTextColor(getColor(android.R.color.darker_gray))
            }
            container.addView(emptyView)
            return
        }

        for (event in pastAttendedEvents) {
            val view = LayoutInflater.from(this).inflate(R.layout.item_event_history, container, false)

            val imageView = view.findViewById<ImageView>(R.id.imageViewEvent)
            val titleView = view.findViewById<TextView>(R.id.textViewTitle)
            val dateView = view.findViewById<TextView>(R.id.textViewDate)
            val statusView = view.findViewById<TextView>(R.id.textViewStatus)

            titleView.text = event.title
            dateView.text = "${event.date} ${event.time}"

            // Cargar imagen si existe
            if (event.imageUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(event.imageUrl)
                    .placeholder(R.drawable.ic_event_placeholder)
                    .into(imageView)
            } else {
                imageView.setImageResource(R.drawable.ic_event_placeholder)
            }

            // Estado: siempre "Asistí" porque solo mostramos eventos confirmados
            statusView.text = "🟢 Asistí"
            statusView.setTextColor(getColor(android.R.color.holo_green_dark))

            // Click para ver detalles
            view.setOnClickListener {
                val intent = Intent(this, EventDetailsActivity::class.java)
                intent.putExtra("eventId", event.id)
                startActivity(intent)
            }

            container.addView(view)
        }
    }
}
