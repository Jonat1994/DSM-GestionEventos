# Desplegar Reglas de Firestore

Las reglas de Firestore han sido actualizadas para permitir que los usuarios lean asistencias (necesario para ver el conteo).

## Desplegar Reglas

```bash
firebase deploy --only firestore:rules
```

O desde Firebase Console:
1. Ve a Firebase Console → Firestore Database
2. Ve a la pestaña "Rules"
3. Copia el contenido de `firestore.rules`
4. Pega y guarda

## Verificar que las Reglas se Desplegaron

1. Ve a Firebase Console → Firestore Database → Rules
2. Verifica que la regla para `attendances` incluya:
   ```
   allow read: if request.auth != null;
   ```

## Importante

Después de desplegar las reglas, los usuarios podrán:
- ✅ Ver el conteo de asistencias de cualquier evento
- ✅ Confirmar/cancelar su propia asistencia
- ✅ El listener en tiempo real funcionará correctamente

