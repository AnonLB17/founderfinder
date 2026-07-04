package com.phoenixcorp.founderfinder.domain.model

import android.text.format.DateUtils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import com.phoenixcorp.founderfinder.utils.TimeZoneUtils
import java.util.*

data class Notification(
    val id: String = "",
    val userId: String = "",
    val recipientId: String? = null,
    val senderId: String = "",

    val senderName: String = "",
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
    val activityId: String? = null,
    val eventTime: Long? = null,           // UTC millis
    val activityType: String? = null,
    val organizationId: String? = null,
    val organizationName: String? = null,

    @ServerTimestamp
    val timestamp: Timestamp? = null,

    val read: Boolean = false,
    val imageUrl: String? = null
) {
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
        return TimeZoneUtils.getRelativeTime(timestampMillis)
    }

    // Time until event (in user's local time zone)
    fun getTimeUntilEvent(): String {
        return TimeZoneUtils.getTimeUntilEvent(eventTime ?: 0L)
    }

    // Local formatted time for display
    fun getLocalEventTime(pattern: String = "MMM dd, yyyy - hh:mm a"): String {
        return TimeZoneUtils.formatLocalTime(eventTime ?: 0L, pattern)
    }
}