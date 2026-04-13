package com.flixclusive.navigation.navgraph

import androidx.compose.runtime.Composable
import com.flixclusive.core.navigation.navargs.FilmScreenNavArgs
import com.flixclusive.core.navigation.navigator.GoBackAction
import com.flixclusive.feature.mobile.film.FilmScreen
import com.flixclusive.feature.mobile.film.FilmScreenNavigator
import com.flixclusive.feature.mobile.markdown.MarkdownScreen
import com.flixclusive.feature.mobile.seeAll.SeeAllScreen
import com.flixclusive.feature.mobile.seeAll.SeeAllScreenNavArgs
import com.flixclusive.feature.mobile.seeAll.SeeAllScreenNavigator
import com.flixclusive.navigation.AppDefaultTransition
import com.flixclusive.navigation.InternalDestination
import com.ramcosta.composedestinations.annotation.ExternalDestination
import com.ramcosta.composedestinations.annotation.ExternalModuleDestinations
import com.ramcosta.composedestinations.annotation.NavHostGraph
import com.ramcosta.composedestinations.generated.appupdates.destinations.AppUpdatesScreenDestination
import com.ramcosta.composedestinations.generated.onboarding.destinations.OnboardingScreenDestination
import com.ramcosta.composedestinations.generated.player.destinations.PlayerScreenDestination
import com.ramcosta.composedestinations.generated.profiles.destinations.UserProfilesScreenDestination
import com.ramcosta.composedestinations.generated.provideradd.destinations.AddProviderScreenDestination
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
    @ExternalModuleDestinations<UsereditModuleDestinations>
    companion object Includes
}

@InternalDestination<HomeNavGraph>(navArgs = FilmScreenNavArgs::class)
@InternalDestination<SearchNavGraph>(navArgs = FilmScreenNavArgs::class)
@InternalDestination<LibraryNavGraph>(navArgs = FilmScreenNavArgs::class)
@Composable
internal fun AppLevelFilmScreen(
    navigator: FilmScreenNavigator,
    navArgs: FilmScreenNavArgs
) {
    FilmScreen(
        navigator = navigator,
        navArgs = navArgs
    )
}

@InternalDestination<HomeNavGraph>(navArgs = SeeAllScreenNavArgs::class)
@InternalDestination<SearchNavGraph>(navArgs = SeeAllScreenNavArgs::class)
@InternalDestination<LibraryNavGraph>(navArgs = SeeAllScreenNavArgs::class)
@Composable
internal fun AppLevelSeeAllScreen(
    navigator: SeeAllScreenNavigator,
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
    navigator: GoBackAction,
    title: String,
    description: String,
) {
    MarkdownScreen(
        navigator = navigator,
        title = title,
        description = description
    )
}
