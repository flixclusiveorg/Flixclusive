package com.flixclusive.core.ui.common.navigation.navigator

import com.flixclusive.model.film.Genre
import com.flixclusive.model.provider.Catalog

interface SearchScreenNavigator {
    fun openSearchExpandedScreen()

    fun openSeeAllScreen(item: Catalog)

    fun openGenreScreen(genre: Genre)
}