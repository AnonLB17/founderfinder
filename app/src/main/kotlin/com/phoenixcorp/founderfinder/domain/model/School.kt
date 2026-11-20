package com.phoenixcorp.founderfinder.domain.model

data class School(
    val id: String,
    val name: String,
    val shortName: String?,
    val location: String?,
    val logoUrl: String? = null
)