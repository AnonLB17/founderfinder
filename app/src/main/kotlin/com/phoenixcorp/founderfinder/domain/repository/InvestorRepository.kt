package com.phoenixcorp.founderfinder.domain.repository

import com.phoenixcorp.founderfinder.domain.model.Investor
import com.phoenixcorp.founderfinder.domain.model.StartupStage

interface InvestorRepository {
    suspend fun searchInvestors(
        query: String,
        minInvestment: Long? = null,
        stages: List<StartupStage>? = null,
        focusAreas: List<String>? = null
    ): List<Investor>

    suspend fun getInvestorById(uid: String): Investor?
    suspend fun getInvestorMatchesForFounder(founderId: String, limit: Int = 10): List<Investor>
}