package com.flixclusive.navigation

import com.flixclusive.feature.mobile.app.updates.destinations.AppUpdatesScreenDestination
import com.flixclusive.feature.mobile.film.destinations.FilmScreenDestination
import com.flixclusive.feature.mobile.home.destinations.HomeScreenDestination
import com.flixclusive.feature.mobile.library.details.destinations.LibraryDetailsScreenDestination
import com.flixclusive.feature.mobile.library.manage.destinations.ManageLibraryScreenDestination
import com.flixclusive.feature.mobile.markdown.destinations.MarkdownScreenDestination
import com.flixclusive.feature.mobile.player.destinations.PlayerScreenDestination
import com.flixclusive.feature.mobile.profiles.destinations.UserProfilesScreenDestination
import com.flixclusive.feature.mobile.provider.add.destinations.AddProviderScreenDestination
import com.flixclusive.feature.mobile.provider.details.destinations.ProviderDetailsScreenDestination
import com.flixclusive.feature.mobile.provider.manage.destinations.ProviderManagerScreenDestination
import com.flixclusive.feature.mobile.provider.settings.destinations.ProviderSettingsScreenDestination
import com.flixclusive.feature.mobile.provider.test.destinations.ProviderTestScreenDestination
import com.flixclusive.feature.mobile.repository.manage.destinations.RepositoryManagerScreenDestination
import com.flixclusive.feature.mobile.search.destinations.SearchScreenDestination
import com.flixclusive.feature.mobile.searchExpanded.destinations.SearchExpandedScreenDestination
import com.flixclusive.feature.mobile.seeAll.destinations.SeeAllScreenDestination
import com.flixclusive.feature.mobile.settings.screen.root.destinations.SettingsScreenDestination
import com.flixclusive.feature.mobile.user.add.destinations.AddUserScreenDestination
import com.flixclusive.feature.mobile.user.destinations.PinSetupScreenDestination
import com.flixclusive.feature.mobile.user.destinations.PinVerifyScreenDestination
import com.flixclusive.feature.mobile.user.destinations.UserAvatarSelectScreenDestination
import com.flixclusive.feature.mobile.user.destinations.UserEditScreenDestination
import com.flixclusive.feature.splashScreen.destinations.SplashScreenDestination
import com.ramcosta.composedestinations.dynamic.routedIn
import com.ramcosta.composedestinations.spec.DestinationSpec
import com.ramcosta.composedestinations.spec.NavGraphSpec

internal object MobileNavGraphs {
    val home =
        object : NavGraphSpec {
            override val route = "home"

            override val startRoute = HomeScreenDestination routedIn this

            override val destinationsByRoute =
                listOf<DestinationSpec<*>>(
                    HomeScreenDestination,
                    FilmScreenDestination,
                    SeeAllScreenDestination,
                ).routedIn(this)
                    .associateBy { it.route }
        }

    val search =
        object : NavGraphSpec {
            override val route = "search"

            override val startRoute = SearchScreenDestination routedIn this

            override val destinationsByRoute =
                listOf<DestinationSpec<*>>(
                    SearchScreenDestination,
                    SearchExpandedScreenDestination,
                    FilmScreenDestination,
                    SeeAllScreenDestination,
                ).routedIn(this)
                    .associateBy { it.route }
        }

    val library =
        object : NavGraphSpec {
            override val route = "library"

            override val startRoute = ManageLibraryScreenDestination routedIn this

            override val destinationsByRoute =
                listOf<DestinationSpec<*>>(
                    ManageLibraryScreenDestination,
                    LibraryDetailsScreenDestination,
                    FilmScreenDestination,
                    SeeAllScreenDestination,
                ).routedIn(this)
                    .associateBy { it.route }
        }

    val settings =
        object : NavGraphSpec {
            override val route = "settings"

            override val startRoute = SettingsScreenDestination routedIn this

            override val destinationsByRoute =
                listOf<DestinationSpec<*>>(
                    SettingsScreenDestination,
                    AddProviderScreenDestination,
                    ProviderManagerScreenDestination,
                    RepositoryManagerScreenDestination,
                    ProviderDetailsScreenDestination,
                    ProviderSettingsScreenDestination,
                    MarkdownScreenDestination,
                    ProviderTestScreenDestination,
                ).routedIn(this)
                    .associateBy { it.route }
        }

    val root =
        object : NavGraphSpec {
            override val route = ROOT

            override val startRoute = SplashScreenDestination

            override val destinationsByRoute =
                listOf<DestinationSpec<*>>(
                    PlayerScreenDestination,
                    SplashScreenDestination,
                    AppUpdatesScreenDestination,
                    UserEditScreenDestination,
                    UserProfilesScreenDestination,
                    UserAvatarSelectScreenDestination,
                    AddUserScreenDestination,
                    MarkdownScreenDestination,
                    PinSetupScreenDestination,
                    PinVerifyScreenDestination,
                ).associateBy { it.route }

            override val nestedNavGraphs =
                listOf(home, search, library, settings)
        }
}
