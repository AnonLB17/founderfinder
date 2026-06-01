package com.phoenixcorp.founderfinder.domain.model

data class ChatMessage(
    val id: String,
    val chatId: String,
    val senderId: String,
    val receiverId: String,
    val text: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val imageUrl: String? = null
)