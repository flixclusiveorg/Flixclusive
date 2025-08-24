package com.flixclusive.core.presentation.player.extensions

import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Formats milliseconds time to readable time string.
 * It also coerces the time on a minimum of 0.
 *
 * @param isInHours to check if the method would pad the string thrice.
 * */
fun Long.formatMinSec(isInHours: Boolean = false): String {
    return if (this <= 0L && isInHours) {
        "00:00:00"
    } else if (this <= 0L) {
        "00:00"
    } else {
        val hours = TimeUnit.MILLISECONDS.toHours(this)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(this) -
            TimeUnit.HOURS.toMinutes(hours)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(this) -
            TimeUnit.MINUTES.toSeconds(minutes) -
            TimeUnit.HOURS.toSeconds(hours)

        if (hours > 0 || isInHours) {
            String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
        }
    }
}
