package com.phoenixcorp.founderfinder.domain.model

data class Match(
    val id: String,
    val userId1: String,
    val userId2: String,
    val matchType: MatchType,           // INVESTOR, ADVISOR, PARTNER
    val score: Float,                   // 0.0 - 1.0
    val createdAt: Long,
    val status: MatchStatus = MatchStatus.PENDING
)

enum class MatchType {
    INVESTOR,
    ADVISOR,
    PARTNER
}

enum class MatchStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}