package com.foro_2

data class Comment(
    var id: String = "",
    val userId: String = "",
    val eventId: String = "",
    val userName: String = "",
    val userPhotoUrl: String = "",
    val text: String = "",
    val rating: Int = 0, // 1-5 estrellas
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "eventId" to eventId,
            "userName" to userName,
            "userPhotoUrl" to userPhotoUrl,
            "text" to text,
            "rating" to rating,
            "timestamp" to timestamp
        )
    }
}

