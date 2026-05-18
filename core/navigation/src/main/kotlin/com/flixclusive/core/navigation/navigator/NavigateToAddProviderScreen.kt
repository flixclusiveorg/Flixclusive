package com.flixclusive.core.navigation.navigator

import com.flixclusive.model.provider.Repository

interface NavigateToAddProviderScreen {
    fun navigateToAddProviderScreen(initialSelectedRepositoryFilter: Repository? = null)
}
