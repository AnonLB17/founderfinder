package com.phoenixcorp.founderfinder.domain.model

data class Invitation(
    val invitationId: String = "",
    val orgId: String = "",
    val inviterId: String = "",
    val inviteeId: String = "",
    val status: String = "pending", // "pending", "accepted", "denied"
    val type: String = "collaborator", // "collaborator", "partner", "advisor"
    val createdAt: Long = 0L
)