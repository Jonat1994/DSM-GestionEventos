package com.foro_2

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Nuevo token FCM: $token")
        
        // Guardar el token en Firestore
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null) {
            saveTokenToFirestore(user.uid, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d("FCM", "Mensaje recibido de: ${remoteMessage.from}")
        
        // Verificar si el mensaje contiene datos
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("FCM", "Datos del mensaje: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }
        
        // Verificar si el mensaje contiene notificación
        remoteMessage.notification?.let { notification ->
            Log.d("FCM", "Título: ${notification.title}")
            Log.d("FCM", "Cuerpo: ${notification.body}")
            showNotification(notification.title ?: "Nuevo evento", notification.body ?: "")
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val title = data["title"] ?: "Nuevo evento"
        val body = data["body"] ?: "Se ha creado un nuevo evento"
        val eventId = data["eventId"]
        
        showNotification(title, body, eventId)
    }

    private fun showNotification(title: String, body: String, eventId: String? = null) {
        val channelId = "event_notifications"
        createNotificationChannel(channelId)
        
        // Intent para abrir la app cuando se toque la notificación
        val intent = if (eventId != null) {
            // Abrir detalles del evento específico
            Intent(this, EventDetailsActivity::class.java).apply {
                putExtra("eventId", eventId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        } else {
            // Abrir lista de eventos
            Intent(this, EventsListActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Notificaciones de Eventos"
            val channelDescription = "Notificaciones cuando se crean nuevos eventos"
            val importance = NotificationManager.IMPORTANCE_HIGH
            
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
                enableVibration(true)
                enableLights(true)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun saveTokenToFirestore(userId: String, token: String) {
        val userRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
        
        userRef.update("fcmToken", token)
            .addOnSuccessListener {
                Log.d("FCM", "Token guardado exitosamente en Firestore")
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "Error al guardar token en Firestore", e)
            }
    }
}

