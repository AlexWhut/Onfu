package com.onfu.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onfu.app.domain.models.Post

@Composable
fun HomeScreen() {
    var posts by remember { mutableStateOf(listOf<Post>()) }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(posts) { post ->
                PostCard(post)
            }
        }
    }
}
