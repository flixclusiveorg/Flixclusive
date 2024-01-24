package com.flixclusive.core.ui.common.util

import android.content.Context
import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.core.util.film.formatMinutes
import com.flixclusive.model.database.WatchHistoryItem
import com.flixclusive.model.database.util.getNextEpisodeToWatch
import com.flixclusive.model.tmdb.TvShow
import com.flixclusive.core.util.R as UtilR

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
                    append(getString(UtilR.string.season_runtime_formatter, totalSeasons))

                    if(totalSeasons > 1)
                        append("s")

                    append(separator)
                }


                if(totalEpisodes > 0) {
                    append(getString(UtilR.string.episode_runtime_formatter, totalEpisodes))

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
        return UiText.StringResource(UtilR.string.watch)

    return when (watchHistoryItem.film.filmType) {
        FilmType.MOVIE -> {
            if (watchHistoryItem.episodesWatched.last().isFinished) {
                UiText.StringResource(UtilR.string.watch_again)
            } else {
                UiText.StringResource(UtilR.string.continue_watching)
            }
        }

        FilmType.TV_SHOW -> {
            val (season, episode) = getNextEpisodeToWatch(watchHistoryItem = watchHistoryItem)

            if (season == null) {
                UiText.StringResource(UtilR.string.watch_again)
            } else {
                UiText.StringResource(UtilR.string.continue_watching_tv_show, season, episode!!)
            }
        }
    }
}