package com.phoenixcorp.founderfinder.domain.model

data class Partner(
    val user: User,
    val partnershipType: List<PartnershipType> = emptyList(),
    val skills: List<String> = emptyList(),
    val lookingFor: String? = null,           // e.g., "Co-founder", "CTO", etc.
    val availability: Boolean = true
)

enum class PartnershipType {
    CO_FOUNDER,
    TECHNICAL_PARTNER,
    MARKETING_PARTNER,
    OPERATIONS_PARTNER,
    OTHER
}