package com.onfu.app.domain.models

data class Post(
    val ownerId: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val id: String = ""
)