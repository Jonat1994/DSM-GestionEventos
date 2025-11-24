package com.foro_2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class EventsAdapter(
    private var events: List<Event>,
    private val isOrganizer: Boolean,
    private val onEventClick: (Event) -> Unit,
    private val onEditClick: (Event) -> Unit
) : RecyclerView.Adapter<EventsAdapter.EventViewHolder>() {
    
    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageViewEvent)
        val titleView: TextView = itemView.findViewById(R.id.textViewTitle)
        val dateView: TextView = itemView.findViewById(R.id.textViewDate)
        val locationView: TextView = itemView.findViewById(R.id.textViewLocation)
        val btnViewDetails: View = itemView.findViewById(R.id.btnViewDetails)
        val btnEdit: View? = itemView.findViewById(R.id.btnEdit)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        
        holder.titleView.text = event.title
        holder.dateView.text = "${event.date} ${event.time}"
        holder.locationView.text = event.location
        
        // Cargar imagen si existe
        if (event.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(event.imageUrl)
                .placeholder(R.drawable.ic_event_placeholder)
                .into(holder.imageView)
        } else {
            holder.imageView.setImageResource(R.drawable.ic_event_placeholder)
        }
        
        // Botón ver detalles
        holder.btnViewDetails.setOnClickListener {
            onEventClick(event)
        }
        
        // Botón editar (solo organizadores)
        holder.btnEdit?.setOnClickListener {
            onEditClick(event)
        }
        
        // Mostrar/ocultar botón editar según el rol
        holder.btnEdit?.visibility = if (isOrganizer) View.VISIBLE else View.GONE
    }
    
    override fun getItemCount(): Int = events.size
    
    fun updateEvents(newEvents: List<Event>) {
        events = newEvents
        notifyDataSetChanged()
    }
}

