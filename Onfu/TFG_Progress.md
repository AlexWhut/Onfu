# TFG Progress - Onfu App

## Project Overview
Onfu is a mobile app MVP built with Android (Kotlin + Jetpack Compose) and Firebase.
The app will allow users to create profiles, upload products (as posts), view feeds, and eventually enable transactions.

The purpose of this document is to log progress, describe completed tasks, and justify the hours dedicated to the project.

---

## Phase 1 - Initial Setup (Preparation)

**Objective:** Prepare the environment and connect the project to Firebase.

**Tasks Completed:**
- Installed and updated Android Studio.
- Created a new Android project:
    - Kotlin
    - Jetpack Compose
    - Minimum API 23
- Created a Firebase project named "Onfu"
- Integrated Firebase services:
    - Firebase Authentication
    - Firestore
    - Firebase Storage
    - Cloud Messaging
- Ensured the project builds successfully (BUILD SUCCESSFUL)
- Secured API keys (`google-services.json`) from GitHub by updating `.gitignore` and removing cached files.

**Hours Justification:** ~8-10 hours
- Android Studio setup: 2 hours
- Firebase project creation and integration: 4-5 hours
- Gradle sync, configuration, and troubleshooting: 2-3 hours

---

## Phase 2 - Base Structure and Security Rules

**Objective:** Build the basic structure of the app, set up navigation, create placeholder screens, and configure simplified Firebase security rules.

**Tasks Completed:**
- Defined project package structure (ui, data, domain, core)
- Set up Jetpack Compose navigation with `AppNavHost.kt` and `Routes.kt`
- Created placeholder screens for:
    - Login
    - Register
    - Home (feed)
    - Profile
    - Upload Post
- Implemented reusable UI components (CustomButton, CustomTextField)
- Defined data models (`User`, `Post`)
- Configured Firestore security rules:
    - Users can only edit their data
    - Users can only upload their own posts
    - Verification documents only visible by admins
- Project builds successfully and navigation works with placeholder screens.

**Hours Justification:** ~12-15 hours
- Designing package structure: 2 hours
- Implementing navigation: 3 hours
- Creating placeholder screens and components: 5 hours
- Writing security rules and testing Firestore access: 2-3 hours
- Debugging and syncing Firebase: 1-2 hours
