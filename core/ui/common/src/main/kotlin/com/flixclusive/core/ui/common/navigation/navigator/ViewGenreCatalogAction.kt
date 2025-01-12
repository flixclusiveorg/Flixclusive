package com.flixclusive.core.ui.common.navigation.navigator

import com.flixclusive.core.ui.common.navigation.navargs.GenreWithBackdrop

interface ViewGenreCatalogAction {
    fun openGenreScreen(genre: GenreWithBackdrop)
}
