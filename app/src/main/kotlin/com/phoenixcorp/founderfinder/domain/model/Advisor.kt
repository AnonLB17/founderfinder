package com.phoenixcorp.founderfinder.domain.model

data class Advisor(
    val user: User,
    val expertise: List<String> = emptyList(),
    val experienceYears: Int = 0,
    val rating: Float = 0f,
    val reviewCount: Int = 0,
    val availability: Boolean = true,
    val hourlyRate: Int? = null,
    val linkedinUrl: String? = null,
    val company: String? = null,
    val title: String? = null
)