package com.flixclusive.core.ui.player

import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.common.tv.Episode

data class PlayerScreenNavArgs(
    val film: Film,
    val episodeToPlay: Episode?,
)