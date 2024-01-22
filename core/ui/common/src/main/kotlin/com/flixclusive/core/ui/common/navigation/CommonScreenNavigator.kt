package com.flixclusive.core.ui.common.navigation

import com.flixclusive.model.tmdb.Film

interface CommonScreenNavigator : GoBackAction {
    fun openFilmScreen(film: Film)
}