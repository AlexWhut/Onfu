package com.onfu.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onfu.app.ui.components.CustomButton
import com.onfu.app.ui.components.CustomTextField

@Composable
fun RegisterScreen(
    onRegisterCompleted: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        CustomTextField(value = email, label = "Email") { email = it }
        Spacer(modifier = Modifier.height(8.dp))
        CustomTextField(value = password, label = "Password", isPassword = true) { password = it }
        Spacer(modifier = Modifier.height(16.dp))
        CustomButton(text = "Registrarse") {
            // TODO: Lógica Firebase Auth
            onRegisterCompleted()
        }
    }
}
