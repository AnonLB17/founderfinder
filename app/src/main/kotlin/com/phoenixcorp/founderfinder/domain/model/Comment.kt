package com.phoenixcorp.founderfinder.domain.model

data class Comment(
    val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val creatorProfilePicture: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val parentId: String? = null,
    val depth: Int = 0,
    val isFavorited: Boolean? = false,
    val likes: Long? = 0
)