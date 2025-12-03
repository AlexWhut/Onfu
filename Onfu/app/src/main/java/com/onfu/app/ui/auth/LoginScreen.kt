package com.onfu.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onfu.app.ui.components.CustomButton
import com.onfu.app.ui.components.CustomTextField
import com.onfu.app.data.auth.AuthRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LoginScreen(
    authRepository: AuthRepository,
    onNavigateHome: () -> Unit,
    onNavigatePreHome: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val firestore = FirebaseFirestore.getInstance()

    // Función para chequear si el perfil existe en Firestore
    fun checkProfile(uid: String) {
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) onNavigateHome()
                else onNavigatePreHome()
            }
            .addOnFailureListener { e ->
                errorMessage = e.message
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        CustomTextField(
            value = email,
            label = "Email",
            onValueChange = { email = it }
        )
        Spacer(modifier = Modifier.height(8.dp))
        CustomTextField(
            value = password,
            label = "Password",
            isPassword = true,
            onValueChange = { password = it }
        )
        Spacer(modifier = Modifier.height(16.dp))

        CustomButton(text = "Login") {
            scope.launch {
                try {
                    val user = authRepository.login(email, password)
                    if (user != null) {
                        checkProfile(user.uid)
                    } else {
                        errorMessage = "Usuario o contraseña incorrectos"
                    }
                } catch (e: Exception) {
                    errorMessage = e.message
                }
            }
        }

        if (!errorMessage.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onNavigateToRegister) {
            Text("¿No tienes cuenta? Regístrate")
        }
    }
}
