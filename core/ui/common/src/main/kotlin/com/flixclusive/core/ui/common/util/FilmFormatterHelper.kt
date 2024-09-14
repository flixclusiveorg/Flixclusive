package com.flixclusive.core.ui.common.util

import android.content.Context
import com.flixclusive.core.locale.UiText
import com.flixclusive.model.database.WatchHistoryItem
import com.flixclusive.model.database.util.getNextEpisodeToWatch
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.util.FilmType
import java.util.Locale
import com.flixclusive.core.locale.R as LocaleR


fun formatMinutes(totalMinutes: Int?): UiText {
    if (totalMinutes == null || totalMinutes <= 0)
        return UiText.StringResource(LocaleR.string.no_runtime)

    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    val hoursText = if (hours > 0) "${hours}h " else ""
    val minutesText = if (minutes > 0) "${minutes}m" else ""

    return UiText.StringValue((hoursText + minutesText).trim())
}

fun formatRating(number: Double?): UiText {
    val noRatingsMessage = UiText.StringResource(LocaleR.string.no_ratings)

    if (number == null)
        return noRatingsMessage

    val ratings = if (number % 1 == 0.0) {
        String.format(Locale.ROOT, "%.1f", number)
    } else {
        String.format(Locale.ROOT, "%.2f", number)
    }

    return if(ratings == "0.0") noRatingsMessage else UiText.StringValue(ratings)
}

fun formatTvRuntime(
    context: Context,
    show: TvShow,
    separator: String = " | "
): String {
    return StringBuilder().apply {
        context.run {
            show.run {
                if (runtime != null) {
                    append(formatMinutes(runtime).asString(context))
                    append(separator)
                }

                if(totalSeasons > 0) {
                    append(getString(LocaleR.string.season_runtime_formatter, totalSeasons))

                    if(totalSeasons > 1)
                        append("s")

                    append(separator)
                }


                if(totalEpisodes > 0) {
                    append(getString(LocaleR.string.episode_runtime_formatter, totalEpisodes))

                    if(totalEpisodes > 1)
                        append("s")

                    append(separator)
                }
            }
        }
    }.toString()
}

fun formatPlayButtonLabel(
    watchHistoryItem: WatchHistoryItem?,
): UiText {
    if (watchHistoryItem == null)
        return UiText.StringResource(LocaleR.string.watch)

    return when (watchHistoryItem.film.filmType) {
        FilmType.MOVIE -> {
            if (watchHistoryItem.episodesWatched.last().isFinished) {
                UiText.StringResource(LocaleR.string.watch_again)
            } else {
                UiText.StringResource(LocaleR.string.continue_watching)
            }
        }

        FilmType.TV_SHOW -> {
            val (season, episode) = getNextEpisodeToWatch(watchHistoryItem = watchHistoryItem)

            if (season == null) {
                UiText.StringResource(LocaleR.string.watch_again)
            } else {
                UiText.StringResource(LocaleR.string.continue_watching_tv_show, season, episode!!)
            }
        }
    }
}