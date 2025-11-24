package com.foro_2

import android.os.Build
import android.view.View
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity

object SystemUIHelper {
    
    /**
     * Configura la actividad para que respete las barras del sistema
     * y se adapte cuando aparece el teclado
     */
    fun setupSystemBars(activity: AppCompatActivity) {
        try {
            // Asegurar que las barras del sistema sean visibles y respetadas
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ (API 30+)
                try {
                    activity.window.insetsController?.let { controller ->
                        // Mostrar barras del sistema
                        controller.show(WindowInsets.Type.systemBars())
                    }
                } catch (e: Exception) {
                    // Si hay algún error, usar el método antiguo
                    @Suppress("DEPRECATION")
                    activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                }
            } else {
                // Android 10 y anteriores - solo asegurar que las barras sean visibles
                @Suppress("DEPRECATION")
                activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        } catch (e: Exception) {
            // Si hay algún error crítico, simplemente ignorarlo para evitar crash
            android.util.Log.e("SystemUIHelper", "Error al configurar barras del sistema", e)
        }
    }
}

