package com.flixclusive.core.ui.common.navigation.navigator

import com.flixclusive.model.provider.Catalog

interface ViewAllFilmsAction {
    fun openSeeAllScreen(item: Catalog)
}
