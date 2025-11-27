# Solución de Problemas: Notificaciones y Conteo de Asistencia

## Problemas Corregidos

### 1. ✅ Conteo de Asistencia en Tiempo Real

**Problema:** El conteo de asistencia no se actualizaba cuando otros usuarios confirmaban asistencia.

**Solución Implementada:**
- Agregado listener en tiempo real (`listenToEventAttendances`) en `EventDetailsActivity`
- El conteo se actualiza automáticamente cuando cualquier usuario confirma/cancela asistencia
- Mejorado logging para diagnosticar problemas

**Cambios:**
- `EventDetailsActivity.setupAttendanceListener()` - Escucha cambios en tiempo real
- `FirestoreUtil.confirmAttendance()` - Mejorado logging y manejo de timestamps
- `FirestoreUtil.getConfirmedAttendeesCount()` - Agregado logging detallado

### 2. ✅ Notificaciones Push en Múltiples Dispositivos

**Problema:** Las notificaciones no se recibían en otros dispositivos.

**Soluciones Implementadas:**

#### A. Mejora en Guardado de Tokens FCM
- Verificación antes de actualizar tokens (evita escrituras innecesarias)
- Uso de `SetOptions.merge()` como fallback si falla `update()`
- Logging detallado para diagnosticar problemas
- Manejo robusto de errores

**Archivos modificados:**
- `LoginActivity.initializeFCMToken()` - Mejorado guardado de tokens
- `EventsListActivity.initializeFCMToken()` - Mejorado guardado de tokens
- `MyFirebaseMessagingService.onNewToken()` - Guarda tokens automáticamente

#### B. Mejora en Envío de Notificaciones
- Logging detallado de usuarios con/sin tokens
- Identificación de usuarios que necesitan iniciar sesión
- Mejor manejo de errores

**Archivo modificado:**
- `FirestoreUtil.sendEventNotificationToUsers()` - Logging mejorado

## Verificaciones Necesarias

### Para Notificaciones:

1. **Verificar que los tokens se guarden:**
   ```
   - Abre Logcat y filtra por "FCM" o "LoginActivity"
   - Inicia sesión en cada dispositivo
   - Busca: "Token FCM guardado/actualizado en Firestore"
   - Verifica en Firestore que cada usuario tenga "fcmToken"
   ```

2. **Verificar que Cloud Functions esté desplegada:**
   ```bash
   firebase functions:list
   ```
   Debe mostrar `sendEventNotifications`

3. **Verificar que se creen notificaciones pendientes:**
   - Crea un evento como organizador
   - Ve a Firestore → Colección `pending_notifications`
   - Debe crearse un documento con status "pending"
   - Debe cambiar a "sent" automáticamente

4. **Verificar logs de Cloud Functions:**
   ```bash
   firebase functions:log
   ```
   O en Firebase Console → Functions → Logs

### Para Conteo de Asistencia:

1. **Verificar que las asistencias se guarden:**
   - Confirma asistencia en un evento
   - Ve a Firestore → Colección `attendances`
   - Debe haber un documento con:
     - `eventId`: ID del evento
     - `userId`: ID del usuario
     - `status`: "CONFIRMED"
     - `timestamp`: timestamp actual

2. **Verificar que el listener funcione:**
   - Abre un evento en un dispositivo
   - Confirma asistencia desde otro dispositivo
   - El conteo debe actualizarse automáticamente en el primer dispositivo

## Logs para Diagnosticar

### Notificaciones:
```
Filtro: "FCM" o "FirestoreUtil"
Buscar:
- "Token FCM obtenido exitosamente"
- "Token FCM guardado/actualizado"
- "=== INICIANDO ENVÍO DE NOTIFICACIONES ==="
- "Tokens FCM válidos: X"
- "Usuarios sin token: X"
```

### Asistencia:
```
Filtro: "FirestoreUtil" o "EventDetailsActivity"
Buscar:
- "Confirmando asistencia"
- "Asistencia creada exitosamente"
- "Conteo de asistencia actualizado: X"
```

## Problemas Comunes y Soluciones

### Notificaciones no se reciben:

1. **Los usuarios no tienen tokens:**
   - Solución: Los usuarios deben iniciar sesión al menos una vez
   - Verifica en Firestore que tengan `fcmToken` en su documento

2. **Cloud Functions no está desplegada:**
   - Solución: Sigue las instrucciones en `DEPLOY_FUNCTIONS.md`
   - Verifica: `firebase functions:list`

3. **Permisos de notificaciones:**
   - Android 13+: Verifica que la app tenga permisos de notificaciones
   - Configuración → Apps → Tu App → Notificaciones

4. **Tokens inválidos:**
   - Los tokens pueden expirar si el usuario desinstala/reinstala la app
   - Cloud Functions elimina tokens inválidos automáticamente

### Conteo de asistencia no se actualiza:

1. **Listener no está activo:**
   - Verifica que `setupAttendanceListener()` se llame en `onCreate`
   - Verifica que no se remueva el listener prematuramente

2. **Asistencias no se guardan:**
   - Revisa los logs: "Error al crear asistencia"
   - Verifica permisos de Firestore para la colección `attendances`

3. **Query no funciona:**
   - Verifica que exista un índice compuesto en Firestore
   - Firestore puede pedir crear índices automáticamente

## Índices de Firestore Necesarios

Si Firestore pide crear índices, créalos:

1. **Para asistencias:**
   - Colección: `attendances`
   - Campos: `eventId` (Ascending), `status` (Ascending)

2. **Para usuarios:**
   - Colección: `users`
   - Campos: `role` (Ascending), `fcmToken` (Ascending)

## Próximos Pasos

1. **Probar en dispositivos:**
   - Instala la app en varios dispositivos
   - Inicia sesión en cada uno
   - Verifica que los tokens se guarden

2. **Probar notificaciones:**
   - Crea un evento como organizador
   - Verifica que los usuarios reciban notificaciones

3. **Probar asistencia:**
   - Confirma asistencia desde múltiples dispositivos
   - Verifica que el conteo se actualice en tiempo real

## Contacto para Soporte

Si los problemas persisten:
1. Revisa los logs detallados en Logcat
2. Verifica los logs de Cloud Functions
3. Revisa Firestore Console para ver los datos
4. Comparte los logs específicos para diagnóstico

