package com.flixclusive.core.util.film

import com.flixclusive.core.util.R
import com.flixclusive.core.util.common.ui.UiText

fun formatMinutes(totalMinutes: Int?): UiText {
    if (totalMinutes == null || totalMinutes <= 0)
        return UiText.StringResource(R.string.no_runtime)

    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    val hoursText = if (hours > 0) "${hours}h " else ""
    val minutesText = if (minutes > 0) "${minutes}m" else ""

    return UiText.StringValue((hoursText + minutesText).trim())
}

fun formatRating(number: Double?): UiText {
    val noRatingsMessage = UiText.StringResource(R.string.no_ratings)

    if (number == null)
        return noRatingsMessage

    val ratings = if (number % 1 == 0.0) {
        String.format("%.1f", number)
    } else {
        String.format("%.2f", number)
    }
    
    return if(ratings == "0.0") noRatingsMessage else UiText.StringValue(ratings)
}