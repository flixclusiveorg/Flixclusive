package com.flixclusive.navigation.navgraph

import androidx.compose.runtime.Composable
import com.flixclusive.core.navigation.navargs.MediaScreenNavArgs
import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.feature.mobile.markdown.MarkdownScreen
import com.flixclusive.feature.mobile.media.MediaScreen
import com.flixclusive.feature.mobile.media.modal.MediaPreviewBottomSheet
import com.flixclusive.feature.mobile.media.modal.MediaPreviewNavArgs
import com.flixclusive.feature.mobile.media.navigator.NavigatorMediaPreviewBottomSheet
import com.flixclusive.feature.mobile.media.navigator.NavigatorMediaScreen
import com.flixclusive.feature.mobile.seeAll.NavigatorSeeAllScreen
import com.flixclusive.feature.mobile.seeAll.SeeAllScreen
import com.flixclusive.feature.mobile.seeAll.SeeAllScreenNavArgs
import com.flixclusive.navigation.AppDefaultTransition
import com.flixclusive.navigation.InternalDestination
import com.ramcosta.composedestinations.annotation.ExternalDestination
import com.ramcosta.composedestinations.annotation.ExternalModuleDestinations
import com.ramcosta.composedestinations.annotation.NavHostGraph
import com.ramcosta.composedestinations.bottomsheet.spec.DestinationStyleBottomSheet
import com.ramcosta.composedestinations.generated.appupdates.destinations.AppUpdatesScreenDestination
import com.ramcosta.composedestinations.generated.media.destinations.MediaImagePreviewDialogDestination
import com.ramcosta.composedestinations.generated.media.destinations.MediaLinksBottomSheetDestination
import com.ramcosta.composedestinations.generated.onboarding.destinations.OnboardingScreenDestination
import com.ramcosta.composedestinations.generated.player.destinations.PlayerScreenDestination
import com.ramcosta.composedestinations.generated.player.destinations.PlayerSplashScreenDestination
import com.ramcosta.composedestinations.generated.profiles.destinations.UserProfilesScreenDestination
import com.ramcosta.composedestinations.generated.provideradd.destinations.AddProviderScreenDestination
import com.ramcosta.composedestinations.generated.providerdetails.destinations.ProviderDetailsBottomSheetDestination
import com.ramcosta.composedestinations.generated.splashscreen.destinations.SplashScreenDestination
import com.ramcosta.composedestinations.generated.useradd.destinations.AddUserScreenDestination
import com.ramcosta.composedestinations.generated.useredit.UsereditModuleDestinations


@NavHostGraph(defaultTransitions = AppDefaultTransition::class)
internal annotation class AppNavGraph {
    @ExternalDestination<AddProviderScreenDestination>
    @ExternalDestination<AddUserScreenDestination>
    @ExternalDestination<AppUpdatesScreenDestination>
    @ExternalDestination<PlayerScreenDestination>
    @ExternalDestination<SplashScreenDestination>(start = true)
    @ExternalDestination<OnboardingScreenDestination>
    @ExternalDestination<UserProfilesScreenDestination>
    @ExternalDestination<MediaImagePreviewDialogDestination>
    @ExternalDestination<MediaLinksBottomSheetDestination>
    @ExternalDestination<PlayerSplashScreenDestination>
    @ExternalDestination<ProviderDetailsBottomSheetDestination>
    @ExternalModuleDestinations<UsereditModuleDestinations>
    companion object Includes
}

@InternalDestination<HomeNavGraph>(navArgs = MediaScreenNavArgs::class)
@InternalDestination<LibraryNavGraph>(navArgs = MediaScreenNavArgs::class)
@Composable
internal fun AppLevelMediaScreen(
    navigator: NavigatorMediaScreen,
    navArgs: MediaScreenNavArgs
) {
    MediaScreen(
        navigator = navigator,
        navArgs = navArgs
    )
}

@InternalDestination<HomeNavGraph>(
    navArgs = MediaPreviewNavArgs::class,
    style = DestinationStyleBottomSheet::class
)
@InternalDestination<LibraryNavGraph>(
    navArgs = MediaPreviewNavArgs::class,
    style = DestinationStyleBottomSheet::class
)
@Composable
internal fun AppLevelMediaPreviewBottomSheet(
    navigator: NavigatorMediaPreviewBottomSheet,
    navArgs: MediaPreviewNavArgs
) {
    MediaPreviewBottomSheet(
        navigator = navigator,
        args = navArgs
    )
}


@InternalDestination<HomeNavGraph>(navArgs = SeeAllScreenNavArgs::class)
@InternalDestination<LibraryNavGraph>(navArgs = SeeAllScreenNavArgs::class)
@Composable
internal fun AppLevelSeeAllScreen(
    navigator: NavigatorSeeAllScreen,
    navArgs: SeeAllScreenNavArgs
) {
    SeeAllScreen(
        navigator = navigator,
        navArgs = navArgs
    )
}

@InternalDestination<AppNavGraph>
@InternalDestination<SettingsNavGraph>
@Composable
internal fun AppLevelMarkdownScreen(
    navigator: NavigateBack,
    title: String,
    description: String,
) {
    MarkdownScreen(
        navigator = navigator,
        title = title,
        description = description
    )
}
