package com.flixclusive.core.navigation.navigator

import com.flixclusive.model.provider.Catalog

interface ViewAllFilmsAction {
    fun openSeeAllScreen(item: Catalog)
}
