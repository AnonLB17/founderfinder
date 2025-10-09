package com.phoenixcorp.founderfinder.data

data class Activity(
    val id: String = "",
    val title: String = "",
    val partnerId: String = "",
    val date: Long = 0L,
    val time: String? = null,
    val creatorId: String = "",
    val orgId: String? = null,
    val organizationId: String? = null, // Added to match Firestore
    val description: String? = null // Added to match Firestore
)