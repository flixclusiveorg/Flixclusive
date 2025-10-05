package com.flixclusive.feature.mobile.player.util

import com.flixclusive.model.film.Film
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.util.FilmType
import java.util.Locale

internal object PlayerUxUtils {
    private const val TV_SHOW_TITLE_FORMAT = "S%d E%d: %s"

    /**
     * Returns the title to be displayed in the player UI.
     *
     * @param film The film object, which can be either a Movie or a TvShow.
     * @param episode The episode object, which is required if the film is a TvShow
     *
     * @return The title to be displayed in the player UI.
     * */
    fun getPlayerTitle(
        film: Film,
        episode: Episode?,
    ): String {
        if (episode == null && film.filmType == FilmType.TV_SHOW) {
            return film.title
        }

        return when (film.filmType) {
            FilmType.MOVIE -> film.title
            FilmType.TV_SHOW -> String.format(
                Locale.ROOT,
                TV_SHOW_TITLE_FORMAT,
                episode!!.season,
                episode.number,
                episode.title,
            )
        }
    }
}
