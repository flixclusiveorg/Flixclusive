package com.flixclusive.core.ui.film

import com.flixclusive.model.film.Film

data class FilmScreenNavArgs(
    val film: Film,
    /**
     * This property is for tv screens
     * */
    val startPlayerAutomatically: Boolean
)