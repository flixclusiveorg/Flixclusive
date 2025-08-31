package com.flixclusive.core.presentation.common.util

import android.content.Context
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

    /**
     * Formats TV show runtime information including episode duration, number of seasons, and number of episodes.
     *
     * Examples:
     * - (45, 3, 24) -> "45m | 3 seasons | 24 episodes"
     * - (null, 1, 10) -> "1 season | 10 episodes"
     * - (30, 0, 0) -> "30m"
     * */
    fun formatTvRuntime(
        context: Context,
        minutesPerEpisode: Int?,
        seasons: Int,
        episodes: Int,
        separator: String = " | "
    ): String {
        return StringBuilder().apply {
            if (minutesPerEpisode != null) {
                append(
                    minutesPerEpisode
                        .formatAsRuntime()
                        .asString(context)
                )

                append(separator)
            }

            if(seasons > 0) {
                append(
                    context.resources.getQuantityString(
                        R.plurals.season_runtime,
                        seasons,
                        seasons
                    )
                )

                append(separator)
            }


            if(episodes > 0) {
                append(
                    context.resources.getQuantityString(
                        R.plurals.episode_runtime,
                        episodes,
                        episodes
                    )
                )

                append(separator)
            }
        }.toString()
    }
}
