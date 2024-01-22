package com.flixclusive.core.ui.player

import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.TMDBEpisode

data class PlayerScreenNavArgs(
    val film: Film,
    val episodeToPlay: TMDBEpisode?,
)