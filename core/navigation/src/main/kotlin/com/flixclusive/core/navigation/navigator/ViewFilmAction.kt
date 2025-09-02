package com.flixclusive.core.ui.common.navigation.navigator

import com.flixclusive.model.film.Film

interface ViewFilmAction {
    fun openFilmScreen(film: Film)
}
