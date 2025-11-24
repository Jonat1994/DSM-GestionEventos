# Configuración de Google Sign-In

## Problemas comunes y soluciones

### 1. Error: "Google Sign-In falló" o "Error de desarrollo"

**Causa más común:** El SHA-1 fingerprint no está configurado en Firebase Console.

**Solución:**
1. Obtén tu SHA-1 fingerprint:
   ```bash
   # Windows (PowerShell)
   keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
   
   # O si usas un keystore personalizado
   keytool -list -v -keystore ruta/a/tu/keystore.jks -alias tu_alias
   ```

2. Ve a Firebase Console → Tu proyecto → Configuración del proyecto → Tus apps → Android app
3. Agrega el SHA-1 fingerprint en la sección "SHA certificate fingerprints"
4. Descarga el nuevo `google-services.json` y reemplázalo en `app/`

### 2. Error: "default_web_client_id inválido"

**Causa:** El `default_web_client_id` en `strings.xml` no es el Web Client ID correcto.

**Solución:**
1. Ve a Firebase Console → Authentication → Sign-in method → Google
2. Asegúrate de que Google Sign-In esté habilitado
3. Ve a Google Cloud Console → APIs & Services → Credentials
4. Busca el OAuth 2.0 Client ID de tipo "Web application" (no Android)
5. Copia el "Client ID" (debe terminar en `.apps.googleusercontent.com`)
6. Actualiza `app/src/main/res/values/strings.xml`:
   ```xml
   <string name="default_web_client_id">TU_WEB_CLIENT_ID_AQUI.apps.googleusercontent.com</string>
   ```

### 3. Verificar configuración actual

El `default_web_client_id` actual está en:
- `app/src/main/res/values/strings.xml` (línea 26-28)

**IMPORTANTE:** 
- ❌ NO uses el Android Client ID (tipo 3 en google-services.json)
- ✅ SÍ usa el Web Client ID (tipo 3 pero de la aplicación web en Google Cloud Console)

### 4. Logs para depuración

Los logs ahora incluyen información detallada:
- `LoginActivity`: Verifica si Google Sign-In se configuró correctamente
- Códigos de error comunes:
  - `12501`: Usuario canceló el inicio de sesión
  - `10`: Error de desarrollo (verifica SHA-1 y Web Client ID)
  - `7`: Error de conexión

### 5. Pasos de verificación rápida

- [ ] Google Sign-In está habilitado en Firebase Console
- [ ] SHA-1 fingerprint está agregado en Firebase Console
- [ ] `default_web_client_id` en strings.xml es el Web Client ID correcto
- [ ] `google-services.json` está actualizado en `app/`
- [ ] La app tiene permisos de Internet en AndroidManifest.xml

