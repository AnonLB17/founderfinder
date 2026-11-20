package com.phoenixcorp.founderfinder.domain.model

data class Forum(
    val id: String = "",

    // Title fields (support legacy data)
    val title: String = "",
    val name: String = "",                    // Legacy field

    val description: String = "",
    val about: String = "",                   // Alternative description field

    val creatorId: String = "",
    val creatorName: String = "",

    val timestamp: Long = 0L,
    val imageUrl: String? = null,

    val likes: Int = 0,
    val isFavorited: Boolean = false,
    val hasLiked: Boolean = false,

    // Important: Category of the forum (e.g. "requestedsolutions", "marketpotential", etc.)
    val category: String = "",

    val location: String? = null,

    // Additional useful fields
    val school: String? = null,
    val tags: List<String> = emptyList(),
    val memberCount: Int = 0,
    val isActive: Boolean = true
)