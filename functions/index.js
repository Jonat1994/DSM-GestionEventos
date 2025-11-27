const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

/**
 * Cloud Function que se ejecuta cuando se crea un nuevo documento en pending_notifications
 * Envía notificaciones push a todos los usuarios con rol "usuario"
 */
exports.sendEventNotifications = functions.firestore
  .document('pending_notifications/{notificationId}')
  .onCreate(async (snap, context) => {
    const notification = snap.data();
    
    console.log('Nueva notificación pendiente:', notification.notificationId || context.params.notificationId);
    
    // Solo procesar si el estado es "pending"
    if (notification.status !== 'pending') {
      console.log('Notificación ya procesada o con estado diferente:', notification.status);
      return null;
    }
    
    const tokens = notification.tokens || [];
    const eventTitle = notification.eventTitle || 'Nuevo Evento';
    const eventDescription = notification.eventDescription || `Se ha creado un nuevo evento: ${eventTitle}`;
    const eventId = notification.eventId || '';
    
    if (tokens.length === 0) {
      console.log('No hay tokens para enviar notificaciones');
      await snap.ref.update({ 
        status: 'failed', 
        error: 'No hay tokens disponibles',
        processedAt: admin.firestore.FieldValue.serverTimestamp()
      });
      return null;
    }
    
    console.log(`Enviando notificaciones a ${tokens.length} usuarios`);
    
    // Preparar el mensaje de notificación
    const message = {
      notification: {
        title: eventTitle,
        body: eventDescription.length > 100 ? eventDescription.substring(0, 100) + '...' : eventDescription
      },
      data: {
        eventId: eventId,
        title: eventTitle,
        body: eventDescription,
        date: notification.eventDate || '',
        time: notification.eventTime || '',
        location: notification.eventLocation || '',
        type: 'new_event'
      },
      android: {
        priority: 'high',
        notification: {
          channelId: 'event_notifications',
          sound: 'default',
          priority: 'high'
        }
      },
      apns: {
        payload: {
          aps: {
            sound: 'default',
            badge: 1
          }
        }
      }
    };
    
    try {
      // Dividir tokens en lotes de 500 (límite de FCM)
      const batchSize = 500;
      let successCount = 0;
      let failureCount = 0;
      
      for (let i = 0; i < tokens.length; i += batchSize) {
        const batch = tokens.slice(i, i + batchSize);
        const batchMessage = {
          ...message,
          tokens: batch
        };
        
        try {
          const response = await admin.messaging().sendEachForMulticast(batchMessage);
          successCount += response.successCount;
          failureCount += response.failureCount;
          
          console.log(`Lote ${Math.floor(i / batchSize) + 1}: ${response.successCount} exitosas, ${response.failureCount} fallidas`);
          
          // Si hay tokens inválidos, eliminarlos de Firestore
          if (response.failureCount > 0) {
            const failedTokens = [];
            response.responses.forEach((resp, idx) => {
              if (!resp.success) {
                failedTokens.push(batch[idx]);
                console.log(`Token inválido: ${batch[idx]}, Error: ${resp.error?.message}`);
              }
            });
            
            // Eliminar tokens inválidos de los documentos de usuario
            if (failedTokens.length > 0) {
              await removeInvalidTokens(failedTokens);
            }
          }
        } catch (error) {
          console.error(`Error al enviar lote ${Math.floor(i / batchSize) + 1}:`, error);
          failureCount += batch.length;
        }
      }
      
      console.log(`Notificaciones enviadas: ${successCount} exitosas, ${failureCount} fallidas`);
      
      // Actualizar estado de la notificación
      await snap.ref.update({ 
        status: 'sent', 
        sentAt: admin.firestore.FieldValue.serverTimestamp(),
        successCount: successCount,
        failureCount: failureCount,
        totalTokens: tokens.length
      });
      
      return { success: true, successCount, failureCount };
    } catch (error) {
      console.error('Error al enviar notificaciones:', error);
      await snap.ref.update({ 
        status: 'failed', 
        error: error.message,
        processedAt: admin.firestore.FieldValue.serverTimestamp()
      });
      throw error;
    }
  });

/**
 * Función auxiliar para eliminar tokens inválidos de Firestore
 */
async function removeInvalidTokens(invalidTokens) {
  const db = admin.firestore();
  const usersRef = db.collection('users');
  
  try {
    const snapshot = await usersRef.where('fcmToken', 'in', invalidTokens).get();
    const batch = db.batch();
    let updateCount = 0;
    
    snapshot.forEach(doc => {
      batch.update(doc.ref, { fcmToken: admin.firestore.FieldValue.delete() });
      updateCount++;
    });
    
    if (updateCount > 0) {
      await batch.commit();
      console.log(`Eliminados ${updateCount} tokens inválidos de Firestore`);
    }
  } catch (error) {
    console.error('Error al eliminar tokens inválidos:', error);
  }
}

/**
 * Función HTTP para probar el envío de notificaciones manualmente
 * URL: https://YOUR_REGION-YOUR_PROJECT.cloudfunctions.net/testNotification
 */
exports.testNotification = functions.https.onRequest(async (req, res) => {
  try {
    const db = admin.firestore();
    const usersSnapshot = await db.collection('users')
      .where('role', '==', 'usuario')
      .where('fcmToken', '!=', null)
      .limit(10)
      .get();
    
    const tokens = [];
    usersSnapshot.forEach(doc => {
      const token = doc.data().fcmToken;
      if (token) tokens.push(token);
    });
    
    if (tokens.length === 0) {
      return res.status(400).json({ error: 'No hay tokens disponibles' });
    }
    
    const message = {
      notification: {
        title: 'Notificación de Prueba',
        body: 'Esta es una notificación de prueba del sistema de eventos'
      },
      data: {
        type: 'test',
        eventId: 'test'
      },
      tokens: tokens
    };
    
    const response = await admin.messaging().sendEachForMulticast(message);
    
    return res.json({
      success: true,
      sent: response.successCount,
      failed: response.failureCount,
      total: tokens.length
    });
  } catch (error) {
    console.error('Error en testNotification:', error);
    return res.status(500).json({ error: error.message });
  }
});

