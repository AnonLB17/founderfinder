package com.phoenixcorp.founderfinder.domain.model

data class Investor(
    val user: User,
    val investmentRangeMin: Long? = null,      // in USD
    val investmentRangeMax: Long? = null,
    val focusAreas: List<String> = emptyList(),
    val preferredStages: List<StartupStage> = emptyList(),
    val portfolioCount: Int = 0,
    val linkedinUrl: String? = null,
    val isActive: Boolean = true
)

enum class StartupStage {
    PRE_SEED, SEED, SERIES_A, SERIES_B, GROWTH, ANY
}