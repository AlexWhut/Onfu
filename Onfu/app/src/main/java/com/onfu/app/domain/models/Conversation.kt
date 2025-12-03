package com.onfu.app.domain.models

data class Conversation(
    val id: String = "",
    val participants: List<String> = emptyList(), // uids
    val lastMessageText: String = "",
    val lastMessageSenderId: String = "",
    val lastMessageAt: Long = 0L
)
