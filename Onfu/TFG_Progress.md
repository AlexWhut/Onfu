
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
