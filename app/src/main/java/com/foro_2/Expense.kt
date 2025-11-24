package com.foro_2

data class Expense(
    var id: String = "",
    val userId: String = "",
    val name: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val date: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "name" to name,
            "amount" to amount,
            "category" to category,
            "date" to date,
            "timestamp" to timestamp
        )
    }
}

