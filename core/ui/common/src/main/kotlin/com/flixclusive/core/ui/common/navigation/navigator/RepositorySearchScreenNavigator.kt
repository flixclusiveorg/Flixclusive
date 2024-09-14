package com.flixclusive.core.ui.common.navigation.navigator

import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.model.provider.Repository

interface RepositorySearchScreenNavigator : GoBackAction {
    fun openRepositoryScreen(repository: Repository)
}