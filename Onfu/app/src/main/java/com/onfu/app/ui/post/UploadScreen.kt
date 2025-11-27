package com.onfu.app.ui.post

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onfu.app.ui.components.CustomButton
import com.onfu.app.ui.components.CustomTextField

@Composable
fun UploadScreen() {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        CustomTextField(value = title, label = "Título") { title = it }
        Spacer(modifier = Modifier.height(8.dp))
        CustomTextField(value = description, label = "Descripción") { description = it }
        Spacer(modifier = Modifier.height(8.dp))
        CustomButton(text = "Subir Imagen") {
            // TODO: Abrir selector de imagen
        }
        Spacer(modifier = Modifier.height(16.dp))
        CustomButton(text = "Subir Post") {
            // TODO: Subir post a Firestore + Storage
        }
    }
}
