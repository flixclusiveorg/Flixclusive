package com.flixclusive.core.navigation.navigator

import com.flixclusive.model.film.Film
import com.flixclusive.model.film.common.tv.Episode

interface StartPlayerAction {
    fun play(film: Film, episode: Episode? = null)
}
