package com.flixclusive.core.ui.common.navigation

import com.flixclusive.gradle.entities.Repository

interface RepositorySearchScreenNavigator : GoBackAction {
    fun openRepositoryScreen(repository: Repository)
}