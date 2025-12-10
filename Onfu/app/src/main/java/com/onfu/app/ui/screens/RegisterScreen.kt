package com.onfu.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun RegisterScreen(onRegisterSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var usernameError by remember { mutableStateOf<String?>(null) }

    // Username policy: only lowercase letters, digits and underscore; max 10 chars.
    val usernameRegex = Regex("^[a-z0-9_]{1,10}$")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Register", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        BasicTextField(value = username, onValueChange = {
            // normalize to lowercase and trim
            username = it.trim().lowercase()
            usernameError = when {
                username.isEmpty() -> null
                !usernameRegex.matches(username) -> "Solo letras minúsculas, números y _ (max 10)"
                else -> null
            }
        }, modifier = Modifier.fillMaxWidth())
        if (usernameError != null) {
            Text(usernameError ?: "", color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(8.dp))
        BasicTextField(value = email, onValueChange = { email = it }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        BasicTextField(value = password, onValueChange = { password = it }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        val canRegister = usernameError == null && username.isNotEmpty()
        Button(onClick = { if (canRegister) onRegisterSuccess() }, enabled = canRegister, modifier = Modifier.fillMaxWidth()) {
            Text("Register")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    RegisterScreen(onRegisterSuccess = {})
}