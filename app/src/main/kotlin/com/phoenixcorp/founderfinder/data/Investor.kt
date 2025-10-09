package com.phoenixcorp.founderfinder.data

data class Investor(
    val name: String = "",
    val email: String = "",
    val industry: String = "",
    val philosophy: String = "",
    val userId: String = "",
    val createdAt: Long = 0L,
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
    val profilePicture: String? = null // Added field
)