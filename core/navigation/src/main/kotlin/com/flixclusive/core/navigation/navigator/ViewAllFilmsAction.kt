package com.flixclusive.core.navigation.navigator

import com.flixclusive.model.provider.Catalog

interface ViewAllMediasAction {
    fun openSeeAllScreen(item: Catalog)
}
