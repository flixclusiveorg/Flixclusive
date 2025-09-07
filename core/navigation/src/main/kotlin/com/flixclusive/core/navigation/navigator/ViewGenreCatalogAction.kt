package com.flixclusive.core.navigation.navigator

import com.flixclusive.core.navigation.navargs.GenreWithBackdrop

interface ViewGenreCatalogAction {
    fun openGenreScreen(genre: GenreWithBackdrop)
}
