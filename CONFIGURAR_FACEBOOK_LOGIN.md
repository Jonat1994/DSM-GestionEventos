# Configuración de Facebook Login

## Pasos para Configurar Facebook Login

### Paso 1: Crear una App en Facebook Developers

1. Ve a [Facebook Developers](https://developers.facebook.com/)
2. Inicia sesión con tu cuenta de Facebook
3. Haz clic en **Mis Apps** → **Crear App**
4. Selecciona **Consumidor** como tipo de app
5. Completa el formulario con:
   - **Nombre de la app**: El nombre de tu aplicación
   - **Email de contacto**: Tu email
6. Haz clic en **Crear App**

### Paso 2: Agregar Facebook Login a tu App

1. En el panel de tu app, busca **Facebook Login** en el menú de productos
2. Haz clic en **Configurar** en Facebook Login
3. Selecciona **Android** como plataforma

### Paso 3: Configurar la App Android

1. En la configuración de Android, ingresa:
   - **Nombre del paquete**: `com.foro_2`
   - **Nombre de la clase principal**: `com.foro_2.SplashActivity`
   - **Hash de clave**: Necesitas obtener el SHA-1 de tu keystore

2. Para obtener el SHA-1 (keystore de debug):
   ```bash
   # Windows (PowerShell)
   keytool -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
   ```
   
   Busca la línea que dice **SHA1:** y copia el valor (formato: `XX:XX:XX:...`)

3. Pega el SHA-1 en el campo **Hash de clave** en Facebook Developers
4. Guarda los cambios

### Paso 4: Obtener el App ID y Client Token

1. En el panel de tu app, ve a **Configuración** → **Básico**
2. Copia el **ID de la app** (App ID)
3. Copia el **Token de cliente** (Client Token) si está disponible

### Paso 5: Configurar strings.xml

1. Abre `app/src/main/res/values/strings.xml`
2. Reemplaza `TU_FACEBOOK_APP_ID_AQUI` con tu **App ID** de Facebook
3. Reemplaza `TU_FACEBOOK_CLIENT_TOKEN_AQUI` con tu **Client Token** (si lo tienes)
4. Reemplaza `fbTU_FACEBOOK_APP_ID_AQUI` en `fb_login_protocol_scheme` con `fb` + tu App ID
   - Ejemplo: Si tu App ID es `123456789`, el protocol scheme debe ser `fb123456789`

Ejemplo:
```xml
<string name="facebook_app_id">1234567890123456</string>
<string name="facebook_client_token">abc123def456ghi789</string>
<string name="fb_login_protocol_scheme">fb1234567890123456</string>
```

### Paso 6: Habilitar Facebook Login en Firebase

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Selecciona tu proyecto
3. Ve a **Authentication** → **Sign-in method**
4. Busca **Facebook** en la lista
5. Haz clic en **Facebook** y luego en **Habilitar**
6. Ingresa tu **App ID** y **App Secret** de Facebook
   - El **App Secret** lo encuentras en Facebook Developers → Configuración → Básico
7. Haz clic en **Guardar**

### Paso 7: Limpiar y Reconstruir el Proyecto

En Android Studio:
- **Build** → **Clean Project**
- **Build** → **Rebuild Project**

O desde la terminal:
```bash
.\gradlew clean
.\gradlew build
```

### Paso 8: Probar Facebook Login

1. Ejecuta la app en un dispositivo o emulador
2. Haz clic en el botón de Facebook en la pantalla de login
3. Deberías ver el diálogo de Facebook Login
4. Inicia sesión con tu cuenta de Facebook

## Solución de Problemas

### Error: "Facebook App ID no configurado"
- Verifica que hayas reemplazado `TU_FACEBOOK_APP_ID_AQUI` en `strings.xml` con tu App ID real

### Error: "Invalid key hash"
- Asegúrate de haber agregado el SHA-1 correcto en Facebook Developers
- Si usas un keystore de release, también necesitas agregar su SHA-1

### Error: "Facebook login cancelado"
- Esto es normal si el usuario cancela el proceso
- Verifica que Facebook Login esté habilitado en Firebase Console

### La app no abre Facebook Login
- Verifica que el `fb_login_protocol_scheme` en `strings.xml` tenga el formato correcto: `fb` + tu App ID
- Asegúrate de haber limpiado y reconstruido el proyecto después de los cambios

## Notas Importantes

- El **App ID** y **Client Token** son sensibles, pero están en `strings.xml` que normalmente no se sube a repositorios públicos si está en `.gitignore`
- Necesitas tener el **App Secret** de Facebook para configurarlo en Firebase Console
- El SHA-1 debe coincidir exactamente con el keystore que estás usando (debug o release)

