package com.flixclusive.core.ui.common.navigation.navigator

import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.model.tmdb.Film

interface FilmScreenTvNavigator : GoBackAction {
    fun openFilmScreenSeamlessly(film: Film)
}