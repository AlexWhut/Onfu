package com.onfu.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun FeedScreen() {
    val posts = listOf("Post 1", "Post 2", "Post 3")
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(posts) { post ->
            Card(modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)) {
                Text(post, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FeedScreenPreview() {
    FeedScreen()
}