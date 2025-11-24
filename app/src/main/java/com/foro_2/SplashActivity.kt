package com.foro_2

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    
    private var videoView: VideoView? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configurar pantalla completa
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        
        setContentView(R.layout.activity_splash)
        
        videoView = findViewById(R.id.videoView)
        val iconLayout = findViewById<View>(R.id.iconLayout)
        
        // Mostrar el icono primero durante 1.5 segundos
        Handler(Looper.getMainLooper()).postDelayed({
            // Ocultar el icono y mostrar el video
            iconLayout?.visibility = View.GONE
            videoView?.visibility = View.VISIBLE
            
            // Intentar cargar el video si existe
            try {
                val videoId = resources.getIdentifier("splash_video", "raw", packageName)
                
                if (videoId != 0) {
                val videoPath = "android.resource://${packageName}/$videoId"
                val videoUri = Uri.parse(videoPath)
                
                videoView?.setVideoURI(videoUri)
                
                // Obtener dimensiones de la pantalla
                val displayMetrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(displayMetrics)
                val screenWidth = displayMetrics.widthPixels
                val screenHeight = displayMetrics.heightPixels
                
                // Obtener dimensiones del video
                var videoWidth = 0
                var videoHeight = 0
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(this, videoUri)
                    videoWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
                    videoHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
                    retriever.release()
                } catch (e: Exception) {
                    // Si hay error al obtener dimensiones, usar valores por defecto
                    videoWidth = 0
                    videoHeight = 0
                }
                
                // Ajustar el VideoView para llenar toda la pantalla sin bordes
                videoView?.post {
                    if (videoWidth > 0 && videoHeight > 0) {
                        val videoAspectRatio = videoWidth.toFloat() / videoHeight.toFloat()
                        val screenAspectRatio = screenWidth.toFloat() / screenHeight.toFloat()
                        
                        val layoutParams = videoView?.layoutParams
                        if (layoutParams != null) {
                            if (videoAspectRatio > screenAspectRatio) {
                                // Video es más ancho, ajustar altura para llenar
                                layoutParams.height = screenHeight
                                layoutParams.width = (screenHeight * videoAspectRatio).toInt()
                            } else {
                                // Video es más alto, ajustar ancho para llenar
                                layoutParams.width = screenWidth
                                layoutParams.height = (screenWidth / videoAspectRatio).toInt()
                            }
                            videoView?.layoutParams = layoutParams
                        }
                    }
                }
                
                // Cuando el video termine, navegar a la siguiente pantalla
                videoView?.setOnCompletionListener {
                    navigateToNextScreen()
                }
                
                // Si hay error al cargar el video, navegar después de 2 segundos
                videoView?.setOnErrorListener { _, _, _ ->
                    Handler(Looper.getMainLooper()).postDelayed({
                        navigateToNextScreen()
                    }, 2000)
                    true
                }
                
                // Iniciar reproducción del video
                videoView?.start()
                
                // Fallback: si el video no termina, navegar después de 5 segundos máximo
                Handler(Looper.getMainLooper()).postDelayed({
                    if (!isFinishing) {
                        navigateToNextScreen()
                    }
                }, 5000)
                } else {
                    // El video no existe, navegar después de 2 segundos
                    Handler(Looper.getMainLooper()).postDelayed({
                        navigateToNextScreen()
                    }, 2000)
                }
            } catch (e: Exception) {
                // Si hay algún error, navegar después de 2 segundos
                Handler(Looper.getMainLooper()).postDelayed({
                    navigateToNextScreen()
                }, 2000)
            }
        }, 1500) // Mostrar el icono durante 1.5 segundos antes del video
    }
    
    private fun navigateToNextScreen() {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        
        val intent = if (user != null) {
            Intent(this, EventsListActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }
        
        startActivity(intent)
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        videoView?.stopPlayback()
        videoView = null
    }
}
