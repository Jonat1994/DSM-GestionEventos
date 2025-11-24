package com.foro_2

data class HistoryEntry(
    var id: String = "",
    val userId: String = "",
    val action: String = "", // "ADD" o "DELETE"
    val expenseName: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val date: String = "", // Fecha del gasto
    val timestamp: Long = System.currentTimeMillis() // Cuándo se realizó la acción
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "action" to action,
            "expenseName" to expenseName,
            "amount" to amount,
            "category" to category,
            "date" to date,
            "timestamp" to timestamp
        )
    }
}

