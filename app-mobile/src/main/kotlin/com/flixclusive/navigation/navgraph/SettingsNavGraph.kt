package com.flixclusive.navigation.navgraph

import com.flixclusive.navigation.AppDefaultTransition
import com.ramcosta.composedestinations.annotation.ExternalDestination
import com.ramcosta.composedestinations.annotation.NavGraph
import com.ramcosta.composedestinations.generated.providermanage.destinations.ProviderManagerScreenDestination
import com.ramcosta.composedestinations.generated.providersettings.destinations.ProviderSettingsScreenDestination
import com.ramcosta.composedestinations.generated.repositorymanage.destinations.RepositoryManagerScreenDestination
import com.ramcosta.composedestinations.generated.settings.destinations.AppearanceTweakScreenDestination
import com.ramcosta.composedestinations.generated.settings.destinations.DataTweakScreenDestination
import com.ramcosta.composedestinations.generated.settings.destinations.ManageMediaLinksTweakScreenDestination
import com.ramcosta.composedestinations.generated.settings.destinations.MediaLinkCardsTweakScreenDestination
import com.ramcosta.composedestinations.generated.settings.destinations.MediaLinksShowDetailTweakScreenDestination
import com.ramcosta.composedestinations.generated.settings.destinations.PlayerTweakScreenDestination
import com.ramcosta.composedestinations.generated.settings.destinations.ProvidersTweakScreenDestination
import com.ramcosta.composedestinations.generated.settings.destinations.SettingsScreenDestination
import com.ramcosta.composedestinations.generated.settings.destinations.SubtitlesTweakScreenDestination
import com.ramcosta.composedestinations.generated.settings.destinations.SystemTweakScreenDestination

@NavGraph<AppNavGraph>(defaultTransitions = AppDefaultTransition::class)
internal annotation class SettingsNavGraph {
    @ExternalDestination<SettingsScreenDestination>(start = true)
    @ExternalDestination<ProviderManagerScreenDestination>
    @ExternalDestination<ProviderSettingsScreenDestination>
    @ExternalDestination<RepositoryManagerScreenDestination>
    @ExternalDestination<AppearanceTweakScreenDestination>
    @ExternalDestination<PlayerTweakScreenDestination>
    @ExternalDestination<ProvidersTweakScreenDestination>
    @ExternalDestination<DataTweakScreenDestination>
    @ExternalDestination<SystemTweakScreenDestination>
    @ExternalDestination<SubtitlesTweakScreenDestination>
    @ExternalDestination<MediaLinkCardsTweakScreenDestination>
    @ExternalDestination<ManageMediaLinksTweakScreenDestination>
    @ExternalDestination<MediaLinksShowDetailTweakScreenDestination>
    companion object Includes
}
