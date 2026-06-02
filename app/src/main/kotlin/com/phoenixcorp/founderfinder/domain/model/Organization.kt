package com.phoenixcorp.founderfinder.domain.model

data class Organization(
    val orgId: String = "",
    val name: String = "",
    val description: String = "",
    val imageUri: String? = null,
    val creatorId: String = "",
    val partnerIds: List<String> = emptyList(), // Added field
    val financingDocuments: List<String> = emptyList() // New field for associated files like business plan/proposal
)