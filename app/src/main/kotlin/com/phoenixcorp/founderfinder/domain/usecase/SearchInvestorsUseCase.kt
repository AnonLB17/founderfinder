package com.phoenixcorp.founderfinder.domain.usecase

import com.phoenixcorp.founderfinder.domain.model.Investor
import com.phoenixcorp.founderfinder.domain.model.StartupStage
import com.phoenixcorp.founderfinder.domain.repository.InvestorRepository
import javax.inject.Inject

class SearchInvestorsUseCase @Inject constructor(
    private val investorRepository: InvestorRepository
) {
    suspend operator fun invoke(
        query: String,
        minInvestment: Long? = null,
        stages: List<StartupStage>? = null,
        focusAreas: List<String>? = null
    ): List<Investor> {
        return investorRepository.searchInvestors(query, minInvestment, stages, focusAreas)
            .filter { it.isActive }
    }
}