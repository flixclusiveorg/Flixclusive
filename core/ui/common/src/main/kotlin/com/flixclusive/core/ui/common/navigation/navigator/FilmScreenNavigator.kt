package com.flixclusive.core.ui.common.navigation.navigator

import com.flixclusive.model.tmdb.Genre

interface FilmScreenNavigator : CommonScreenNavigator {
    fun openGenreScreen(genre: Genre)
}