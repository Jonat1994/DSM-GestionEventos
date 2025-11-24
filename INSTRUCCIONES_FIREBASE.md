# 🔥 INSTRUCCIONES PARA CONFIGURAR FIREBASE FIRESTORE

## El problema más común: Reglas de Firestore

Si el historial no muestra datos, probablemente las **reglas de Firestore** están bloqueando la escritura/lectura.

## ✅ Cómo verificar y arreglar:

### Paso 1: Ir a Firebase Console
1. Ve a https://console.firebase.google.com/
2. Selecciona tu proyecto
3. En el menú lateral, haz clic en **Firestore Database**
4. Ve a la pestaña **Reglas** (Rules)

### Paso 2: Verificar las reglas actuales
Deberías ver algo como esto:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if false;  // ❌ ESTO ESTÁ BLOQUEANDO TODO
    }
  }
}
```

### Paso 3: Cambiar las reglas
Reemplaza las reglas con esto (para desarrollo):

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Permitir lectura/escritura solo a usuarios autenticados
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

O esta versión MÁS SEGURA (recomendado):

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Colección de usuarios
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Colección de gastos
    match /expenses/{expenseId} {
      allow read: if request.auth != null && resource.data.userId == request.auth.uid;
      allow create: if request.auth != null && request.resource.data.userId == request.auth.uid;
      allow update, delete: if request.auth != null && resource.data.userId == request.auth.uid;
    }
    
    // Colección de historial
    match /history/{historyId} {
      allow read: if request.auth != null && resource.data.userId == request.auth.uid;
      allow create: if request.auth != null && request.resource.data.userId == request.auth.uid;
      allow delete: if request.auth != null && resource.data.userId == request.auth.uid;
    }
  }
}
```

### Paso 4: Publicar las reglas
1. Haz clic en **Publicar** (Publish)
2. Espera unos segundos para que se apliquen

## 🧪 Probar el historial

Después de cambiar las reglas:

1. **Cierra y vuelve a abrir la app** (o reinicia)
2. Ve a la pantalla de **📜 Historial Completo**
3. Presiona el botón **🧪 AGREGAR ENTRADA DE PRUEBA**
4. Deberías ver:
   - Un Toast que dice: "✅ Entrada agregada! ID: xxxxx"
   - El contador de entradas debe cambiar de 0 a 1
   - La entrada debe aparecer en la lista

## 📊 Información de Debug

En la pantalla de historial verás:
```
Usuario ID: xxxxxxxxxxxxx
Entradas recibidas: X
```

Si dice:
- **"Entradas recibidas: 0"** → Las reglas de Firestore pueden estar bloqueando o no hay datos
- **"ERROR: Usuario no autenticado"** → Problema con Firebase Auth
- **"Entradas recibidas: 1"** o más → ¡Funciona correctamente!

## 🔍 Ver datos en Firebase

1. Ve a Firestore Database
2. Busca la colección **history**
3. Deberías ver los documentos guardados allí

Si NO ves la colección "history", significa que:
- Las reglas están bloqueando la escritura
- Hay un error en el código (revisa Logcat)

## 📱 Ver Logs (Opcional)

En Android Studio:
1. Abre **Logcat**
2. Filtra por: `FirestoreUtil` o `HistoryActivity`
3. Busca mensajes como:
   - ✅ Historial guardado exitosamente
   - ❌ Error al guardar historial
   - 📥 Snapshot recibido, documentos: X

## ⚠️ Nota Importante

Las reglas de Firestore que permiten `allow read, write: if true;` son **PELIGROSAS** y solo deben usarse para pruebas. Usa siempre reglas que verifiquen autenticación.

