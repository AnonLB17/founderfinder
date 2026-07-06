package com.phoenixcorp.founderfinder.domain.model

data class Thread(
    val id: String = "",
    val forumId: String = "",           // ← Critical for navigation
    val category: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val creatorProfilePicture: String = "",
    val message: String = "",
    val timestamp: Long = 0L,

    // Like system
    val likes: Long = 0,
    val likedBy: List<String> = emptyList(),   // ← NEW: For per-user like tracking

    // Favorite system
    val isFavorited: Boolean = false,          // ← Keep for UI

    val institutionName: String = "",
    val imageUrl: String? = null,
    val location: String? = null,
    val topicHeader: String? = null
)