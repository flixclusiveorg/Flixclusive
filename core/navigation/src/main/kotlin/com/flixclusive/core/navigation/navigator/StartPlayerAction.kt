package com.flixclusive.core.navigation.navigator

import com.flixclusive.model.film.Film
import com.flixclusive.model.film.common.tv.Episode

interface StartPlayerAction {
    fun playMovie(movie: Film)

    fun playEpisode(episode: Episode, film: Film)

    fun playEpisode(
        season: Int,
        episode: Int,
        film: Film
    )
}
