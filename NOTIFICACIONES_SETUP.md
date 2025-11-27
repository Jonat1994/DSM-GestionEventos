# Configuración de Notificaciones Push

## Resumen
Se ha implementado el sistema de notificaciones push usando Firebase Cloud Messaging (FCM). Cuando un organizador crea un evento, todos los usuarios normales reciben una notificación.

## Componentes Implementados

### 1. Servicio de Mensajería (`MyFirebaseMessagingService.kt`)
- Maneja la recepción de tokens FCM
- Procesa notificaciones entrantes
- Muestra notificaciones en el dispositivo

### 2. Guardado de Tokens FCM
- Los tokens se guardan automáticamente en Firestore cuando el usuario inicia sesión
- Se actualizan cuando cambian (método `onNewToken`)

### 3. Envío de Notificaciones
- Cuando se crea un evento, se guarda una notificación pendiente en Firestore
- La colección `pending_notifications` contiene las notificaciones que deben enviarse

## Configuración Requerida

### Opción 1: Usar Cloud Functions (Recomendado)

**Los archivos ya están creados en el proyecto:**
- `functions/index.js` - Código de la función
- `functions/package.json` - Dependencias
- `firebase.json` - Configuración de Firebase

**Para desplegar:**

1. **Instalar Firebase CLI (si no lo tienes):**
```bash
npm install -g firebase-tools
```

2. **Iniciar sesión en Firebase:**
```bash
firebase login
```

3. **Instalar dependencias:**
```bash
cd functions
npm install
```

4. **Desplegar la función:**
```bash
cd ..
firebase deploy --only functions
```

**Ver guía completa en:** `DEPLOY_FUNCTIONS.md`

### Opción 2: Usar API REST de FCM (Alternativa)

Si prefieres no usar Cloud Functions, puedes crear un endpoint en tu backend que:
1. Escuche la colección `pending_notifications`
2. Use la API REST de FCM para enviar notificaciones
3. Requiere la clave del servidor de Firebase

## Estructura de Datos en Firestore

### Colección: `users`
```json
{
  "email": "usuario@example.com",
  "role": "usuario",
  "fcmToken": "token_fcm_aqui"
}
```

### Colección: `pending_notifications`
```json
{
  "eventId": "event_id",
  "eventTitle": "Título del evento",
  "eventDescription": "Descripción",
  "eventDate": "01/01/2024",
  "eventTime": "10:00",
  "eventLocation": "Ubicación",
  "tokens": ["token1", "token2", ...],
  "createdAt": 1234567890,
  "status": "pending"
}
```

## Permisos en AndroidManifest

Ya están configurados:
- Servicio de FCM registrado
- Permisos de Internet

## Pruebas

1. **Verificar que los tokens se guarden:**
   - Inicia sesión como usuario normal
   - Verifica en Firestore que el campo `fcmToken` esté presente en el documento del usuario

2. **Crear un evento como organizador:**
   - Crea un evento
   - Verifica que se cree un documento en `pending_notifications`

3. **Probar notificaciones:**
   - Si usas Cloud Functions, las notificaciones se enviarán automáticamente
   - Si no, necesitas procesar manualmente la colección `pending_notifications`

## Notas Importantes

- Los tokens FCM pueden cambiar, por lo que se actualizan automáticamente
- Solo los usuarios con rol "usuario" reciben notificaciones (no organizadores)
- Las notificaciones incluyen el ID del evento para abrir los detalles al tocarlas
- El canal de notificaciones se crea automáticamente en Android 8.0+

## Solución de Problemas

1. **No se reciben notificaciones:**
   - Verifica que el token FCM esté guardado en Firestore
   - Verifica que Cloud Functions esté desplegada y funcionando
   - Revisa los logs de Cloud Functions en Firebase Console

2. **Token no se guarda:**
   - Verifica los permisos de Firestore
   - Revisa los logs en Logcat con filtro "FCM" o "LoginActivity"

3. **Notificaciones no se envían:**
   - Verifica que haya usuarios con rol "usuario" en Firestore
   - Verifica que los usuarios tengan tokens FCM guardados
   - Revisa los logs de Cloud Functions

