package com.phoenixcorp.founderfinder.domain.model

data class Investor(
    val userId: String,
    val name: String,
    val email: String? = null,
    val profilePicture: String? = null,
    val industry: String = "",
    val philosophy: String = "",
    val preferredIndustries: List<String> = emptyList(),
    val investmentStage: String = "",
    val investmentRangeMin: String = "",
    val investmentRangeMax: String = "",
    val approachAndInvolvement: String = "",
    val roiExpectations: String = "",
    val portfolioCompanies: List<String> = emptyList(),
    val testimonials: List<String> = emptyList(),
    val equityTerms: String = "",
    val boardRole: String = "",
    val returnTimeline: String = "",
    val createdAt: Long = 0L,

    // Add this field
    val isActive: Boolean = true
)