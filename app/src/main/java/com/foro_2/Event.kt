package com.foro_2

data class Event(
    var id: String = "",
    val organizerId: String = "", // ID del organizador/admin que creó el evento
    val title: String = "",
    val description: String = "",
    val date: String = "", // Formato: dd/MM/yyyy
    val time: String = "", // Formato: HH:mm
    val location: String = "",
    val imageUrl: String = "", // URL de la imagen del evento
    val timestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "organizerId" to organizerId,
            "title" to title,
            "description" to description,
            "date" to date,
            "time" to time,
            "location" to location,
            "imageUrl" to imageUrl,
            "timestamp" to timestamp,
            "createdAt" to createdAt
        )
    }
}

