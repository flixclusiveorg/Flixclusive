package com.flixclusive.feature.mobile.settings.screen.links.manage

import android.content.res.Resources
import java.util.Date
import java.util.concurrent.TimeUnit
import com.flixclusive.core.strings.R as LocaleR

internal object LinkUtil {
    fun String.getMediaFormat(): String? {
        val extension = substringAfterLast('.', "").lowercase()
        return when (extension) {
            "m3u8" -> "HLS"
            "mpd" -> "DASH"
            "mp4" -> "MP4"
            "mkv" -> "MKV"
            "webm" -> "WebM"
            "ts" -> "TS"
            "srt" -> "SRT"
            "vtt" -> "VTT"
            "ass" -> "ASS"
            "ssa" -> "SSA"
            else -> if (contains("hls", ignoreCase = true)) "HLS"
                    else if (contains("dash", ignoreCase = true)) "DASH"
                    else null
        }
    }

    fun Date.toRelativeTime(resources: Resources): String {
        val now = System.currentTimeMillis()
        val diff = now - time

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> resources.getString(LocaleR.string.time_just_now)
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                resources.getQuantityString(LocaleR.plurals.minutes_ago, minutes.toInt(), minutes.toInt())
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                resources.getQuantityString(LocaleR.plurals.hours_ago, hours.toInt(), hours.toInt())
            }
            else -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                resources.getQuantityString(LocaleR.plurals.days_ago, days.toInt(), days.toInt())
            }
        }
    }
}
