package com.flixclusive.core.navigation.navigator

import com.flixclusive.model.provider.Repository

interface ViewRepositoryAction {
    fun openRepositoryDetails(repository: Repository)
}
