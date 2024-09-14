package com.flixclusive.core.ui.common.navigation.navigator

import com.flixclusive.model.film.Genre
import com.flixclusive.model.provider.Catalog

interface HomeNavigator : CommonScreenNavigator {
    fun openGenreScreen(genre: Genre)
    fun openSeeAllScreen(item: Catalog)
}