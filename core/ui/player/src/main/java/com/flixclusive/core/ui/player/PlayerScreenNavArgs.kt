package com.flixclusive.core.ui.player

import com.flixclusive.model.film.Film
import com.flixclusive.model.film.common.tv.Episode

data class PlayerScreenNavArgs(
    val film: Film,
    val episodeToPlay: Episode?,
)