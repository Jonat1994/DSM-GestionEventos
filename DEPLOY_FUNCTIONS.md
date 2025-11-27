# Guía de Despliegue de Cloud Functions

Esta guía te ayudará a configurar y desplegar las Cloud Functions para el sistema de notificaciones.

## Requisitos Previos

1. **Node.js instalado** (versión 18 o superior)
   - Descarga desde: https://nodejs.org/
   - Verifica la instalación: `node --version`

2. **Firebase CLI instalado**
   ```bash
   npm install -g firebase-tools
   ```

3. **Cuenta de Firebase** con el proyecto configurado

## Pasos de Configuración

### 1. Inicializar Firebase en el proyecto

Si aún no has inicializado Firebase, ejecuta:

```bash
firebase login
firebase init functions
```

Cuando te pregunte:
- **¿Qué lenguaje quieres usar?** → JavaScript
- **¿Quieres usar ESLint?** → Sí (opcional)
- **¿Quieres instalar dependencias ahora?** → Sí

### 2. Instalar dependencias

Navega a la carpeta `functions` e instala las dependencias:

```bash
cd functions
npm install
```

### 3. Verificar configuración

Asegúrate de que los archivos estén en su lugar:
- `functions/package.json` ✓
- `functions/index.js` ✓
- `firebase.json` ✓

### 4. Desplegar las funciones

Desde la raíz del proyecto:

```bash
firebase deploy --only functions
```

O si estás en la carpeta `functions`:

```bash
cd ..
firebase deploy --only functions
```

### 5. Verificar el despliegue

1. Ve a Firebase Console: https://console.firebase.google.com/
2. Selecciona tu proyecto
3. Ve a **Functions** en el menú lateral
4. Deberías ver `sendEventNotifications` listada

## Pruebas

### Prueba 1: Crear un evento

1. Inicia sesión como organizador en la app
2. Crea un nuevo evento
3. Verifica en Firebase Console:
   - Ve a **Firestore** → Colección `pending_notifications`
   - Deberías ver un nuevo documento
   - El estado debería cambiar de "pending" a "sent" automáticamente

### Prueba 2: Función de prueba HTTP

Puedes probar manualmente la función HTTP:

```bash
# Obtén la URL de tu función desde Firebase Console
curl https://YOUR_REGION-YOUR_PROJECT.cloudfunctions.net/testNotification
```

O abre la URL en tu navegador.

## Monitoreo

### Ver logs de las funciones

```bash
firebase functions:log
```

O desde Firebase Console:
1. Ve a **Functions**
2. Haz clic en `sendEventNotifications`
3. Ve a la pestaña **Logs**

### Ver métricas

En Firebase Console → Functions → `sendEventNotifications` → **Usage**

## Solución de Problemas

### Error: "Functions directory does not exist"

Asegúrate de que la carpeta `functions` exista y contenga `package.json` e `index.js`.

### Error: "Permission denied"

Ejecuta:
```bash
firebase login
```

### Error: "Module not found"

Desde la carpeta `functions`:
```bash
npm install
```

### Las notificaciones no se envían

1. Verifica los logs: `firebase functions:log`
2. Verifica que haya usuarios con `fcmToken` en Firestore
3. Verifica que los usuarios tengan rol "usuario" (no "organizador")
4. Verifica que se esté creando el documento en `pending_notifications`

### Tokens inválidos

La función automáticamente elimina tokens inválidos de Firestore. Si hay muchos errores:
1. Verifica que los usuarios hayan iniciado sesión recientemente
2. Los tokens FCM pueden expirar si el usuario desinstala/reinstala la app

## Estructura de Archivos

```
proyecto/
├── functions/
│   ├── index.js          # Código de las Cloud Functions
│   ├── package.json      # Dependencias de Node.js
│   └── node_modules/     # Dependencias instaladas
├── firebase.json         # Configuración de Firebase
└── app/                  # Código de la app Android
```

## Costos

Firebase Cloud Functions tiene un plan gratuito generoso:
- **2 millones de invocaciones/mes** gratis
- **400,000 GB-segundos/mes** gratis
- **200,000 CPU-segundos/mes** gratis

Para la mayoría de apps, esto es más que suficiente.

## Actualizar funciones

Si haces cambios en `functions/index.js`:

```bash
firebase deploy --only functions
```

## Eliminar funciones

```bash
firebase functions:delete sendEventNotifications
```

## Recursos Adicionales

- Documentación de Firebase Functions: https://firebase.google.com/docs/functions
- Documentación de FCM: https://firebase.google.com/docs/cloud-messaging
- Precios: https://firebase.google.com/pricing

