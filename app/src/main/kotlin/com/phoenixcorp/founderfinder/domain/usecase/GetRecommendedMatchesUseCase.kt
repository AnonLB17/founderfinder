package com.phoenixcorp.founderfinder.domain.usecase

import com.phoenixcorp.founderfinder.domain.model.Match
import com.phoenixcorp.founderfinder.domain.model.MatchType
import com.phoenixcorp.founderfinder.domain.repository.MatchRepository
import javax.inject.Inject

class GetRecommendedMatchesUseCase @Inject constructor(
    private val matchRepository: MatchRepository
) {
    suspend operator fun invoke(userId: String, type: MatchType, limit: Int = 10): List<Match> {
        return matchRepository.getRecommendedMatches(userId, type, limit)
    }
}