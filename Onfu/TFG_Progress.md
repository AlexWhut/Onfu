
# Progreso TFG - App Onfu

## Resumen del proyecto
Onfu es un MVP móvil desarrollado para Android (Kotlin + Jetpack Compose) y utilizando Firebase como backend. La aplicación permite a los usuarios crear perfiles, publicar productos (como posts), ver un feed y, en fases posteriores, habilitar transacciones.

Este documento registra el progreso del proyecto, describe las tareas completadas y justifica las horas dedicadas en cada fase.

---

## Fase 1 — Preparación e integración inicial

**Objetivo:** Preparar el entorno de desarrollo y conectar el proyecto con Firebase.

**Tareas completadas:**
- Instalación y actualización de Android Studio.
- Creación del proyecto Android con:
    - Kotlin
    - Jetpack Compose
    - API mínima 23
- Creación del proyecto en Firebase llamado "Onfu".
- Integración de servicios Firebase:
    - Firebase Authentication
    - Firestore
    - Firebase Storage
    - Cloud Messaging
- Verificación de construcción exitosa (BUILD SUCCESSFUL).
- Protección de credenciales (`google-services.json`) en `.gitignore` y eliminación de caches comprometidos.

**Justificación de horas:** ~8-10 horas
- Configuración de Android Studio: 2 horas
- Creación e integración en Firebase: 4-5 horas
- Sincronización y resolución de problemas de Gradle: 2-3 horas

---

## Fase 2 — Estructura base y reglas de seguridad

**Objetivo:** Construir la estructura base de la app, configurar la navegación y reglas básicas de seguridad en Firestore.

**Tareas completadas:**
- Definición de la estructura de paquetes: `ui`, `data`, `domain`, `core`.
- Configuración de la navegación con Jetpack Compose en `AppNavHost.kt` y `Routes.kt`.
- Creación de pantallas placeholder:
    - Login
    - Registro
    - Inicio (feed)
    - Perfil
    - Subir publicación
- Componentes UI reutilizables (ej.: `CustomButton`, `CustomTextField`).
- Modelos de datos definidos (`User`, `Post`).
- Reglas de Firestore simplificadas:
    - Los usuarios sólo pueden editar su propio documento de perfil.
    - Las publicaciones sólo pueden ser creadas/editarse por su autor.
    - Documentos de verificación visibles únicamente para administradores.
- Construcción y navegación verificada con pantallas placeholder.

**Justificación de horas:** ~12-15 horas
- Diseño de estructura de paquetes: 2 horas
- Implementación de navegación: 3 horas
- Creación de pantallas y componentes: 5 horas
- Escritor y prueba de reglas de Firestore: 2-3 horas
- Depuración y sincronización con Firebase: 1-2 horas

---

## Fase 3 — Implementación de autenticación y lógica de usuario

**Objetivo:** Implementar la lógica completa de login y registro, persistencia de sesión y consolidar integraciones con Firebase (Auth, Firestore, Storage).

**Cambios principales realizados (resumen por archivos):**
- `app/src/main/java/.../auth/`: Añadidos `AuthRepository.kt` y `AuthViewModel.kt` para encapsular llamadas a Firebase Authentication y exponer estados a la UI.
- `app/src/main/java/.../ui/auth/`: Implementadas pantallas `LoginScreen.kt` y `RegisterScreen.kt` con validaciones, manejo de errores y navegación hacia `Home`.
- `app/src/main/java/.../data/`: Añadidos servicios `FirebaseAuthService.kt` y `UserService.kt` para abstraer FirebaseAuth, Firestore y Storage.
- `app/src/main/java/.../models/`: Actualizados `User.kt` y `Post.kt` con campos adicionales (`avatarUrl`, `createdAt`, `uid`).
- `AppNavHost.kt` / `Routes.kt`: Integración de rutas seguras y flujo condicional según estado de autenticación.
- `build.gradle.kts` (módulo `app`): Añadidas dependencias de Firebase Auth, Firestore y Storage; configurado `google-services`.
- Reglas de Firestore: Adaptadas para soportar creación de perfil al registrarse y control de acceso por `uid`.

**Detalles técnicos — qué se implementó:**
- **Registro de usuario:**
    - Uso de `FirebaseAuth.createUserWithEmailAndPassword(email, password)` para crear credenciales.
    - Al registrarse, se crea un documento en Firestore `users/{uid}` con el perfil inicial (nombre, email, `avatarUrl` vacío, `createdAt`).
    - Si el usuario sube un avatar, se almacena en Firebase Storage y se actualiza `avatarUrl` en Firestore.
- **Inicio de sesión (Login):**
    - Uso de `FirebaseAuth.signInWithEmailAndPassword(email, password)`.
    - Tras autenticación, se carga el documento de perfil desde Firestore y se expone mediante `AuthViewModel`.
- **Persistencia de sesión:**
    - Basada en `FirebaseAuth.getCurrentUser()` y `AuthStateListener` para mantener sesión entre reinicios de la app.
    - La navegación reacciona al estado de autenticación para redirigir a `Home` o a pantallas de autenticación.
- **Validaciones y experiencia de usuario (UX):**
    - Validaciones en cliente (formato de email, contraseña mínima).
    - Indicadores de carga y mensajes claros en errores (snackbars/toasts).
- **Recuperación de contraseña:**
    - Implementada con `FirebaseAuth.sendPasswordResetEmail(email)` y feedback al usuario.
- **Manejo de errores:**
    - Mapeo de códigos de error de Firebase a mensajes comprensibles (usuario no encontrado, contraseña incorrecta, email en uso, error de red).
- **Seguridad:**
    - Reglas de Firestore que limitan lectura/escritura del documento `users/{uid}` sólo al propietario.
    - Reglas que permiten creación/edición de posts sólo por su autor.

**Impacto y beneficios:**
- Usuarios pueden registrarse, iniciar sesión, persistir sesión y recuperar contraseña en la app.
- Separación de responsabilidades entre UI, ViewModel y repositorios/servicios, facilitando pruebas y mantenimiento.
- Base preparada para funciones avanzadas: edición de perfil, verificación por email y control por roles.

**Siguientes pasos recomendados:**
- Añadir tests unitarios para `AuthRepository` y pruebas de UI para flujos de autenticación.
- Mejorar la experiencia offline y la reanudación de subidas a Storage.
- Revisar y endurecer reglas de Firestore antes de despliegue a producción.

---

## Resumen de horas estimadas
- Fase 1: 8-10 horas
- Fase 2: 12-15 horas
- Fase 3: 20-30 horas (implementación de autenticación y pruebas iniciales)

---

## Anexos y notas técnicas
- Archivo `google-services.json` se mantiene fuera del control de versiones y sólo en entornos locales.
- Para ejecutar la app localmente: abrir en Android Studio y sincronizar gradle.
- Si se desea, puedo crear una rama con estos cambios y preparar un commit con mensaje sugerido.

--- 

## Phase 3 - Implementación de Autenticación y Lógica de Usuario

**Objetivo:** Implementar la lógica completa de login y registro, persistencia de sesión, y consolidar las integraciones con Firebase (Auth, Firestore y Storage). 

**Cambios principales realizados (resumen por archivos):**
- **`app/src/main/java/.../auth/`**: Añadidos `AuthRepository.kt` y `AuthViewModel.kt` para encapsular llamadas a Firebase Authentication y exponer estados a la UI.
- **`app/src/main/java/.../ui/auth/`**: Implementadas pantallas `LoginScreen.kt` y `RegisterScreen.kt` con validaciones, manejo de errores y navegación hacia `Home`.
- **`app/src/main/java/.../data/`**: Añadidos servicios `FirebaseAuthService.kt` y `UserService.kt` para abstracción de FirebaseAuth, Firestore y Storage.
- **`app/src/main/java/.../models/`**: Actualizados `User.kt` y `Post.kt` para incluir campos necesarios (ej. `avatarUrl`, `createdAt`, `uid`).
- **`AppNavHost.kt` / `Routes.kt`**: Integración de rutas seguras y flujo condicional según estado de autenticación.
- **`build.gradle.kts` (módulo `app`)**: Añadidas dependencias de Firebase Auth, Firestore y Storage; configurado `google-services`.
- **Reglas de Firestore**: Adaptadas para soportar creación de perfil al registrarse y control de acceso por `uid`.

**Qué logramos con la lógica de Login y Registro (detalles técnicos):**
- **Registro de usuario:** Se crea el usuario en Firebase Authentication usando correo y contraseña. Tras registro exitoso, se crea un documento en Firestore `users/{uid}` con el perfil inicial (nombre, email, `avatarUrl` vacío, `createdAt`). Si el usuario sube un avatar, se guarda en Firebase Storage y se actualiza `avatarUrl` en el documento.
- **Login:** El flujo de login usa `FirebaseAuth.signInWithEmailAndPassword`. Al iniciar sesión correctamente, se carga el documento de perfil desde Firestore y se publica en `AuthViewModel` para que la UI lo consuma.
- **Persistencia de sesión:** Se utiliza el estado de `FirebaseAuth.getCurrentUser()` y listeners (`AuthStateListener`) para mantener sesión entre reinicios de la app; la navegación se actualiza automáticamente según el usuario autenticado.
- **Validaciones y UX:** Validaciones en cliente (email formato, contraseña mínima), mensajes de error traducidos a textos legibles, indicadores de carga y toasts/snackbars para feedback.
- **Recuperación de contraseña:** Implementada vía `FirebaseAuth.sendPasswordResetEmail(email)` con feedback de éxito/error.
- **Manejo de errores:** Mapeo de errores de Firebase a UI (usuario no encontrado, contraseña incorrecta, email ya en uso, network).
- **Seguridad:** Firestore rules restringen escritura/lectura de documentos de usuario al `uid` correspondiente; posts sólo pueden crearse/editars por su autor.

**Impacto y beneficios:**
- **Experiencia de usuario funcional:** Usuarios pueden registrarse, iniciar sesión, persistir sesión y recuperar contraseña sin depender de datos de placeholder.
- **Separación de responsabilidades:** Lógica de autenticación y acceso a datos separada en repositorios/servicios, facilitando testing y mantenimiento.
- **Preparación para features futuras:** Con perfil persistente y subida de avatares ya implementada, es sencillo añadir edición de perfil, verificación por correo, y autorización por roles.

**Siguientes pasos recomendados:**
- **Pruebas:** Añadir tests unitarios para `AuthRepository` y pruebas de UI para flujos de login/registro.
- **Mejoras UX:** Añadir manejo de estados offline y reintentos en subidas a Storage.
- **Seguridad:** Revisar y endurecer reglas de Firestore según necesidades de producción.

--- 

Horas dedicadas estimadas a Phase 3: ~20-30 horas

---

## Fase 4 — Integraciones de Storage y estabilización de entorno (01‑Dic‑2025)

**Objetivo:** Habilitar subida de foto de perfil a Firebase Storage, asegurar lectura en la app y estabilizar el entorno de build y despliegue.

**Tareas y avances del día:**
- Subida de avatar: Ajustamos el flujo en `UploadFragment` para usar el bucket configurado por Firebase (`FirebaseApp.options.storageBucket`) y rutas `avatars/{uid}/...`.
- Reglas de Storage: Publicamos reglas que permiten escritura sólo al propietario y lectura (pública o autenticada según necesidad) en `avatars/{userId}/{**}`.
- Placeholder: Confirmamos uso de `placeholder/error` gris para cuando `photoUrl` sea nulo.
- Validación en consola: Verificamos la creación de la carpeta `avatars/` y la presencia de archivos en el bucket correcto.

**Problemas encontrados y cómo se resolvieron:**
- Error de build con Java 25 (`java version "25.0.1"`):
    - Síntoma: Gradle/Kotlin DSL fallaba con `IllegalArgumentException: 25.0.1`.
    - Causa: El sistema usaba JDK 25; el tooling del proyecto requiere JDK 21.
    - Solución: Configuramos `JAVA_HOME` a `C:\Program Files\Java\jdk-21` y añadimos `org.gradle.java.home` en `gradle.properties`. El build pasó (BUILD SUCCESSFUL).
- Fallo al subir imagen (404 "Object does not exist at location"):
    - Síntoma: `StorageException` 404 al iniciar upload resumible.
    - Causas detectadas: Bucket incorrecto y posibles llamadas al Storage Emulator sin tenerlo activo.
    - Solución: Unificamos el bucket a `gs://onfu-fe8fd.firebasestorage.app` (consola Firebase), eliminamos el override temporal y desactivamos el uso de `useEmulator(...)` en entornos sin emulator. Tras ello, la subida funcionó y `avatars/{uid}` apareció en el bucket.
- Reglas de Storage denegando por defecto:
    - Síntoma: Subidas denegadas si las reglas estaban en `allow read, write: if false` global.
    - Solución: Reglas:
        ```
        rules_version = '2';
        service firebase.storage {
            match /b/{bucket}/o {
                match /avatars/{userId}/{allPaths=**} {
                    allow read: if true; // o if request.auth != null
                    allow write: if request.auth != null && request.auth.uid == userId;
                }
                match /{allPaths=**} { allow read, write: if false; }
            }
        }
        ```
- Problemas con push a GitHub por archivos >100MB:
    - Síntoma: Rechazo del push (`GH001`) por incluir `app/jdk-21_windows-x64_bin.exe` y `GoogleCloudSDKInstaller.exe`.
    - Solución: Quitamos esos binarios del índice, agregamos `.gitignore` (excluir `*.exe`, `*.zip`, `build/`, etc.) y realizamos `git push --force` tras limpiar el historial. El remoto quedó sincronizado.

**Validaciones realizadas:**
- Logcat `STORAGE_DEBUG` mostrando bucket configurado y rutas de upload correctas.
- Visualización de `avatars/{uid}` en Firebase Console → Storage.
- Actualización de `users/{uid}.photoUrl` en Firestore y carga de avatar en la UI.

**Justificación de horas (día):** ~6-8 horas
- Diagnóstico y fijación JDK/Gradle: 1.5-2 horas
- Diagnóstico Storage (bucket, reglas, emulator) y correcciones: 3-4 horas
- Limpieza repo y push: 1-2 horas
- Verificaciones y documentación: 0.5-1 hora

**Siguientes pasos:**
- Crear drawable `avatar_placeholder` gris consistente y revisar su uso en todas las vistas.
- Añadir prueba mínima `putBytes` para diagnóstico rápido en Storage.
- Endurecer reglas de lectura (si se requiere autenticación) y documentar decisión.

---

## Fase 5 — Upload funcional, feed sin texto y bio de perfil (02‑Dic‑2025)

**Objetivo:** Hacer funcional la pestaña de subida de posts (imágenes), mostrar un feed únicamente con imágenes en cuadrícula, y mover la descripción al detalle del post. Añadir bio editable bajo `@username` y eliminar el uso del emulador de Storage.

**Cambios principales realizados:**
- Upload de posts:
    - Implementado `UploadFragment` (nuevo en `ui/upload`) para seleccionar imagen, previsualizar y subir a Storage con redimensionado a 1360 px de ancho y altura máxima 1080 (JPEG calidad 85).
    - `PostRepository.uploadPost(...)` guarda la imagen en `posts/{ownerId}/{timestamp}.jpg`, obtiene la URL pública y persiste el documento en Firestore (incluye `id` del documento).
    - Eliminado el uso del emulador de Storage y cualquier configuración de cleartext; se usa el bucket HTTPS real `gs://onfu-fe8fd.firebasestorage.app`.
- Feed en cuadrícula (3 columnas):
    - `FeedFragment` muestra sólo imágenes (sin texto/captions) en un `RecyclerView` con `GridLayoutManager(3)`.
    - `item_post.xml` ajustado a relación de aspecto 1080:1360 (ancho:alto) para optimizar visualización en la cuadrícula.
    - Al tocar un item, se abre `PostDetailFragment` que muestra la imagen completa y la descripción del post (texto sólo en el detalle).
- Perfil y bio:
    - Bajo `@username` se muestra `profile_bio` por defecto con el texto “no bio”.
    - Bio editable al tocar, con límite de 50 palabras. Se persiste en `users/{uid}` y se refleja mediante listeners.
- Dependencias y estabilidad:
    - Añadidas dependencias faltantes: `androidx.recyclerview:recyclerview:1.3.2` y `androidx.constraintlayout:constraintlayout:2.1.4`.
    - Corregidos listeners de Firestore usando la sobrecarga con `Activity` y guardas de ciclo de vida para evitar NPE tras destruir la vista.
    - Ajustado `HomeFragment` para abrir el nuevo `UploadFragment` (posts) desde `nav_add`.

**Reglas de Firebase Storage (actualizadas):**
```
rules_version = '2';
service firebase.storage {
    match /b/{bucket}/o {
        match /avatars/{userId}/{allPaths=**} {
            allow read: if true; // o if request.auth != null
            allow write: if request.auth != null && request.auth.uid == userId;
        }
        match /posts/{userId}/{allPaths=**} {
            allow read: if true; // o if request.auth != null
            allow write: if request.auth != null && request.auth.uid == userId;
        }
        match /{allPaths=**} { allow read, write: if false; }
    }
}
```

**Problemas y cómo se resolvieron:**
- 403 Permission denied al subir posts: Se corrigió la ruta de subida a `posts/{uid}/...` para alinearse con las reglas; tras ello, las subidas funcionaron.
- Importaciones en rojo de RecyclerView/ConstraintLayout: Se añadieron las dependencias correspondientes en Gradle.
- `addSnapshotListener` firma incorrecta y NPE post-destrucción de vista: Se usó la sobrecarga con `Activity` y se añadieron guardas de ciclo de vida (`_binding == null`).
- Advertencia “No adapter attached; skipping layout”: Se inicializa el adapter tras recibir datos de Firestore.
- Placeholder de Coil: Se eliminaron placeholders para que se cargue directamente la foto real (manteniendo `CircleCrop` sólo para avatar).

**Validaciones realizadas:**
- Subida de posts confirma URL obtenida y documento creado en Firestore con `id` persistido.
- Feed muestra 3 columnas de imágenes con relación 1080:1360; al tocar abre detalle con descripción.
- Bio muestra “no bio” por defecto y se actualiza correctamente al editar.

**Justificación de horas (día):** ~6-8 horas
- Implementación Upload + redimensionado + repositorio: 3-4 horas
- Feed imágenes-only + detalle de post: 2 horas
- Bio editable + reglas + estabilización listeners: 1-2 horas

**Siguientes pasos recomendados:**
- Añadir contador de palabras en el diálogo de edición de bio y desactivar “Guardar” si supera 50.
- Optimizar carga de cuadrícula con `Coil.size(1080, 1360)` para ahorrar memoria.
- Definir si el feed será global o filtrado por usuarios seguidos.
