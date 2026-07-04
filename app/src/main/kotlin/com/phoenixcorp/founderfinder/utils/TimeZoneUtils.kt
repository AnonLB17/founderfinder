package com.phoenixcorp.founderfinder.utils

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.*

object TimeZoneUtils {

    // Get user's current time zone
    fun getUserTimeZone(): TimeZone = TimeZone.getDefault()

    // Relative time (e.g. "2 hours ago")
    fun getRelativeTime(timestampMillis: Long): String {
        return DateUtils.getRelativeTimeSpanString(
            timestampMillis,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
    }

    // Time until event (e.g. "in 3 hours")
    fun getTimeUntilEvent(eventUtcMillis: Long): String {
        val now = System.currentTimeMillis()
        val diff = eventUtcMillis - now

        return if (diff > 0) {
            DateUtils.getRelativeTimeSpanString(
                eventUtcMillis,
                now,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_ALL
            ).toString()
        } else {
            "Event has started"
        }
    }

    // Format any UTC time to user's local time
    fun formatLocalTime(utcMillis: Long, pattern: String = "MMM dd, yyyy - hh:mm a"): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        sdf.timeZone = getUserTimeZone()
        return sdf.format(Date(utcMillis))
    }
}