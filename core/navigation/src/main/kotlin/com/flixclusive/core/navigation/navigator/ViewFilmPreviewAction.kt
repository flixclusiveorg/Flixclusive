package com.flixclusive.core.navigation.navigator

import com.flixclusive.model.film.Film

interface ViewFilmPreviewAction {
    fun previewFilm(film: Film)
}
