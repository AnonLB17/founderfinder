package com.phoenixcorp.founderfinder.domain.model

data class Incubator(
    val incubatorId: String = "",
    val name: String = "",
    val websiteUrl: String = "",
    val location: String = "",
    val imageUri: String? = null,
    val creatorId: String = "",
    val createdAt: Long = 0L
)