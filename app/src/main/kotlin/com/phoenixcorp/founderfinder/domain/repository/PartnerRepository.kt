package com.phoenixcorp.founderfinder.domain.repository

import com.phoenixcorp.founderfinder.domain.model.Partner
import com.phoenixcorp.founderfinder.domain.model.PartnershipType

interface PartnerRepository {
    suspend fun searchPartners(
        query: String,
        school: String? = null,
        partnershipTypes: List<PartnershipType>? = null
    ): List<Partner>

    suspend fun getPartnerById(uid: String): Partner?
}