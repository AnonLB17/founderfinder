package com.phoenixcorp.founderfinder.domain.usecase

import com.phoenixcorp.founderfinder.domain.model.Partner
import com.phoenixcorp.founderfinder.domain.model.PartnershipType
import com.phoenixcorp.founderfinder.domain.repository.PartnerRepository
import javax.inject.Inject

class SearchPartnersUseCase @Inject constructor(
    private val partnerRepository: PartnerRepository
) {
    suspend operator fun invoke(
        query: String,
        school: String? = null,
        partnershipTypes: List<PartnershipType>? = null
    ): List<Partner> {
        return partnerRepository.searchPartners(query, school, partnershipTypes)
            .filter { it.availability }
    }
}