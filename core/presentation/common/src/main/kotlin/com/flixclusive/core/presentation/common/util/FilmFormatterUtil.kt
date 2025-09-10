package com.flixclusive.core.presentation.common.util

import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.presentation.common.R
import java.util.Locale

/**
 * Utility object for formatting film-related information such as runtime and ratings.
 * */
object FilmFormatterUtil {
    /**
     * Formats minutes into a human-readable string.
     *
     * Examples:
     * - 0 minutes -> "No runtime"
     * - 45 minutes -> "45m"
     * - 120 minutes -> "2h"
     * - 135 minutes -> "2h 15m"
     * */
    fun Int.formatAsRuntime(): UiText {
        if (this <= 0)
            return UiText.from(R.string.no_runtime)

        val hours = this / 60
        val minutes = this % 60

        val hoursText = if (hours > 0) "${hours}h " else ""
        val minutesText = if (minutes > 0) "${minutes}m" else ""

        return UiText.StringValue((hoursText + minutesText).trim())
    }

    /**
     * Helper function to format minutes into a human-readable string.
     *
     * Examples:
     * - 0.0 -> "No ratings"
     * - 3.0 -> "3.0"
     * - 4.25 -> "4.25"
     * */
    fun Double.formatAsRating(): UiText {
        val ratings = if (this % 1 == 0.0) {
            String.format(Locale.ROOT, "%.1f", this)
        } else {
            String.format(Locale.ROOT, "%.2f", this)
        }

        return when (ratings) {
            "0.0" -> UiText.from(R.string.no_ratings)
            else -> UiText.StringValue(
                // Remove trailing 0 if present (e.g., 3.20 -> 3.2, but 3.0 stays 3.0)
                when {
                    ratings.endsWith("0") && !ratings.endsWith(".0") -> ratings.dropLast(1)
                    else -> ratings
                }
            )
        }
    }
}
