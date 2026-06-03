package com.phoenixcorp.founderfinder.domain.model

data class Organization(
    val id: String = "",                    // Standard field name (used in Firestore document ID)
    val name: String = "",
    val description: String = "",
    val imageUri: String? = null,

    // Additional useful fields
    val creatorId: String = "",
    val partnerIds: List<String> = emptyList(),
    val financingDocuments: List<String> = emptyList(), // business plans, pitch decks, etc.

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)