package com.flixclusive.presentation.common.viewmodels.player

import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.TMDBEpisode

data class PlayerScreenNavArgs(
    val film: Film,
    val episodeToPlay: TMDBEpisode?,
)