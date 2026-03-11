package com.flixclusive.navigation.navgraph

import com.flixclusive.navigation.AppDefaultTransition
import com.ramcosta.composedestinations.annotation.ExternalDestination
import com.ramcosta.composedestinations.annotation.NavGraph
import com.ramcosta.composedestinations.generated.providerdetails.destinations.ProviderDetailsScreenDestination
import com.ramcosta.composedestinations.generated.providermanage.destinations.ProviderManagerScreenDestination
import com.ramcosta.composedestinations.generated.providersettings.destinations.ProviderSettingsScreenDestination
import com.ramcosta.composedestinations.generated.providertest.destinations.ProviderTestScreenDestination
import com.ramcosta.composedestinations.generated.repositorymanage.destinations.RepositoryManagerScreenDestination
import com.ramcosta.composedestinations.generated.settings.destinations.SettingsScreenDestination

@NavGraph<AppNavGraph>(defaultTransitions = AppDefaultTransition::class)
internal annotation class SettingsNavGraph {
    @ExternalDestination<SettingsScreenDestination>(start = true)
    @ExternalDestination<ProviderDetailsScreenDestination>
    @ExternalDestination<ProviderManagerScreenDestination>
    @ExternalDestination<ProviderSettingsScreenDestination>
    @ExternalDestination<ProviderTestScreenDestination>
    @ExternalDestination<RepositoryManagerScreenDestination>
    companion object Includes
}
