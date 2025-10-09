package com.phoenixcorp.founderfinder.data

data class Conversation(
    val recipientId: String = "",
    val recipientName: String = "",
    val lastMessage: String? = null,
    val timestamp: Long? = null,
    val profilePicture: String? = null
)