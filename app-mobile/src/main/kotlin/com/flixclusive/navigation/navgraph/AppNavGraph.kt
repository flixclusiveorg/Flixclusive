package com.flixclusive.navigation.navgraph

import androidx.compose.runtime.Composable
import com.flixclusive.core.navigation.navargs.MediaScreenNavArgs
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
import com.ramcosta.composedestinations.generated.markdown.destinations.MarkdownScreenDestination
import com.ramcosta.composedestinations.generated.media.destinations.MediaImagePreviewDialogDestination
import com.ramcosta.composedestinations.generated.media.destinations.MediaLinksBottomSheetDestination
import com.ramcosta.composedestinations.generated.onboarding.destinations.OnboardingScreenDestination
import com.ramcosta.composedestinations.generated.player.destinations.PlayerScreenDestination
import com.ramcosta.composedestinations.generated.player.destinations.PlayerSplashScreenDestination
import com.ramcosta.composedestinations.generated.provideradd.destinations.AddProviderScreenDestination
import com.ramcosta.composedestinations.generated.providerdetails.destinations.ProviderDetailsBottomSheetDestination
import com.ramcosta.composedestinations.generated.splashscreen.destinations.SplashScreenDestination
import com.ramcosta.composedestinations.generated.useradd.destinations.AddUserScreenDestination
import com.ramcosta.composedestinations.generated.useredit.UsereditModuleDestinations
import com.ramcosta.composedestinations.generated.userprofiles.destinations.UserProfilesScreenDestination

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
    @ExternalDestination<MarkdownScreenDestination>
    @ExternalModuleDestinations<UsereditModuleDestinations>
    companion object Includes
}

@InternalDestination<HomeNavGraph>(navArgs = MediaScreenNavArgs::class)
@InternalDestination<LibraryNavGraph>(navArgs = MediaScreenNavArgs::class)
@InternalDestination<SearchNavGraph>(navArgs = MediaScreenNavArgs::class)
@InternalDestination<SettingsNavGraph>(navArgs = MediaScreenNavArgs::class)
@Composable
internal fun CommonMediaScreen(
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
@InternalDestination<SearchNavGraph>(
    navArgs = MediaPreviewNavArgs::class,
    style = DestinationStyleBottomSheet::class
)
@InternalDestination<SettingsNavGraph>(
    navArgs = MediaPreviewNavArgs::class,
    style = DestinationStyleBottomSheet::class
)
@Composable
internal fun CommonMediaPreviewBottomSheet(
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
@InternalDestination<SearchNavGraph>(navArgs = SeeAllScreenNavArgs::class)
@InternalDestination<SettingsNavGraph>(navArgs = SeeAllScreenNavArgs::class)
@Composable
internal fun CommonSeeAllScreen(
    navigator: NavigatorSeeAllScreen,
    navArgs: SeeAllScreenNavArgs
) {
    SeeAllScreen(
        navigator = navigator,
        navArgs = navArgs
    )
}
