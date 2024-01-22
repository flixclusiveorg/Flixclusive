package com.flixclusive.feature.mobile.film.util

import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.model.database.WatchHistoryItem
import com.flixclusive.model.database.util.getNextEpisodeToWatch
import com.flixclusive.core.util.R as UtilR

internal fun formatPlayButtonLabel(
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