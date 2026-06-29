package com.phoenixcorp.founderfinder.domain.model

import android.text.format.DateUtils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Notification(
    val id: String = "",
    val userId: String = "",           // recipient
    val recipientId: String? = null,
    val senderId: String = "",

    // Sender info from UserProfile
    val senderName: String = "",       // Will be populated as "First Last"
    val senderFirstName: String? = null,
    val senderLastName: String? = null,

    val type: String = "",
    val title: String = "",
    val body: String = "",
    val content: String? = null,

    val screen: String? = null,
    val category: String? = null,

    val chatId: String? = null,
    val messageId: String? = null,
    val forumId: String? = null,
    val threadId: String? = null,
    val commentId: String? = null,

    @ServerTimestamp
    val timestamp: Timestamp? = null,

    val read: Boolean = false,
    val imageUrl: String? = null
) {
    // Helper for UI
    val displaySenderName: String
        get() {
            val fullName = listOfNotNull(senderFirstName, senderLastName)
                .joinToString(" ")
                .trim()
            return if (fullName.isNotBlank()) fullName else senderName.ifBlank { "Unknown User" }
        }

    val timestampMillis: Long
        get() = timestamp?.toDate()?.time ?: System.currentTimeMillis()

    fun getRelativeTime(): String {
        return DateUtils.getRelativeTimeSpanString(
            timestampMillis,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
    }
}