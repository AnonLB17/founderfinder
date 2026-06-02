package com.phoenixcorp.founderfinder.domain.model

data class Message(
    val id: String = "",
    val senderId: String = "",
    val recipientId: String? = null,
    val content: String = "",
    val timestamp: Long = 0L,
    val type: String = "text",
    val orgId: String? = null
)