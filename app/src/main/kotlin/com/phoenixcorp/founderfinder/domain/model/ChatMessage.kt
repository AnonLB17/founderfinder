package com.phoenixcorp.founderfinder.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class ChatMessage(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String? = null,
    val recipientId: String? = null,           // ← Added for notifications
    val text: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false,
    val imageUrl: String? = null,
    val fileUrl: String? = null,
    val fileName: String? = null,
    val type: String = "text",                 // text, image, file, etc.

    val orgId: String? = null,   // ← Added for organization messages

    @ServerTimestamp
    val serverTimestamp: Timestamp? = null
)