package com.phoenixcorp.founderfinder.domain.model

import com.phoenixcorp.founderfinder.utils.TimeZoneUtils
import java.util.Calendar

data class Activity(
    val id: String = "",
    val title: String = "",
    val description: String? = null,
    val partnerId: String? = null,
    val date: Long = 0L,                    // UTC millis (this is the source of truth)
    val time: String? = null,               // Optional local time string for display
    val creatorId: String = "",

    val orgId: String? = null,
    val organizationId: String? = null,
    val organizationName: String? = null,

    val activityType: String = "personal",

    val imageUrl: String? = null
) {
    val isOrganizationActivity: Boolean
        get() = activityType == "organization" || !orgId.isNullOrBlank() || !organizationId.isNullOrBlank()

    val displayType: String
        get() = if (isOrganizationActivity) "🏢 Organization" else "👤 Personal"

    // Local time display methods
    fun getLocalStartTime(): String = TimeZoneUtils.formatLocalTime(date, "MMM dd, yyyy - hh:mm a")

    fun getLocalTimeOnly(): String = TimeZoneUtils.formatLocalTime(date, "hh:mm a")
}