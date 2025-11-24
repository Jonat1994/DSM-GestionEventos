package com.foro_2

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

class ViewExpensesActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout
    private lateinit var auth: FirebaseAuth
    private lateinit var tvTotalExpenses: TextView
    private lateinit var spinnerFilterCategory: Spinner
    private lateinit var btnFilter: Button
    private lateinit var btnClearFilter: Button
    private var expensesListener: ListenerRegistration? = null
    private var allExpenses: List<Expense> = emptyList()

    // Categorías para el filtro
    private val categories = arrayOf(
        "Todas",
        "Alimentación",
        "Transporte",
        "Servicios",
        "Entretenimiento",
        "Salud",
        "Educación",
        "Compras",
        "Vivienda",
        "Otros"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_expenses)

        container = findViewById(R.id.expenseListContainer)
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses)
        spinnerFilterCategory = findViewById(R.id.spinnerFilterCategory)
        btnFilter = findViewById(R.id.btnFilter)
        btnClearFilter = findViewById(R.id.btnClearFilter)
        auth = FirebaseAuth.getInstance()

        // Configurar el Spinner de categorías
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFilterCategory.adapter = adapter

        // Configurar botón de filtrar
        btnFilter.setOnClickListener {
            val selectedCategory = spinnerFilterCategory.selectedItem.toString()
            applyFilter(selectedCategory)
        }

        // Configurar botón de limpiar filtro
        btnClearFilter.setOnClickListener {
            spinnerFilterCategory.setSelection(0)
            applyFilter("Todas")
        }

        // Botón para volver al Home
        findViewById<Button>(R.id.btnBackToHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        setupExpensesListener()
    }

    override fun onPause() {
        super.onPause()
        expensesListener?.remove()
    }

    private fun setupExpensesListener() {
        val user = auth.currentUser ?: run {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        expensesListener = FirestoreUtil.listenToUserExpenses(user.uid) { expenses ->
            allExpenses = expenses.sortedByDescending { it.timestamp }
            applyFilter("Todas")
        }
    }

    private fun applyFilter(category: String) {
        val filteredExpenses = if (category == "Todas") {
            allExpenses
        } else {
            allExpenses.filter { it.category == category }
        }
        displayExpenses(filteredExpenses)
    }

    private fun displayExpenses(expenses: List<Expense>) {
        container.removeAllViews()

        if (expenses.isEmpty()) {
            Toast.makeText(this, "No hay gastos registrados", Toast.LENGTH_SHORT).show()
            tvTotalExpenses.text = "Total: $0.00"
            return
        }

        // Calcular total
        val total = expenses.sumOf { it.amount }
        tvTotalExpenses.text = String.format("Total: $%.2f", total)

        for (expense in expenses) {
            val view = layoutInflater.inflate(R.layout.expense_item, container, false)

            view.findViewById<TextView>(R.id.tvExpenseName).text = expense.name
            view.findViewById<TextView>(R.id.tvExpenseAmount).text = String.format("$%.2f", expense.amount)
            view.findViewById<TextView>(R.id.tvExpenseCategory).text = expense.category
            view.findViewById<TextView>(R.id.tvExpenseDate).text = expense.date

            // Botón eliminar
            val btnDelete = view.findViewById<Button>(R.id.btnDeleteExpense)
            btnDelete.setOnClickListener {
                showDeleteConfirmationDialog(expense)
            }

            container.addView(view)
        }
    }

    private fun showDeleteConfirmationDialog(expense: Expense) {
        AlertDialog.Builder(this)
            .setTitle("¿Eliminar gasto?")
            .setMessage("¿Estás seguro de eliminar \"${expense.name}\"?")
            .setPositiveButton("Sí") { _, _ ->
                FirestoreUtil.deleteExpense(expense.id,
                    onSuccess = {
                        Toast.makeText(this, "Gasto eliminado", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = {
                        Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

