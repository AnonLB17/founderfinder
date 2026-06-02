package com.phoenixcorp.founderfinder.domain.repository

import com.phoenixcorp.founderfinder.domain.model.Organization

interface OrganizationRepository {
    suspend fun getOrganizations(): List<Organization>
    suspend fun saveOrganization(organization: Organization)
}