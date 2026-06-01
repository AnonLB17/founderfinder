package com.phoenixcorp.founderfinder.domain.model

data class Profile(
    val user: User,
    val headline: String? = null,
    val education: List<Education> = emptyList(),
    val experience: List<Experience> = emptyList(),
    val skills: List<String> = emptyList()
)

data class Education(
    val school: String,
    val degree: String?,
    val graduationYear: Int?
)

data class Experience(
    val company: String,
    val title: String,
    val duration: String?,
    val description: String?
)