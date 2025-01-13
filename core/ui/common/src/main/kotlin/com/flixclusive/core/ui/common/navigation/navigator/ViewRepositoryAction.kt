package com.flixclusive.core.ui.common.navigation.navigator

import com.flixclusive.model.provider.Repository

interface ViewRepositoryAction {
    fun openRepositoryDetails(repository: Repository)
}
