package com.flixclusive.core.ui.common.navigation.navigator

import com.flixclusive.model.provider.Catalog

interface HomeNavigator : CommonScreenNavigator, GenreScreenNavigator {
    fun openSeeAllScreen(item: Catalog)
}