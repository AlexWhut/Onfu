package com.onfu.app.domain.models

data class Post(
    val ownerId: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    // Number of likes (kept in sync by client or cloud function). Optional in Firestore documents.
    val likesCount: Long = 0,
    val id: String = ""
)