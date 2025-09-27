package com.flixclusive.core.navigation.navigator

import com.flixclusive.model.provider.Repository

interface AddProviderAction {
    fun openAddProviderScreen(initialSelectedRepositoryFilter: Repository? = null)
}
