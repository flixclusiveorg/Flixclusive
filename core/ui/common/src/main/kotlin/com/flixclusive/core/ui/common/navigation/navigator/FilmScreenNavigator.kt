package com.flixclusive.core.ui.common.navigation.navigator

import com.flixclusive.model.film.Genre

interface FilmScreenNavigator : CommonScreenNavigator {
    fun openGenreScreen(genre: Genre)
}