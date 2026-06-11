package com.phoenixcorp.founderfinder.domain.model

import androidx.annotation.Keep

@Keep
data class Partner(
    val user: User,
    val partnershipType: List<PartnershipType> = emptyList(),
    val skills: List<String> = emptyList(),
    val lookingFor: String? = null,
    val availability: Boolean = true,
    val experienceYears: Int = 0
)

@Keep
enum class PartnershipType {
    CO_FOUNDER,
    TECHNICAL_PARTNER,
    MARKETING_PARTNER,
    OPERATIONS_PARTNER,
    OTHER
}