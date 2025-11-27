package com.onfu.app.domain.models

data class Post(
    val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = ""
)
