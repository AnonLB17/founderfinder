package com.phoenixcorp.founderfinder.domain.model

data class Forum(
    val id: String,
    val title: String,
    val description: String,
    val creatorId: String,
    val creatorName: String,
    val timestamp: Long,
    val imageUrl: String?,
    val likes: Int,
    val isFavorited: Boolean,
    val hasLiked: Boolean,
    val category: String,
    val location: String? = null // Add this field
)