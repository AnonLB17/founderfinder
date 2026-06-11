package com.phoenixcorp.founderfinder.domain.usecase

import com.phoenixcorp.founderfinder.domain.repository.InvestorRepository
import javax.inject.Inject

class CreateInvestorProfileUseCase @Inject constructor(
    private val investorRepository: InvestorRepository
) {

    suspend operator fun invoke(investorData: Map<String, Any>): Result<Unit> {
        return try {
            // For now we use direct repository call.
            // In the future we can add business logic here (validation, defaults, etc.)
            investorRepository.createInvestorProfile(investorData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}