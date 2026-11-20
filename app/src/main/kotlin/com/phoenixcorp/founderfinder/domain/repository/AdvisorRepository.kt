package com.phoenixcorp.founderfinder.domain.repository

import com.phoenixcorp.founderfinder.domain.model.Advisor

interface AdvisorRepository {
    suspend fun searchAdvisors(
        query: String,
        school: String? = null,
        expertise: List<String>? = null
    ): List<Advisor>

    suspend fun getAdvisorById(uid: String): Advisor?
    suspend fun getFeaturedAdvisors(limit: Int = 10): List<Advisor>
    suspend fun updateAdvisorProfile(advisor: Advisor): Result<Unit>
}