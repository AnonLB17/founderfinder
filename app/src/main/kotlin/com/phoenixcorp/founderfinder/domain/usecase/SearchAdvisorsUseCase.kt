package com.phoenixcorp.founderfinder.domain.usecase

import com.phoenixcorp.founderfinder.domain.model.Advisor
import com.phoenixcorp.founderfinder.domain.repository.AdvisorRepository
import javax.inject.Inject

class SearchAdvisorsUseCase @Inject constructor(
    private val advisorRepository: AdvisorRepository
) {
    suspend operator fun invoke(
        query: String,
        school: String? = null,
        expertise: List<String>? = null
    ): List<Advisor> {
        return advisorRepository.searchAdvisors(query, school, expertise)
            .filter { it.availability && it.rating >= 3.5f } // Business rule example
            .sortedByDescending { it.rating }
    }
}