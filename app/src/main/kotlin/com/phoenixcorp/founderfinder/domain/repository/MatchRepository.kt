package com.phoenixcorp.founderfinder.domain.repository

import com.phoenixcorp.founderfinder.domain.model.Match
import com.phoenixcorp.founderfinder.domain.model.MatchStatus
import com.phoenixcorp.founderfinder.domain.model.MatchType

interface MatchRepository {
    suspend fun getRecommendedMatches(userId: String, type: MatchType, limit: Int = 10): List<Match>
    suspend fun createMatch(match: Match): Result<Unit>
    suspend fun updateMatchStatus(matchId: String, status: MatchStatus)
}