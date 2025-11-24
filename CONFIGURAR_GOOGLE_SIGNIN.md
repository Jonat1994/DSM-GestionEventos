# 🔧 Configuración de Google Sign-In - PASOS A SEGUIR

## ⚠️ IMPORTANTE: Tu SHA-1 Fingerprint

Tu SHA-1 fingerprint para el keystore de debug es:
```
86:E9:B7:30:46:9F:11:40:76:F8:95:05:19:E1:37:78:B3:A6:5D:01
```

**DEBES agregar este SHA-1 en Firebase Console para que Google Sign-In funcione.**

---

## 📋 Pasos para Configurar Google Sign-In

### Paso 1: Agregar SHA-1 en Firebase Console

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Selecciona tu proyecto: **database-6eb52**
3. Ve a **Configuración del proyecto** (ícono de engranaje)
4. En la sección **Tus apps**, selecciona tu app Android
5. Desplázate hasta **SHA certificate fingerprints**
6. Haz clic en **Agregar huella digital**
7. Pega este SHA-1: `86:E9:B7:30:46:9F:11:40:76:F8:95:05:19:E1:37:78:B3:A6:5D:01`
8. Haz clic en **Guardar**
9. **IMPORTANTE:** Descarga el nuevo archivo `google-services.json` y reemplázalo en `app/google-services.json`

### Paso 2: Verificar que Google Sign-In esté Habilitado

1. En Firebase Console, ve a **Authentication**
2. Haz clic en **Sign-in method**
3. Busca **Google** en la lista
4. Si no está habilitado, haz clic en **Google** y luego en **Habilitar**
5. Guarda los cambios

### Paso 3: Obtener el Web Client ID Correcto

1. Ve a [Google Cloud Console](https://console.cloud.google.com/)
2. Selecciona tu proyecto: **database-6eb52**
3. Ve a **APIs & Services** → **Credentials**
4. Busca el OAuth 2.0 Client ID de tipo **"Web application"** (NO el de Android)
5. Copia el **Client ID** completo (debe terminar en `.apps.googleusercontent.com`)
6. Abre `app/src/main/res/values/strings.xml`
7. Reemplaza el valor de `default_web_client_id` con el Web Client ID que copiaste

### Paso 4: Verificar el Archivo google-services.json

1. Asegúrate de que `app/google-services.json` esté actualizado (después de agregar el SHA-1)
2. Verifica que el `package_name` sea `com.foro_2`

### Paso 5: Limpiar y Reconstruir el Proyecto

En Android Studio o desde la terminal:
```bash
./gradlew clean
./gradlew build
```

O en PowerShell:
```powershell
.\gradlew clean
.\gradlew build
```

### Paso 6: Probar la App

1. Ejecuta la app en un dispositivo o emulador
2. Intenta iniciar sesión con Google
3. Revisa los logs en Logcat buscando "LoginActivity" para ver mensajes de error específicos

---

## 🔍 Verificación Rápida

- [ ] SHA-1 agregado en Firebase Console: `86:E9:B7:30:46:9F:11:40:76:F8:95:05:19:E1:37:78:B3:A6:5D:01`
- [ ] `google-services.json` descargado y actualizado después de agregar SHA-1
- [ ] Google Sign-In habilitado en Firebase Console → Authentication → Sign-in method
- [ ] Web Client ID correcto en `strings.xml` (debe ser el de tipo "Web application", NO Android)
- [ ] Proyecto limpiado y reconstruido

---

## 🐛 Códigos de Error Comunes

- **10**: Error de desarrollo → Verifica SHA-1 y Web Client ID
- **12501**: Usuario canceló el inicio de sesión
- **7**: Error de conexión → Verifica tu internet
- **8**: Error interno de Google → Intenta de nuevo

---

## 📞 Si Aún No Funciona

1. Revisa los logs en Logcat (filtra por "LoginActivity")
2. Verifica que todos los pasos anteriores estén completados
3. Asegúrate de que el Web Client ID en `strings.xml` sea el correcto (debe ser diferente al Android Client ID)
4. Espera unos minutos después de agregar el SHA-1 (puede tardar en propagarse)

