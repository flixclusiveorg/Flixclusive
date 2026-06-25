package com.flixclusive.feature.mobile.settings.screen.links.util

import android.content.res.Resources
import com.flixclusive.core.strings.R
import java.util.Date
import java.util.concurrent.TimeUnit

internal object LinkUtil {
    fun Date.toRelativeTime(resources: Resources): String {
        val now = System.currentTimeMillis()
        val diff = now - time

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> resources.getString(R.string.time_just_now)
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                resources.getQuantityString(R.plurals.minutes_ago, minutes.toInt(), minutes.toInt())
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                resources.getQuantityString(R.plurals.hours_ago, hours.toInt(), hours.toInt())
            }
            else -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                resources.getQuantityString(R.plurals.days_ago, days.toInt(), days.toInt())
            }
        }
    }
}
