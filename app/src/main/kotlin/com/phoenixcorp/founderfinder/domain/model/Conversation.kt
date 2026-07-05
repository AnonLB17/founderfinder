package com.phoenixcorp.founderfinder.domain.model

data class Conversation(
    val conversationId: String = "",
    val participants: List<String> = emptyList(),
    val otherUserId: String = "",           // For easy access in UI
    val otherUserName: String = "",
    val otherUserProfilePicture: String? = null,
    val lastMessage: String? = null,
    val lastMessageAt: Long? = null,
    val lastMessageSenderId: String? = null,
    val unreadCount: Int = 0,               // Optional: per-user unread
    val createdAt: Long = 0L
)