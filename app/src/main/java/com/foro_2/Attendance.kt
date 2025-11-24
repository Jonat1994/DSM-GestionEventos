package com.foro_2

data class Attendance(
    var id: String = "",
    val userId: String = "",
    val eventId: String = "",
    val status: String = "", // "CONFIRMED" o "CANCELLED"
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "eventId" to eventId,
            "status" to status,
            "timestamp" to timestamp
        )
    }
}

