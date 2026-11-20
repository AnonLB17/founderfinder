package com.phoenixcorp.founderfinder.domain.permissions

/**
 * Central permission rules by role.
 *
 * SPECTATOR = view-only (fly on the wall): no create, no update, no delete.
 * All other roles keep full write access unless you tighten them later.
 */
object UserPermissions {

    const val ROLE_SPECTATOR = "SPECTATOR"
    const val ROLE_FOUNDER = "FOUNDER"
    const val ROLE_PARTNER = "PARTNER"
    const val ROLE_ADVISOR = "ADVISOR"
    const val ROLE_INVESTOR = "INVESTOR"

    fun isSpectator(role: String?): Boolean =
        role.equals(ROLE_SPECTATOR, ignoreCase = true)

    /** Browse home, forums, profiles, search, notifications, chat (read). */
    fun canView(role: String?): Boolean = true

    /** Create threads, ideas, forums, activities, orgs, posts, uploads. */
    fun canCreate(role: String?): Boolean = !isSpectator(role)

    /** Edit own profile fields, edit own posts/threads, update role extras. */
    fun canUpdate(role: String?): Boolean = !isSpectator(role)

    /** Delete own content. */
    fun canDelete(role: String?): Boolean = !isSpectator(role)

    /** Send chat messages. Spectators can open chat UI but not send. */
    fun canSendMessage(role: String?): Boolean = !isSpectator(role)

    /** Like / favorite – treat as write; block for spectators. */
    fun canEngage(role: String?): Boolean = !isSpectator(role)

    /** Post comments / replies. */
    fun canComment(role: String?): Boolean = !isSpectator(role)

    fun blockedActionMessage(action: String = "do that"): String =
        "Spectators can view only. Sign up as a Founder to $action."
}