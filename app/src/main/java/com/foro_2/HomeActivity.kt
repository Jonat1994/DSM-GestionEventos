package com.foro_2

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.foro_2.databinding.ActivityHomeBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import java.util.Calendar

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private var expensesListener: ListenerRegistration? = null
    
    // Variables para estadísticas de gastos
    private var monthlyTotal: Double = 0.0
    private var categoryTotals: Map<String, Double> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        setupUI()
    }

    private fun setupUI() {
        binding.welcomeText.text = "Hola, ${firebaseAuth.currentUser?.email ?: "Usuario"} 👋"

        binding.btnCreateExpense.setOnClickListener {
            startActivity(Intent(this, CreateExpenseActivity::class.java))
        }

        binding.btnViewExpenses.setOnClickListener {
            startActivity(Intent(this, ViewExpensesActivity::class.java))
        }

        binding.btnViewHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        
        // Botón de prueba temporal para verificar historial (DEBUGGING)
        binding.btnViewHistory.setOnLongClickListener {
            val user = firebaseAuth.currentUser
            if (user != null) {
                // Agregar entrada de prueba directamente
                val testEntry = HistoryEntry(
                    userId = user.uid,
                    action = "ADD",
                    expenseName = "PRUEBA - Gasto de prueba",
                    amount = 99.99,
                    category = "Prueba",
                    date = "01/01/2025",
                    timestamp = System.currentTimeMillis()
                )
                
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("history")
                    .add(testEntry.toMap())
                    .addOnSuccessListener {
                        Toast.makeText(this, "✅ Entrada de prueba agregada al historial", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            true
        }

        binding.signOutButton.setOnClickListener {
            firebaseAuth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadExpensesData()
    }

    override fun onPause() {
        super.onPause()
        expensesListener?.remove()
    }

    private fun loadExpensesData() {
        val user = firebaseAuth.currentUser ?: return

        expensesListener = FirestoreUtil.listenToUserExpenses(user.uid) { expenses ->
            // Calcular estadísticas
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH) + 1
            val currentYear = calendar.get(Calendar.YEAR)

            // Filtrar gastos del mes actual
            val monthlyExpenses = expenses.filter { expense ->
                val parts = expense.date.split("/")
                if (parts.size == 3) {
                    val month = parts[1].toIntOrNull() ?: 0
                    val year = parts[2].toIntOrNull() ?: 0
                    month == currentMonth && year == currentYear
                } else false
            }

            // Calcular total mensual
            monthlyTotal = monthlyExpenses.sumOf { it.amount }
            binding.tvMonthlyTotal.text = String.format("Total Mensual: $%.2f", monthlyTotal)

            // Calcular totales por categoría
            categoryTotals = monthlyExpenses.groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            // Actualizar gráfico
            updateChart(categoryTotals)

            // Mostrar número de gastos del mes
            binding.tvExpenseCount.text = "Gastos este mes: ${monthlyExpenses.size}"
        }
    }

    private fun updateChart(categoryTotals: Map<String, Double>) {
        if (categoryTotals.isEmpty()) {
            binding.pieChart.clear()
            return
        }

        val entries = categoryTotals.map { (category, total) ->
            PieEntry(total.toFloat(), category)
        }

        val colors = listOf(
            Color.parseColor("#FF6384"),
            Color.parseColor("#36A2EB"),
            Color.parseColor("#FFCE56"),
            Color.parseColor("#4BC0C0"),
            Color.parseColor("#9966FF"),
            Color.parseColor("#FF9F40"),
            Color.parseColor("#FF6384"),
            Color.parseColor("#C9CBCF"),
            Color.parseColor("#4BC0C0")
        )

        val dataSet = PieDataSet(entries, "Gastos por Categoría").apply {
            this.colors = colors
            sliceSpace = 3f
            valueTextSize = 14f
            valueTextColor = Color.WHITE
        }

        binding.pieChart.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            isRotationEnabled = true
            centerText = "Gastos por\nCategoría"
            setCenterTextSize(16f)
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(10f)
            animateY(1000)
            invalidate()
        }
    }
}