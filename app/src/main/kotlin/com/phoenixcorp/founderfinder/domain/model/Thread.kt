package com.phoenixcorp.founderfinder.domain.model

data class Thread(
    val id: String = "",
    val forumId: String = "",           // ← Critical for navigation
    val category: String = "",
    val creatorId: String = "",
    val creatorName: String = "Anonymous",
    val creatorProfilePicture: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val likes: Long = 0,
    val isFavorited: Boolean = false,
    val institutionName: String = "",
    val imageUrl: String? = null,
    val location: String? = null,
    val topicHeader: String? = null
)