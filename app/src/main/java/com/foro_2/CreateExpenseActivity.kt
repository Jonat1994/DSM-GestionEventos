package com.foro_2

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.foro_2.databinding.ActivityCreateExpenseBinding
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

class CreateExpenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateExpenseBinding
    private lateinit var auth: FirebaseAuth

    // Categorías predefinidas de gastos
    private val categories = arrayOf(
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

        // Inicializar ViewBinding
        binding = ActivityCreateExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Configurar el Spinner de categorías
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter

        // Agregar el prefijo "$" al campo de monto
        setupAmountPrefix()
        
        // Configurar el DatePicker para el campo de fecha
        setupDatePicker()

        // Botón para volver al Home (superior)
        binding.btnBackToHomeTop.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        // Botón para crear gasto
        binding.btnCreateExpense.setOnClickListener {
            val user = auth.currentUser
            if (user == null) {
                Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val name = binding.etExpenseName.text.toString()
            val amountStr = binding.etAmount.text.toString().replace("$", "").trim()
            val category = binding.spinnerCategory.selectedItem.toString()
            val date = binding.etDate.text.toString()

            // Validaciones
            if (name.isEmpty()) {
                Toast.makeText(this, "Ingresa el nombre del gasto", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Ingresa el monto", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Ingresa un monto válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (date.isEmpty()) {
                Toast.makeText(this, "Selecciona una fecha", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val expense = Expense(
                userId = user.uid,
                name = name,
                amount = amount,
                category = category,
                date = date
            )

            FirestoreUtil.addExpense(expense,
                onSuccess = {
                    Toast.makeText(this, "Gasto registrado exitosamente", Toast.LENGTH_SHORT).show()
                    finish()
                },
                onFailure = {
                    Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun setupAmountPrefix() {
        // Establecer el prefijo "$" inicial
        binding.etAmount.setText("$")
        binding.etAmount.setSelection(1)
        
        var isUpdating = false
        
        binding.etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                
                val text = s?.toString() ?: ""
                
                // Si el usuario borró todo y el campo está vacío
                if (text.isEmpty()) {
                    isUpdating = true
                    binding.etAmount.setText("$")
                    binding.etAmount.setSelection(1)
                    isUpdating = false
                }
                // Si el texto no comienza con "$" (y no está vacío)
                else if (text.length > 0 && text[0] != '$') {
                    isUpdating = true
                    binding.etAmount.removeTextChangedListener(this)
                    binding.etAmount.setText("$$text")
                    binding.etAmount.setSelection(binding.etAmount.text.length)
                    binding.etAmount.addTextChangedListener(this)
                    isUpdating = false
                }
            }
        })
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        
        // Configurar el click listener para abrir el DatePicker
        binding.etDate.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            
            val datePickerDialog = DatePickerDialog(
                this,
                DatePickerDialog.OnDateSetListener { _, selectedYear, selectedMonth, selectedDay ->
                    // Formatear la fecha seleccionada al formato dd/MM/yyyy
                    val formattedDay = String.format("%02d", selectedDay)
                    val formattedMonth = String.format("%02d", selectedMonth + 1)
                    val formattedDate = "$formattedDay/$formattedMonth/$selectedYear"
                    
                    binding.etDate.setText(formattedDate)
                },
                year,
                month,
                day
            )
            
            datePickerDialog.show()
        }
    }
}

