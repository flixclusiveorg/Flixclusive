package com.flixclusive.core.ui.common.navigation.navigator

import com.flixclusive.model.tmdb.Genre
import com.flixclusive.model.tmdb.category.Category

interface HomeNavigator : CommonScreenNavigator {
    fun openGenreScreen(genre: Genre)
    fun openSeeAllScreen(item: Category)
}