package com.flixclusive.core.util.film

fun formatMinutes(totalMinutes: Int?): String {
    if (totalMinutes == null)
        return "No runtime"

    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    val hoursText = if (hours > 0) "${hours}h " else ""
    val minutesText = if (minutes > 0) "${minutes}m" else ""

    return (hoursText + minutesText).trim()
}